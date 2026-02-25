//@ sourceURL=wip_work_order.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let data = form.getRaw();
        let readonly = !['draft', 'suspend'].includes(data.status);
        if (readonly) {
            for (let editor in form.editors) {
                if (!['material_name_spec', 'material_category', 'unit_id', 'origin'].includes(editor)) {
                    form.editors[editor].readonly(readonly);
                }
            }
        }
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
    },
    cancelOrder(e, target) {
        this.changeStatus(target.getSelected(), '取消', 'cancel');
    },
    suspendOrder(e, target) {
        this.changeStatus(target.getSelected(), '暂停', 'suspend');
    },
    releaseOrder(e, target) {
        this.saveAndChangeStatus(target, '发放', 'release');
    },
    processFilter(criteria) {
        let me = this;
        let materialId = me.form.editors.material_id.getRawValue();
        criteria.push("|", ['product_family_id.product_ids', '=', materialId], ['product_family_id', '=', null]);
        return criteria;
    },
    requestMaterials(target, grid) {
        let me = this;
        me.issueNumDialog = jmaa.showDialog({
            title: "设置发料套数".t(),
            css: '',
            init: function (dialog) {
                me.loadView('mfg.work_order', 'form', "issue_num_dialog").then(v => {
                    dialog.form = dialog.body.JForm({
                        arch: v.views.form.arch,
                        fields: v.fields,
                        module: v.module,
                        model: v.model,
                        view: me,
                    });
                    me.rpc(me.model, 'read', {
                        ids: [me.getSelected()[0]],
                        fields: ["plan_qty", "issue_qty"],
                    }).then(data => {
                        dialog.form.setData(data[0]);
                    });
                });
            },
            submit(dialog) {
                let me = this;
                if (!dialog.form.valid()) {
                    jmaa.msg.error(dialog.form.getErrors())
                    return;
                }
                let data = dialog.form.getSubmitData();
                let qty = data.qty
                let plan_qty = data.plan_qty
                let issue_qty = data.issue_qty
                if (qty > plan_qty - issue_qty) {
                    jmaa.msg.error("总发料套数不能超过计划数".t());
                    return
                }
                // 保存,生成发料单
                jmaa.rpc({
                    model: "mfg.work_order",
                    module: me.module,
                    method: 'requestMaterials',
                    args: {
                        ids: [data.id],
                        qty
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show("操作成功".t());
                    },
                    onerror: function (e) {
                        jmaa.msg.error(e);
                    },
                });
                dialog.close()
            }

        });

    },
    saveIssueNum(id, qty) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'requestMaterials',
            args: {
                ids: [id],
                qty
            },
            onsuccess: function (r) {
                jmaa.msg.show("操作成功".t());
            },
            onerror: function (e) {
                jmaa.msg.error(e);
            },
        });
    },
    renderIssueNum(id, target, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'read',
            args: {
                fields: ["plan_qty", "issue_qty"],
                ids: [id]
            },
            onsuccess: function (r) {
                callback(r.data[0]);
            }
        });
    },
    showRoute() {
        let me = this;
        setTimeout(() => me.form.editors.work_order_route_id.updateFlow(), 200);
    },
});

jmaa.editor('route-editor', {
    extends: ['editors.Editor', 'RouteDiagram'],
    routeModel: 'mfg.work_order_route',
    nodeModel: 'mfg.work_order_route_node',
    getTpl() {
        let me = this;
        return `<div class="content route-flow">
                    <div class="route-details">
                        <div class="toolbar details-toolbar">
                            <button type="button" class="btn btn-default btn-flat btn-reload">${"刷新".t()}</button>
                            <button disabled class="btn btn-default btn-flat btn-undo">${"撤销".t()}</button>
                            <button disabled class="btn btn-default btn-flat btn-redo">${"恢复".t()}</button>
                        </div>
                        <div class="route-diagram"></div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).css('height', '600px').on('click', '.btn-undo', function (e) {
            me.flowDiagram.undo();
        }).on('click', '.btn-reload', function (e) {
            me.loadFlow();
        }).on('click', '.btn-redo', function (e) {
            me.flowDiagram.redo();
        });
        me.initDiagram();
    },
    loadFlow(id, callback) {
        let me = this;
        if (!me.owner.dataId) {
            return;
        }
        me.dropmenu = null;
        me.flowDiagram.readonly(false);
        me.view.rpc(me.owner.model, 'loadFlow', {
            ids: [me.owner.dataId],
            fields: ['type', 'label', 'parent_id', 'child_ids', 'present', 'process_type', 'collection_result',
                'is_optional', 'is_output', 'create_task', 'enable_move_in', 'is_repeatable', 'is_deduction', 'to_fqc',
                'x', 'y', 'ok_id', 'ng_id', 'is_start', 'is_end'],
        }, {
            active_test: true,
        }).then(r => {
            me.flowDiagram.updateSize({width: r.canvas_width, height: r.canvas_height});
            let nodes = [];
            let parents = {};
            let children = [];
            for (let n of r.nodes) {
                if (!n.parent_id) {
                    n.children = [];
                    nodes.push(n);
                    parents[n.id] = n;
                } else {
                    children.push(n);
                }
            }
            for (let n of children) {
                let p = parents[n.parent_id];
                p && p.children.push(n);
            }
            me.flowDiagram.load(nodes);
            me.flowDiagram.dataId = r.id;
            if (!r.editable) {
                me.dom.find('.details-toolbar .btn:not(.btn-reload)').hide();
                me.dom.find('.details-toolbar .btn-reload').show();
            } else {
                me.dom.find('.details-toolbar .btn').show();
            }
            me.flowDiagram.readonly(!r.editable);
            me.family_id = r.family_id;
            callback && callback();
        });
    },
    updateFlow() {
        let me = this;
        me.flowDiagram.items.each(function () {
            let item = this;
            for (let c of item.connectors) {
                c.update();
            }
        });
        me.flowDiagram.aerialView.updateView();
        me.flowDiagram.aerialView.load();
    },
    setValue(value) {
        let me = this;
        if (value) {
            me.loadFlow();
        } else {
            me.flowDiagram.dataId = null;
            me.flowDiagram.unload();
            me.dom.find('.details-toolbar .btn').hide();
        }
    },
    processFilter() {
        let me = this;
        return ["|", ['product_family_id', '=', me.family_id], ['product_family_id', '=', null]];
    },
});

