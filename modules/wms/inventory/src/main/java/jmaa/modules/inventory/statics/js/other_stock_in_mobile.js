//@ sourceURL=other_stock_in_mobile.js
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
    scanCode() {
        let me = this;
        let form = me.orderForm;
        let result = form.editors.result;
        if (!form.dataId) {
            jmaa.msg.error('请选择其它入库单'.t());
            return;
        }
        if (!form.valid()) {
            jmaa.msg.error(form.getErrors());
            return;
        }
        let input = me.dom.find('.scan-label-input');
        let code = input.val();
        let data = form.getRaw();
        let autoConfirm = data.auto_confirm;
        let warehouseId = data.warehouse_id;
        let locationId = data.location_id;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [form.dataId],
                code,
                warehouseId,
                locationId,
                autoConfirm
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                input.val('');
                form.editors.result.setValue({msg: r.data.message});
                if (!form.editors.auto_confirm.getRawValue()){
                    me.openScanLabel(r.data, code,form.editors.warehouse_id.getRawValue(),form.editors.location_id.getRawValue())
                }
            }
        });
    },
    openScanLabel(data, code,warehouseId,locationId) {
        let me = this;
        jmaa.showDialog({
            title: `识别标签`,
            init(dialog) {
                me.loadView("wms.other_stock_in_dialog", 'form', "other_stock_in_dialog_mobile").then(v => {
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
                    dialog.form.editors.warehouse_id.setValue(warehouseId)
                    dialog.form.editors.location_id.setValue(locationId)
                    dialog.form.editors.qty.setReadonly(data.data.stock_rule === 'sn' || (data.data.stock_rule === 'lot' && data.data.lot_in_qty))
                    dialog.form.editors.sn.setReadonly(true);
                });
            },
            submit(dialog) {
                let form = dialog.form;
                me.submitMaterial(dialog)

            }
        });
    },
    checkLocation() {
        let me = this;
        let location = me.orderForm.editors.location.getRawValue();
        if (location) {
            // 有值,
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'checkLocation',
                args: {
                    ids: [me.orderList.getSelected()[0]],
                    locationCode:location,
                },
                onerror(r) {
                    me.orderForm.editors.location_id.setValue(null)
                    me.orderForm.editors.location.setValue(null)
                    jmaa.msg.error(r);
                },
                onsuccess: function (r) {
                    me.orderForm.editors.location_id.setValue(r.data)
                }
            });
        }
    },
    submitMaterial(dialog) {
        let me = this;
        let form = dialog.form;
        let qty = form.editors.qty.getValue();
        if (qty <= 0) {
            jmaa.msg.error("扫码数量必须大于0".t());
            form.setInvalid('qty', '必须大于0');
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
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                form.setData({});
                jmaa.msg.show('操作成功'.t())
                me.orderForm.editors.result.setValue({msg: r.data.message});
                dialog.close();
            }
        });
    },
    loadOrderList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-input').val();
        let criteria = [["status", "in", ["delivering", "approve"]]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchOrder',
            args: {
                keyword,
                criteria,
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
                criteria: [['other_stock_in_id', '=', me.orderList.getSelected()[0]]],
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
        let criteria = [['other_stock_in_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=scope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['stock_in_id', '=', null]);
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
        me.orderForm.editors.auto_confirm.setValue(false);
        me.tabs.open('otherStockInTab');
    },
    resetSearchOrder() {
        let me = this;
        me.dom.find('.search-input').val('');
        me.orderList.load();
    },
    createStockOut() {
        let me = this;
        jmaa.msg.confirm({
            title: '生成入库单'.t(),
            content: '确认生成入库单?'.t(),
            submit() {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'stockIn',
                    args: {
                        ids: me.orderList.getSelected(),
                    },
                    onerror(r) {
                        console.log(r.message)
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        me.detailsList.load();
                        jmaa.msg.show('操作成功'.t());
                    }
                });
            }
        })
    },
    deleteDetail(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.msg.confirm({
            title: '确认删除'.t(),
            content: '确认删除?'.t(),
            submit() {
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
            },
            cancel() {
            },
        });
    }
})
