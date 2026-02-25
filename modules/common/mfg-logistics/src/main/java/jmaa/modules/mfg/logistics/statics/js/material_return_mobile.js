//@ sourceURL=material_return_mobile.js
jmaa.view({
    searchOrder() {
        let me = this;
        let input = me.dom.find('.return-input');
        input.val('');
        me.orderList.load();
    },
    loadOrderList(list, callback) {
        let me = this;
        let criteria = [["status", "!=", "done"]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'search',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
    createOrder() {
        let me = this;
        jmaa.showDialog({
            title: '创建退料单'.t(),
            init(dialog) {
                me.loadView(me.model, 'form').then(v => {
                    dialog.form = dialog.body.JForm({
                        model: me.model,
                        module: me.module,
                        view: me,
                        fields: v.fields,
                        arch: v.views.form.arch,
                    });
                    dialog.form.dom.enhanceWithin();
                    dialog.form.create();
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'create',
                    args: data,
                    onsuccess(r) {
                        dialog.close();
                        me.orderList.load();
                    }
                })
            }
        });
    },
    openOrder() {
        let me = this;
        me.loadOrder();
        me.changePage("order");
        me.orderForm.editors.result.setValue(null)
    },
    loadOrder() {
        let me = this;
        let data = me.orderList.getSelectedData()[0];
        me.orderForm.setData(data);
        me.detailsList.load();
        me.orderForm.editors.print_flag.setValue(false)
    },
    onOrderFormLoad(e, form) {
        let me = this;
        if ('draft' != form.editors.status.getRawValue()) {
            form.editors.related_code.readonly(true)
        }
    },
    loadLine() {
        let me = this;
        me.lineList.load();
    },
    loadDetails() {
        let me = this;
        me.detailsList.load();
    },
    onDetailsListLoad() {
        let me = this;
        // 这里控制按钮显示隐藏
        // 只能放这里, 列表刷新有延时,  再其他地方设置都无效,(列表的删除按钮)
        let status = me.orderForm.editors.status.getRawValue()
        if (status === 'done') {
            me.dom.find(".btn-order-group").attr("disabled", true);
        } else {
            me.dom.find(".btn-order-group").attr("disabled", false);
        }
    },
    loadLineList(list, callback) {
        let me = this;
        let criteria = [['material_return_id', '=', me.orderList.getSelected()[0]]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                relatedField: 'line_ids',
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['material_return_id', '=', me.orderList.getSelected()[0]]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                relatedField: 'details_ids',
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
    snEnter() {
        let me = this;
        me.orderForm.triggerChange("sn");
    },
    backSearchOrder() {
        let me = this;
        let input = me.dom.find('.return-input');
        input.val('');
        me.orderList.load();
    },
    scanReturn() {
        let me = this;
        let form = me.orderForm;
        let data = form.getRaw();
        let input = me.dom.find('.return-input');
        let code = input.val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.orderForm.dataId],
                code,
            },
            onerror(r) {
                data.result = {error: true, msg: r.message};
            },
            onsuccess: function (r) {
                data.result = {msg: r.data.message};
                data.material_id = r.data.data.material_id;
                data.material_name_spec = r.data.data.material_name_spec;
                data.return_qty = r.data.data.return_qty;
            }
        });
    },
    submitCode() {
        let me = this;
        let form = me.orderForm
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let input = me.dom.find('.return-input');
        let code = input.val();
        let data = form.getRaw();
        let printFlag = data.print_flag
        if (printFlag && !data.template_id) {
            return jmaa.msg.error('请选择打印模板'.t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'returnMaterial',
            args: {
                ids: [form.dataId],
                code,
                materialId: data.material_id,
                returnedQty: data.return_qty,
                printFlag: printFlag,
                templateId: data.template_id,
                warehouseId: data.warehouse_id,
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.material_id.setValue(null)
                form.editors.return_qty.setValue(null);
                input.val('');
                data.result = {msg: r.data.message};
                if (printFlag) {
                    jmaa.print(r.data.data);
                }
            }
        });
    },
    deleteDetails(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.msg.confirm({
            title: '确认删除'.t(),
            content: '确认删除?'.t(),
            submit() {
                jmaa.rpc({
                    model: "mfg.material_return_details",
                    module: me.module,
                    method: 'delete',
                    args: {
                        ids: [id],
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        me.detailsList.load();
                    }
                });
            },
            cancel() {
            },
        });
    },
    submitOrder() {
        let me = this;
        let form = me.orderForm
        jmaa.showDialog({
            title: "生成入库单".t(),
            init: function (dialog) {
                dialog.form = dialog.body.JForm({
                    cols: 1,
                    arch: `<form cols="2">
                                <editor label="备注" type="text" name="comment" colspan="2" />
                            </form>`
                });
                dialog.form.dom.enhanceWithin();
            },
            submit: function (dialog) {
                let data = dialog.form.getData();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'commit',
                    args: {
                        ids: [form.dataId],
                        comment: data.comment,
                    },
                    onerror(r) {
                        return jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t())
                        me.lineList.load()
                        me.detailsList.load();
                        me.orderForm.editors.status.setValue('done')
                        dialog.close()
                    }
                });
            },
        })
    },
    addMaterial() {
        let me = this;
        let form = me.orderForm;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "addMaterial",
            args: {
                ids: [form.dataId],
                code: data.sn,
                materialId: data.material_id,
                qty: data.qty
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                data.result = {msg: r.data.message};
                form.editors.sn.resetValue();
                form.editors.material_id.resetValue();
                form.editors.qty.resetValue();
            },
            onerror(r) {
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                }
                jmaa.msg.error(r);
            },
        });
    },
});
jmaa.editor("material-editor", {
    extends: 'editors.many2one',
    searchRelated(callback) {
        let me = this;
        let workOrderId = me.owner.getRaw().work_order_id;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "findMaterial",
            args: {
                limit: me.limit,
                offset: me.offset,
                criteria: me.getFilter(),
                fields: [me.displayField],
                workOrderId,
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    }
});
