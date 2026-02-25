jmaa.component("Diagram", {
    model: 'modeling.diagram',
    module: 'modeling',
    width: 2000,
    height: 2000,
    getTpl: function () {
        return `<div class="tool-bar">
                    <button title="${"刷新".t()}" class="btn btn-default btn-reload">
                        ${"刷新".t()}
                    </button>
                    <button title="${"撤销".t()}" class="btn btn-default btn-undo">
                        ${"撤销".t()}
                    </button>
                    <button title="${"恢复".t()}" class="btn btn-default btn-redo">
                        ${"恢复".t()}
                    </button>
                    <button title="${"查看代码".t()}" class="btn btn-default btn-code-view">
                        ${"查看代码".t()}
                    </button>
                    <button title="${"下载代码".t()}" class="btn btn-default btn-code-down">
                        ${"下载代码".t()}
                    </button>
                    <button title="${"从源码包生成模型图".t()}" class="btn btn-default btn-reflact">
                        ${"生成模型图".t()}
                    </button>
                    <div title="${"缩放".t()}" class="zoom-slider">
                        <div></div>
                    </div>
                </div>
                <div class="model-diagram" tabindex="-1">
                    <div class="tool-panel">
                        <div class="tool-item ui-draggable ui-draggable-handle" data="model">
                            <i class="tool-icon mr-1 fa fa-medium-m"></i>模型
                        </div >
                        <div class="tool-item ui-draggable ui-draggable-handle" data="memo">
                            <i class="tool-icon mr-1 fa fa-sticky-note"></i>备注
                        </div>
                    </div >
                    <div class="aerial-view"></div>
                    <div class="drag-container">
                        <div class="diagram-panel">
                            <div id="'+ this.id + '" class="model-canvas container-fluid">
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    init: function () {
        let me = this, dom = me.dom;
        dom.append(me.getTpl());
        me.zoom = 100;
        me.zoomSlider = dom.find('.zoom-slider div').bootstrapSlider({
            formatter: function (v) {
                if (v >= 5) {
                    return v / 5 * 100 + '%';
                }
                return v * 15 + 25 + '%';
            },
            width: '100px'
        });
        me.zoomSlider.on("change", function (e) {
            let v = e.value.newValue;
            if (v >= 5) {
                me.zoom = v / 5 * 100;
            }
            me.zoom = v * 15 + 25;
            me.canvas.css("zoom", me.zoom + "%");
            dom.triggerHandler('zoom', [me]);
        });
        me.models = jmaa.create("KeyValue");
        me.canvas = dom.find('.model-canvas').height(me.height).width(me.width);
        me.cmdMemo = jmaa.create('CommandMemo', {
            change: function (m) {
                dom.find('.btn-redo').attr('disabled', !m.canRedo());
                dom.find('.btn-undo').attr('disabled', !m.canUndo());
            }
        });
        dom.on('click', '.item-model', function (e) {
            let id = $(this).attr('id'), m = me.models.get(id);
            if (!e.ctrlKey) {
                me.models.each(function () {
                    this.unselect();
                });
                m.select();
            } else {
                if (m.selected) {
                    m.unselect();
                } else {
                    m.select();
                }
            }
        }).on('click', '.btn-undo', function (e) {
            me.cmdMemo.undo();
        }).on('click', '.btn-reload', function (e) {
            me.load();
        }).on('click', '.btn-redo', function (e) {
            me.cmdMemo.redo();
        }).on('click', '.btn-code-view', function (e) {
            let ids = [];
            me.models.each(function () {
                if (this.selected) {
                    ids.push(this.data.id);
                }
            });
            me.viewCode(ids);
        }).on('click', '.btn-code-down', function (e) {
            let ids = [];
            me.models.each(function () {
                if (this.selected) {
                    ids.push(this.data.id);
                }
            });
            window.open(jmaa.web.getTenantPath() + "/diagram/code?models=" + ids.join(), '_self');
        }).on('click', '.btn-reflact', function (e) {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: "reflect",
                args: {ids: [me.id]},
                onsuccess: function (r) {
                    me.load();
                    me.dom.triggerHandler("modelChanged", [me]);
                }
            });
        });
        dom.find(".model-diagram").keydown(function (event) {
            if (event.ctrlKey && event.keyCode == 65) {
                me.selectModels();
                event.stopPropagation();
                event.cancelBubble = true;
                event.preventDefault();
            }
        }).focus();
        me.onModelChanged(me.modelChange);
        me.tools = new ToolItem({
            dom: me.dom.find('.tool-item'),
            diagram: me,
            containment: me.dom.find('.drag-container')
        });
        me.aerialView = new AerialView({dom: me.dom.find('.aerial-view'), diagram: me});
        me.load();
    },
    updateSize: function (size) {
        let me = this;
        me.canvas.width(size.width).height(size.height);
        me.aerialView.updateView();
    },
    onZoom: function (handler) {
        this.dom.on('zoom', handler);
    },
    onModelChanged: function (handler) {
        this.dom.on("modelChanged", handler);
    },
    deleteModel: function (id, undo) {
        let me = this, model = me.models.get(id), values = $.extend({}, model.data);
        values.x = model.x;
        values.y = model.y;
        let save = function () {
            let m = me.models.get(id);
            if (!m) {
                return;
            }
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: "deleteModel",
                args: {modelId: m.data.id},
                onsuccess: function (r) {
                    let m = me.removeModel(id);
                    me.dom.triggerHandler("modelChanged", [me, m]);
                }
            });
        };
        if (!undo) {
            me.cmdMemo.add({
                undo: function () {
                    me.saveModel("addModel", values, {
                        success: function (r) {
                            let opt = $.extend({}, {data: values});
                            opt.id = id;
                            opt.data.id = r.data;
                            opt.x = values.x;
                            opt.y = values.y;
                            let model = me._addModel(opt);
                            me.dom.triggerHandler("modelChanged", [me, model]);
                            me.updateConnectors();
                        }
                    });
                },
                redo: function () {
                    save();
                }
            });
        }
        save();
    },
    unlinkModel: function (id, undo) {
        let me = this;
        let save = function () {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: "unlinkModel",
                args: {ids: [me.id], modelId: id},
                onsuccess: function (r) {
                    for (let m of me.models.values) {
                        if (m.data.id === id) {
                            me.removeModel(m.id);
                        }
                    }
                }
            });
        };
        if (!undo) {
            let m = me.models.first(function (m) {
                return m.data.id == id
            });
            let param = {id: m.data.id, x: m.x, y: m.y};
            me.cmdMemo.add({
                undo: function () {
                    me.linkModel(param, true);
                },
                redo: function () {
                    save();
                }
            });
        }
        save();
    },
    removeModel: function (id) {
        let me = this, model = me.models.remove(id);
        me.canvas.find('#' + id).remove();
        me.updateConnectors();
        me.aerialView.remove(id);
        return model;
    },
    linkModel: function (param, undo) {
        let me = this, model;
        let models = me.models.values
        for (let i = 0; i < me.models.values.length; i++) {
            if (models[i].data.id === param.id) {
                jmaa.msg.error({code: 1000, message: '模型已存在'}, "警告");
                model = models[i].data
                return model;
            }
        }
        let save = function () {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: "linkModel",
                args: {ids: [me.id], modelId: param.id, x: String(param.x), y: String(param.y)},
                onsuccess: function (r) {
                    let values = r.data, fields = values.fields;
                    delete values.fields;
                    let opt = $.extend({}, {data: values});
                    opt.data.module_id = me.module_id;
                    opt.x = values.x;
                    opt.y = values.y;
                    model = me._addModel(opt);
                    $.each(fields, function (idx, item) {
                        model.addField({data: item});
                    });
                    me.updateConnectors();
                }
            });
        };
        if (!undo) {
            me.cmdMemo.add({
                undo: function () {
                    me.unlinkModel(param.id, true);
                },
                redo: function () {
                    save();
                }
            });
        }
        save();
    },
    _addModel: function (data) {
        let me = this;
        data.diagram = me;
        data.renderTo = me.canvas;
        let model = jmaa.create("Model", data);
        me.models.add(model.id, model);
        me.aerialView.add(model.id);
        return model;
    },
    saveModel: function (method, values, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: method, //"addModel" : "updateModel"
            args: {ids: [me.id], values: values},
            onerror: function (e) {
                if (callback && callback.error) {
                    callback.error(e);
                }
                jmaa.msg.error(e);
            },
            onsuccess: function (r) {
                if (callback && callback.success) {
                    callback.success(r);
                }
            }
        });
    },
    addModel: function (values, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "addModel",
            args: {ids: [me.id], values: values},
            onerror: function (e) {
                if (callback && callback.error) {
                    callback.error(e);
                }
                jmaa.msg.errpr(e);
            },
            onsuccess: function (r) {
                values.dataId = r.data;
                let model = me._addModel($.extend({}, values));
                me.dom.triggerHandler("modelChanged", [me, model]);
                me.updateConnectors();
                if (callback && callback.success) {
                    callback.success(r);
                }
            }
        });
    },
    newModel: function (position) {
        let me = this, modelId = jmaa.utils.randomId();
        me.modelEditor.edit({
            submit: function (dialog) {
                if (dialog.form.valid()) {
                    dialog.busy(true);
                    let values = dialog.form.getData();
                    values.x = position.x;
                    values.y = position.y;
                    let update = function (r) {
                        let opt = $.extend({}, {data: values});
                        opt.id = modelId;
                        opt.data.id = r.data;
                        opt.data.module_id = me.module_id;
                        opt.x = values.x;
                        opt.y = values.y;
                        let model = me._addModel(opt);
                        me.dom.triggerHandler("modelChanged", [me, model]);
                        me.updateConnectors();
                    }
                    me.saveModel("addModel", values, {
                        error: function (e) {
                            dialog.busy(false);
                        },
                        success: function (r) {
                            update(r);
                            dialog.close();
                        }
                    });
                    me.cmdMemo.add({
                        undo: function () {
                            me.deleteModel(modelId, true);
                        },
                        redo: function () {
                            me.saveModel("addModel", values, {
                                success: function (r) {
                                    update(r);
                                }
                            });
                        }
                    });
                }
            }
        });
    },
    editModel: function (modelId) {
        let me = this;
        me.modelEditor.edit({
            id: me.models.get(modelId).data.id,
            submit: function (dialog) {
                if (dialog.form.valid()) {
                    dialog.busy(true);
                    let values = dialog.form.getData(), original = me.modelEditor.data;
                    values.id = me.models.get(modelId).data.id;
                    let update = function (vals) {
                        let model = me.models.get(modelId);
                        model.update(vals);
                        me.dom.triggerHandler("modelChanged", [me, model]);
                        me.updateConnectors();
                    }
                    me.saveModel("updateModel", values, {
                        error: function (e) {
                            dialog.busy(false);
                        },
                        success: function (r) {
                            update(values);
                            dialog.close();
                        }
                    });
                    me.cmdMemo.add({
                        undo: function () {
                            original.id = me.models.get(modelId).data.id;
                            me.saveModel("updateModel", original, {
                                success: function (r) {
                                    update(original);
                                }
                            });
                        },
                        redo: function () {
                            me.saveModel("updateModel", values, {
                                success: function (r) {
                                    update(values);
                                }
                            });
                        }
                    });
                }
            }
        });
    },
    deleteField: function (modelId, fieldId) {
        let me = this, model = me.models.get(modelId), values = $.extend({}, model.fields.get(fieldId).data);
        values.model_id = model.data.id;
        let save = function () {
            let model = me.models.get(modelId);
            let dataId = model.fields.get(fieldId).data.id;
            me.saveField("updateModel", {id: me.models.get(modelId).data.id, field_ids: [[2, dataId]]}, {
                success: function (r) {
                    model.removeField(fieldId);
                    me.updateConnectors();
                    me.aerialView.update(me.models.get(modelId).data.id);
                }
            });
        };
        me.cmdMemo.add({
            undo: function () {
                me.saveField("addModelField", values, {
                    success: function (r) {
                        let data = $.extend({}, values);
                        data.id = fieldId;
                        data.dataId = r.data;
                        me.models.get(modelId).addField(data);
                        me.updateConnectors();
                        me.aerialView.update(me.models.get(modelId).data.id);
                    }
                });
            },
            redo: function () {
                save();
            }
        });
        save();
    },
    createField: function (modelId) {
        let me = this, model = me.models.get(modelId), fieldId = jmaa.utils.randomId();
        me.fieldEditor.edit({
            submit: function (dialog) {
                if (dialog.form.valid()) {
                    dialog.busy(true);
                    let values = dialog.form.getData();
                    values.model_id = model.data.id;
                    let update = function (r) {
                        let opt = $.extend({}, {data: values});
                        opt.id = fieldId;
                        opt.data.id = r.data;
                        me.models.get(modelId).addField(opt);
                        me.updateConnectors();
                        me.aerialView.update(modelId);
                    }
                    me.cmdMemo.add({
                        undo: function () {
                            let model = me.models.get(modelId);
                            me.saveField("updateModel", {
                                id: model.data.id,
                                field_ids: [[2, model.fields.get(fieldId).data.id]]
                            }, {
                                success: function (r) {
                                    model.removeField(fieldId);
                                    me.updateConnectors();
                                    me.aerialView.update(modelId);
                                }
                            });
                        },
                        redo: function () {
                            me.saveField("addModelField", values, {
                                success: function (r) {
                                    update(r);
                                }
                            });
                        }
                    });
                    me.saveField("addModelField", values, {
                        success: function (r) {
                            update(r);
                            dialog.close();
                        },
                        error: function (e) {
                            dialog.busy(false);
                        }
                    });
                }
            }
        });
    },
    editField: function (modelId, fieldId) {
        let me = this, model = me.models.get(modelId), field = model.fields.get(fieldId).data;
        me.fieldEditor.edit({
            id: field.id,
            submit: function (dialog) {
                if (dialog.form.valid()) {
                    dialog.busy(true);
                    let values = {}, data = dialog.form.getData(), original = {}, originalData = {};
                    values.id = original.id = me.models.get(modelId).data.id;
                    values.field_ids = [[1, field.id, data]]
                    $.extend(originalData, me.fieldEditor.data);
                    let update = function (r, d) {
                        me.models.get(modelId).fields.get(fieldId).update(d);
                        me.updateConnectors();
                        me.aerialView.update(modelId);
                    }
                    me.cmdMemo.add({
                        undo: function () {
                            original.field_ids = [[1, me.models.get(modelId).fields.get(fieldId).data.id, me.fieldEditor.data]];
                            me.saveField("updateModel", original, {
                                success: function (r) {
                                    update(r, originalData);
                                }
                            });
                        },
                        redo: function () {
                            values.field_ids = [[1, me.models.get(modelId).fields.get(fieldId).data.id, data]]
                            me.saveField("updateModel", values, {
                                success: function (r) {
                                    update(r, data);
                                }
                            });
                        }
                    });
                    me.saveField("updateModel", values, {
                        success: function (r) {
                            update(r, data);
                            dialog.close();
                        },
                        error: function (e) {
                            dialog.busy(false);
                        }
                    });
                }
            }
        });
    },
    saveField: function (method, values, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: method,// "updateModel" : "addModelField",
            args: {values: values},
            onerror: function (e) {
                if (callback && callback.error) {
                    callback.error(e);
                }
                jmaa.msg.error(e);
            },
            onsuccess: function (r) {
                if (callback && callback.success) {
                    //me.load()
                    callback.success(r);
                }
            }
        });
    },
    updateLocation: function (param) {
        let me = this, save = function (p) {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: "updateModelPosition",
                args: {ids: [me.id], modelId: p.modelId, x: String(p.x), y: String(p.y)},
                onerror: function (e) {
                    jmaa.msg.error(e);
                },
                onsuccess: function (r) {
                    me.aerialView.update(p.id);
                }
            });
        };
        me.cmdMemo.add({
            undo: function () {
                me.models.get(param.id).moveTo({x: param.x0, y: param.y0});
                save({id: param.id, modelId: param.modelId, x: param.x0, y: param.y0});
            },
            redo: function () {
                me.models.get(param.id).moveTo({x: param.x, y: param.y});
                save({id: param.id, modelId: param.modelId, x: param.x, y: param.y});
            }
        });
        save(param);
    },
    viewCode: function (models) {
        let me = this;
        jmaa.showDialog({
            title: "代码预览".t(),
            css: "modal-xl modal-code",
            init: function (dialog) {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: "getCode",
                    args: {modelIds: models},
                    onerror: function (e) {
                        jmaa.msg.error(e);
                    },
                    onsuccess: function (r) {
                        let tabs = dialog.body.JTabs();
                        let tid = 0;
                        $.each(r.data, function () {
                            let item = this;
                            tabs.openTab("code-" + tid++, {
                                title: item.name,
                                init: function (tab) {
                                    let html = `<div class="row code-preview m-2">
                                                    <div class="col-6 h-100">
                                                        <span>模型</span>
                                                        <pre class="h-100 border"><code class="h-100 hljs">${hljs.highlight(item.code, {language: 'java'}).value}</code></pre>
                                                    </div>
                                                    <div class="col-6 h-100">
                                                        <span>视图</span>
                                                        <pre class="h-100 border"><code class="h-100 hljs">${hljs.highlight(item.view, {language: 'xml'}).value}</code></pre>
                                                    </div>
                                                </div>`;
                                    tab.append(html);

                                }
                            });
                        });
                    }
                });
            }
        });
    },
    selectModels: function () {
        this.models.each(function () {
            this.select();
        });
    },
    unselectModels: function () {
        this.models.each(function () {
            this.unselect();
        });
    },
    load: function () {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "loadDiagram",
            args: {ids: [me.id]},
            onsuccess: function (r) {
                me.models = jmaa.create("KeyValue");
                me.canvas.empty();
                $.each(r.data, function () {
                    let d = this, fields = d.fields || [], data = {};
                    data.x = d.x;
                    data.y = d.y;
                    delete d.y;
                    delete d.x;
                    delete d.fields;
                    data.data = d;
                    let model = me._addModel(data);
                    $.each(fields, function (i, item) {
                        model.addField({data: item});
                    });
                });
                me.updateConnectors();
                me.aerialView.load();
                me.cmdMemo.clear();
            }
        });
    },
    updateConnectors: function () {
        let me = this;
        me.canvas.find('.model-connector,.connector-endpoint,.connector-note').remove();
        me.models.each(function () {
            this.connectors = [];
        });
        me.models.each(function () {
            let model = this;
            model.fields.each(function () {
                let field = this;
                const fieldType = field.data.field_type || field.field_type || ''
                if (fieldType === 'one2many') {
                    let comodel = me.models.first(function (m) {
                        return m.data.model === field.data.relation;
                    });
                    if (comodel) {
                        let inverse = comodel.fields.first(function (f) {
                            return f.data.name === field.data.relation_field
                        }), conn;
                        if (inverse) {
                            conn = jmaa.create("Connector", {
                                renderTo: me.canvas,
                                diagram: me,
                                type: 'one2many',
                                from: me.canvas.find('#' + field.id),
                                to: me.canvas.find('#' + inverse.id)
                            });
                            inverse.inverse = true;
                        } else {
                            console.warn(`模型[${model.data.model}]一对多[${field.data.name}]的关联字段[${field.relation_field}]在模型[${comodel.data.model}]中不存在`);
                            conn = jmaa.create("Connector", {
                                renderTo: me.canvas,
                                diagram: me,
                                css: 'warn',
                                type: 'one2many',
                                from: me.canvas.find('#' + field.id),
                                to: me.canvas.find('#' + comodel.id + ' .model-name')
                            });
                        }
                        model.connectors.push(conn);
                        comodel.connectors.push(conn);
                    }
                } else if (fieldType === 'many2many' && !this.inverse) {
                    let comodel = me.models.first(function (m) {
                        return m.data.model === field.data.relation;
                    });
                    if (comodel) {
                        let inverse = comodel.fields.first(function (f) {
                            let data = f.data;
                            return data.field_type === 'many2many' && (data.relation_table === field.data.relation_table || data.relation === model.data.model);
                        }), conn;
                        if (inverse) {
                            conn = jmaa.create("Connector", {
                                renderTo: me.canvas,
                                diagram: me,
                                type: 'many2many',
                                from: me.canvas.find('#' + field.id),
                                to: me.canvas.find('#' + inverse.id)
                            });
                            inverse.inverse = true;
                        } else {
                            conn = jmaa.create("Connector", {
                                renderTo: me.canvas,
                                diagram: me,
                                type: 'many2many',
                                from: me.canvas.find('#' + field.id),
                                to: me.canvas.find('#' + comodel.id + ' .model-model')
                            });
                        }
                        model.connectors.push(conn);
                        comodel.connectors.push(conn);
                    }
                }
            });
        });
        me.models.each(function () {
            let model = this;
            model.fields.each(function () {
                let field = this;
                const fieldType = field.data.field_type || field.field_type || ''
                if (fieldType === 'many2one' && !this.inverse) {
                    let comodel = me.models.first(function (m) {
                        return m.data.model === field.data.relation;
                    });
                    if (comodel) {
                        let conn = jmaa.create("Connector", {
                            renderTo: me.canvas,
                            diagram: me,
                            type: 'many2one',
                            from: me.canvas.find('#' + field.id),
                            to: me.canvas.find('#' + comodel.id + ' .model-name')
                        });
                        model.connectors.push(conn);
                        comodel.connectors.push(conn);
                    }
                }
            });
        });
    }
});
