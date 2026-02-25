//@ sourceURL=mfg_material_issue.js
jmaa.view({
    stockOut() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOut',
            args: {
                ids: [me.form.dataId]
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.form.load();
            }
        });
    },
    //发料按钮
    issue() {
        let me = this;
        me.form.offset = 0;
        jmaa.showDialog({
            title: '发料单：'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'mfg.material_issue_dialog',
                        type: 'form'
                    },
                    onsuccess: function (r) {
                        let v = r.data;
                        dialog.form = dialog.body.JForm({
                            cols: 4,
                            model: v.model,
                            module: v.module,
                            fields: v.fields,
                            arch: v.views.form.arch,
                            view: me
                        });
                        dialog.dom.find('.buttons-right')
                            .html(`<button type="button" t-click="prevMaterial" class="btn btn-default btn-flat">${'上一个'.t()}</button>
                                    <button type="button" t-click="nextMaterial" class="btn btn-default btn-flat">${'下一个'.t()}</button>`);
                        dialog.form.editors.code.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode == 13 && $(this).val()) {
                                me.submitCode(dialog.form);
                            }
                        }).find('input').focus();
                        me.loadIssueMaterial(null, function (data) {
                            dialog.form.setData(data);
                        });
                    }
                });
            },
            issueMaterial(e, dialog) {
                me.issueMaterial(dialog.form);
            },
            prevMaterial(e, dialog) {
                me.form.offset--;
                me.loadIssueMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                    dialog.form.setData(data);
                    dialog.form.editors.result.setValue({msg: `${'加载物料'.t()}[${data.material_id[1]}]`});
                });
            },
            nextMaterial(e, dialog) {
                me.form.offset++;
                me.loadIssueMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                    dialog.form.setData(data);
                    dialog.form.editors.result.setValue({msg: `${'加载物料'.t()}[${data.material_id[1]}]`});
                });
            },
            cancel(dialog) {
                me.form.load();
            }
        });
    },
    loadIssueMaterial(warehouseId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadIssueMaterial',
            args: {
                ids: [me.form.dataId],
                warehouseId,
                offset: me.form.offset,
            },
            onsuccess: function (r) {
                callback(r.data);
            }
        });
    },
    issueMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'issueMaterial',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                materialId: data.material_id,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
                qty: data.issue_qty,
                printFlag: data.print_flag,
                templateId: data.template_id,
            },
            onerror(r) {
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                data.result = {msg: r.data.message};
                let printMap = r.data.printMap
                if (printMap && printMap.data) {
                    jmaa.print(printMap);
                }
            }
        });
    },
    submitCode(form) {
        let me = this;
        let data = form.getData();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'issue',
            args: {
                ids: [me.form.dataId],
                code: data.code,
            },
            onerror(r) {
                data.code = '';
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                data.result = {msg: r.data.message};
                form.setData(r.data.data);
                if (r.data.split) {
                    me.splitLabel(form, r.data.split);
                } else if (r.data.confirm) {
                    me.confirmIssue(form, r.data.confirm);
                }
            }
        });
    },
    confirmIssue(form, confirmData) {
        let me = this;
        jmaa.showDialog({
            id: 'confirm-issue',
            css: 'modal-lg',
            title: '确认发料：'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    model: form.model,
                    fields: form.fields,
                    module: me.module,
                    arch: `<form cols="3">
                                <field name="material_id"></field>
                                <editor name="material_name_spec" label="名称规格" type="char" readonly="1" colspan="2"></editor>
                                <editor name="code" type="char" label="标签条码" readonly="1" colspan="2"></editor>
                                <editor name="label_qty" type="float" label="标签数量" readonly="1" decimals="${confirmData.unit_accuracy}"></editor>
                                <editor name="to_issue_qty" type="float" label="待发数量" readonly="1" decimals="${confirmData.unit_accuracy}"></editor>
                                <editor name="issue_qty" type="float" label="发料数量" required="1" min="0" decimals="${confirmData.unit_accuracy}"></editor>
                                <field name="warehouse_id" readonly="1"></field>
                                <field name="location_id" lookup="[['warehouse_id','=',warehouse_id]]"></field>
                                <editor name="print_label" label="打印新标签" type="boolean"></editor>
                                <field name="template_id" lookup="[['category','=','material_label']]" t-visible="print_label"></field>
                            </form>`,
                    view: me
                });
                dialog.form.setData(confirmData);
                dialog.form.editors.issue_qty.setAttr('max', confirmData.lock_qty ? confirmData.issue_qty : null);
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'issueMaterial',
                    args: {
                        ids: [me.form.dataId],
                        code: data.code,
                        materialId: data.material_id,
                        warehouseId: data.warehouse_id,
                        locationId: data.location_id,
                        qty: data.issue_qty,
                        printLabel: data.print_label,
                        templateId: data.template_id,
                    },
                    onerror(r) {
                        if (r.code == 1000) {
                            form.getData().result = {error: true, msg: r.message};
                        }
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        r.data.data.result = {msg: r.data.message};
                        form.setData(r.data.data);
                        let printData = r.data.printData
                        if (printData) {
                            jmaa.print(printData);
                        }
                        dialog.close();
                    }
                });
            }
        });
    },
    splitLabel(form, splitData) {
        let me = this;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: '拆分标签：'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    model: form.model,
                    fields: form.fields,
                    module: me.module,
                    arch: `<form cols="3">
                                <field name="material_id"></field>
                                <editor name="material_name_spec" label="名称规格" type="char" readonly="1" colspan="2"></editor>
                                <field name="warehouse_id" readonly="1"></field>
                                <field name="location_id" readonly="1"></field>
                                <editor name="sn" type="char" label="标签序列号" readonly="1"></editor>
                                <editor name="qty" type="float" label="标签数量" readonly="1" decimals="${splitData.unit_accuracy}"></editor>
                                <editor name="to_issue_qty" type="float" label="待发数量" readonly="1" decimals="${splitData.unit_accuracy}"></editor>
                                <editor name="split_qty" type="float" label="拆分数量" decimals="${splitData.unit_accuracy}"></editor>
                                <editor name="print_old" type="boolean" label="打印原标签"></editor>
                            </form>`,
                    view: me
                });
                dialog.form.setData(splitData);
            },
            submit(dialog) {
                let data = dialog.form.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    dialog,
                    args: {
                        ids: [me.form.dataId],
                        sn: data.sn,
                        splitQty: data.split_qty,
                        printOld: data.print_old
                    },
                    onsuccess: function (r) {
                        r.data.data.result = {msg: r.data.message};
                        form.setData(r.data.data);
                        jmaa.print(r.data.printData, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    searchLine() {
        let me = this;
        me.form.editors.line_ids.load();
    },
    onToolbarInit(e, bar) {
        let me = this;
        bar.dom.find('#showUndone').on('change', function () {
            me.searchLine();
        });
    },
    lineFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchInput').val();
        if (keyword) {
            criteria.push(['material_id.code', 'like', keyword])
        }
        let showUndone = target.toolbar.dom.find('#showUndone').is(':checked');
        if (showUndone) {
            criteria.push(['status', 'in', ['new', 'issuing']]);
        }
        return criteria;
    },
    filterWarehouse(criteria) {
        let me = this;
        let values = me.form.editors.warehouse_ids.getRawValue();
        criteria.push(['id', 'in', values])
        return criteria;
    },
});
