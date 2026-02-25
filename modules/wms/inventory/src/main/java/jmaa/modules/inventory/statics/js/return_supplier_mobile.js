//@ sourceURL=return_supplier_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.orderList.load();
        me.dom.find('[name=scope]').on('change', function () {
            me.detailsList.load();
        });
    },
    searchOrder() {
        let me = this;
        me.orderList.load();
    },
    scanReturn() {
        let me = this;
        let form = me.orderForm;
        let result = form.editors.result;
        if (!form.dataId) {
            jmaa.msg.error('请选择退供应商单'.t());
            return;
        }
        if (!form.valid()) {
            jmaa.msg.error(form.getErrors());
            return;
        }
        let input = me.dom.find('.return-input');
        let code = input.val();
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [form.dataId],
                code,
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                input.val('');
                form.editors.result.setValue({msg: r.data.message});
                me.openScanLabel(r.data, code)
            }
        });
    },
    openScanLabel(data, code) {
        let me = this;
        jmaa.showDialog({
            title: `识别标签`,
            init(dialog) {
                me.loadView("wms.return_supplier_dialog", 'form', "return_supplier_dialog_mobile").then(v => {
                    dialog.form = dialog.body.JForm({
                        arch: v.views.form.arch,
                        fields: v.fields,
                        module: v.module,
                        model: v.model,
                        view: me,
                    });
                    dialog.form.dom.enhanceWithin();
                    dialog.form.setData(data.data)
                    dialog.form.editors.sn.setValue(code)
                    dialog.form.editors.commit_qty.setReadonly(data.data.stock_rule === 'sn' || (data.data.stock_rule === 'lot' && data.data.lot_out_qty))
                });
            },
            submit(dialog) {
                let form = dialog.form;
                let actionType = data.action
                if (actionType === 'split') {
                    me.splitLabel(form, data);
                } else {
                    // 提交
                    me.returnMaterial(dialog)
                }
            }
        });
    },
    returnMaterial(dialog) {
        let me = this;
        let form = dialog.form;
        let qty = form.editors.commit_qty.getValue();
        if (qty <= 0) {
            jmaa.msg.error("退货数量必须大于0".t());
            form.setInvalid('commit_qty', '必须大于0');
            return;
        }
        let toQty = form.editors.deficit_qty.getValue();
        if (qty > toQty) {
            jmaa.msg.error("退货数量不能大于待退数量".t());
            form.setInvalid('commit_qty', '不能大于待退数量');
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'submitScanCode',
            args: {
                ids: [me.orderForm.dataId],
                materialId: form.editors.material_id.getRawValue(),
                warehouseId: form.editors.warehouse_id.getRawValue(),
                locationId: form.editors.location_id.getRawValue(),
                sn: form.editors.sn.getRawValue(),
                qty
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData({});
                jmaa.msg.show('操作成功'.t())
                me.orderForm.editors.result.setValue({msg: r.data.message});
                dialog.close();
            }
        });
    },
    splitLabel(form, data) {
        let me = this;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${form.editors.sn.getValue()}`,
            init(dialog) {
                let fields = {
                    sn: {name: 'sn', type: 'char', label: '标签条码', readonly: true},
                    qty: {name: 'qty', type: 'float', label: '标签数量', readonly: true},
                    deficit_qty: {name: 'deficit_qty', type: 'float', label: '待退数量', readonly: true},
                    split_qty: {name: 'split_qty', type: 'float', label: '拆分数量'},
                    print_old: {name: 'print_old', type: 'boolean', label: '打印原标签', defaultValue: true},
                };
                let arch = `<form cols="3">
                                <field name="sn"/>
                                <field name="qty" decimals="${data.split.accuracy}"/>
                                <field name="deficit_qty" decimals="${data.split.accuracy}"/>
                                <field name="split_qty" decimals="${data.split.accuracy}"/>
                                <field name="print_old"/>
                            </form>`;
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    fields: fields,
                    arch: arch,
                    view: me
                });
                dialog.splitForm.setData(data.split);
                dialog.splitForm.dom.enhanceWithin();
            },
            submit(dialog) {
                dialog.busy(true);
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    args: {
                        sn: dialog.splitForm.editors.sn.getValue(),
                        splitQty: dialog.splitForm.editors.split_qty.getValue(),
                        printOld: dialog.splitForm.editors.print_old.getValue()
                    },
                    onsuccess: function (r) {
                        form.editors.sn.setValue(r.data.data[0].code);
                        form.editors.qty.setValue(r.data.newQty);

                        let deficitQty = form.editors.deficit_qty.getValue();
                        if (r.data.newQty > deficitQty) {
                            form.editors.commit_qty.setValue(deficitQty);
                        } else {
                            form.editors.commit_qty.setValue(r.data.newQty);
                        }
                        if (form.editors.stock_rule.getRawValue() === 'sn') {
                            form.editors.commit_qty.setReadonly(true);
                        }
                        me.orderForm.editors.result.setValue({msg: '拆分新标签:'.t() + r.data.newSn});
                        data.action = 'material'
                        dialog.busy(false);
                        dialog.close();
                        // 前面那一步,还是拆分状态,
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            },
        });
    },
    splitReturn() {
        let me = this;
        let form = me.orderForm;
        let result = form.editors.result;
        let input = me.dom.find('.return-input');
        let code = input.val();
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [form.dataId],
                code,
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                input.val('');
                form.editors.result.setValue({msg: r.data.message});
                me.openScanLabel(r.data, code)
            }
        });
    },
    loadOrderList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-return-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchReturnOrder',
            args: {
                keyword,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                order: 'code asc',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data});
            }
        });
    },
    showMaterialList() {
        let me = this;
        me.materialList.load();
    },
    loadMaterialList(list, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria: [['return_id', '=', me.orderList.getSelected()[0]]],
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
    showDetailsList() {
        let me = this;
        me.detailsList.load();
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['return_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=scope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['stock_out_id', '=', null]);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                relatedField: 'details_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    },
    openDetails() {
        let me = this;
        me.changePage("details");
        let data = me.orderList.getSelectedData()[0];
        me.orderForm.dataId = data.id;
        me.orderForm.setData(data);
        me.orderForm.editors.result.reset();
        me.tabs.open('returnTab');
    },
    resetSearchOrder() {
        let me = this;
        me.dom.find('.search-return-input').val('');
        me.orderList.load();
    },
    createStockOut() {
        let me = this;
        jmaa.showDialog({
            title: "生成出库单".t(),
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
                    method: 'stockOut',
                    args: {
                        ids: me.orderList.getSelected(),
                        comment: data.comment,
                    },
                    onerror(r) {
                        console.log(r.message)
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        me.detailsList.load();
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                    }
                });
            },
        })
    },
    deleteDetail(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'deleteDetail',
            args: {
                ids: [id],
            },
            onsuccess: function (r) {
                me.detailsList.load()
                jmaa.msg.show('操作成功'.t())
            }
        });
    }
})
