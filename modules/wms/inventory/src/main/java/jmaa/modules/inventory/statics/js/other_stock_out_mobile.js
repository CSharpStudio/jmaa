//@ sourceURL=other_stock_out_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.orderList.load();
        me.dom.find('[name=detailScope]').on('change', function () {
            me.detailsList.load();
        });
        me.dom.find('[name=materialScope]').on('change', function () {
            me.materialList.load();
        });
    },
    searchOrder() {
        let me = this;
        me.dom.find('.search-input').val('');
        me.stockOutForm.setData({})
        me.stockOutForm.editors.message.reset()
        me.orderList.load();
    },
    backSearchOrder() {
        let me = this;
        me.dom.find('.search-input').val('');
        me.dom.find('.label-input').val('');
        me.orderList.load();
    },
    loadOrderList(list, callback) {
        let me = this;
        let criteria = [["status", "in", ["delivering", "approve"]]];
        let keyword = me.dom.find('.search-input').val();
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
    openStockOut() {
        let me = this;
        me.changePage("details");
        let data = me.orderList.getSelectedData()[0];
        me.stockOutForm.dataId = data.id;
        me.stockOutForm.offest = 0;
        me.stockOutForm.editors.result.reset();
        me.stockOutForm.editors.print_flag.setValue(false);
        me.stockOutForm.editors.warehouse_id.setValue(data.warehouse_id[0]);
        me.loadStockOutMaterial();
        me.tabs.open('stockOutTab');
    },
    nextMaterial() {
        let me = this;
        me.stockOutForm.offest++;
        me.loadStockOutMaterial(true);
    },
    prevMaterial() {
        let me = this;
        me.stockOutForm.offest--;
        me.loadStockOutMaterial(true);
    },
    loadStockOutMaterial(showResult) {
        let me = this;
        let form = me.stockOutForm;
        if (!form.dataId) {
            return jmaa.msg.error('请选择发料单'.t());
        }
        let printFlag = me.stockOutForm.editors.print_flag.getValue();
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadStockOutMaterial',
            args: {
                ids: [form.dataId],
                offset: form.offest,
                warehouseId: data.warehouse_id,
            },
            onsuccess: function (r) {
                form.setData(r.data);
                if (showResult) {
                    data.result = {msg: '读取物料'.t() + r.data.material_id[1]};
                }
                form.editors.print_flag.setValue(printFlag)
            }
        });
    },
    filterWarehouse(criteria) {
        let me = this;
        let data = me.stockOutForm.getRaw();
        criteria.push(['id', 'in', data.warehouse_id]);
        return criteria;
    },
    showMaterialList() {
        let me = this;
        me.materialList.load();
    },
    loadMaterialList(list, callback) {
        let me = this;
        let criteria = [['other_stock_out_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=materialScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', 'not in', ['delivered', 'done']]);
        }
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
    showDetailsList() {
        let me = this;
        me.detailsList.load();
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['other_stock_out_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=detailScope]:checked').val();
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
    scanCode() {
        let me = this;
        let input = me.dom.find('.label-input');
        let code = input.val();
        me.submitCode(code);
        let stockRule = me.stockOutForm.editors.stock_rule.getRawValue();
    },
    submitCode(code) {
        let me = this;
        let form = me.stockOutForm;
        if (!form.dataId) {
            return jmaa.msg.error('请选择发料单'.t());
        }
        let printFlag = me.stockOutForm.editors.print_flag.getRawValue();
        let locationId = me.stockOutForm.editors.location_id.getRawValue();
        let stockRule = me.stockOutForm.editors.stock_rule.getRawValue();
        if (!form.valid() && 'sn' === stockRule) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'delivery',
            args: {
                ids: [form.dataId],
                code,
            },
            onerror(r) {
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                data.result = {msg: r.data.message};
                me.stockOutForm.setData(r.data.data);
                if (r.data.action == 'split') {
                    me.splitLabel(form, r.data.split);
                }
                if ('sn' === r.data.data.stock_rule) {
                    me.dom.find('.label-input').val('');
                }
                me.stockOutForm.editors.print_flag.setValue(printFlag)
                me.stockOutForm.editors.location_id.setValue(locationId)
                form.editors.scan_qty.setReadonly(r.data.data.stock_rule === 'lot' && r.data.data.lot_out_qty)
            }
        });
    },
    splitLabel(form, splitData) {
        let me = this;
        jmaa.showDialog({
            title: '拆分标签'.t(),
            init(dialog) {
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    arch: `<form>
                                <editor name="sn" type="char" label="标签条码" readonly="1" colspan="2"></editor>
                                <editor name="qty" type="float" label="标签数量" readonly="1" decimals="${splitData.accuracy}"></editor>
                                <editor name="to_delivery_qty" type="float" label="待发数量" readonly="1" decimals="${splitData.accuracy}"></editor>
                                <editor name="split_qty" type="float" label="拆分数量" decimals="${splitData.accuracy}"></editor>
                                <editor name="print_old" type="boolean" label="打印原标签"></editor>
                            </form>`,
                    view: me
                });
                dialog.splitForm.dom.enhanceWithin();
                dialog.splitForm.setData(splitData);
            },
            submit(dialog) {
                let data = dialog.splitForm.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    args: {
                        sn: data.sn,
                        splitQty: data.split_qty,
                        printOld: data.print_old
                    },
                    dialog,
                    onsuccess: function (r) {
                        data.result = {msg: '拆分新标签:'.t() + r.data.newSn};
                        me.submitCode(r.data.newSn);
                        dialog.close();
                        //移动端打印
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    stockOutMaterial() {
        let me = this;
        let form = me.stockOutForm;
        if (form.editors.to_delivery_qty.getRawValue() <= 0) {
            return jmaa.msg.error("当前物料已备齐".t());
        }
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let input = me.dom.find('.label-input');
        let code = input.val();
        if (!code) {
            return jmaa.msg.error("请先扫标签条码后操作".t());
        }
        let printFlag = form.editors.print_flag.getRawValue();
        let locationId = form.editors.location_id.getRawValue();
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOutMaterial',
            args: {
                ids: [form.dataId],
                code,
                materialId: data.material_id,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
                qty: data.scan_qty,
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
                data.result = {msg: r.data.message};
                me.stockOutForm.setData(r.data.data);
                input.val('');
                let printMap = r.data.printMap
                if (printMap && printMap.data) {
                    jmaa.print(printMap);
                }
                me.stockOutForm.editors.print_flag.setValue(printFlag)
                me.stockOutForm.editors.location_id.setValue(locationId)
            }
        });
    },
    createStockOut() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOut',
            args: {
                ids: me.orderList.getSelected()
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.detailsList.load();
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
                    model: "wms.other_stock_out_details",
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
});
