//@ sourceURL=transfer_order_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.orderList.load()
        me.dom.find('[name=detailScope]').on('change', function () {
            me.detailsList.load();
        });
        me.dom.find('[name=materialScope]').on('change', function () {
            me.lineList.load();
        });
    },
    searchOrder() {
        let me = this;
        me.dom.find('.transfer-input').val('');
        me.orderList.load();
    },
    loadOrderList(list, callback) {
        let me = this;
        let criteria = [];
        let keyword = me.dom.find('.search-transfer-input').val();
        if (keyword) {
            criteria.push(['code', 'like', keyword]);
        }
        criteria.push(['status', '!=', 'done']);
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'search',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    },
    openOrderDetails() {
        let me = this;
        me.changePage("details");
        let data = me.orderList.getSelectedData()[0];
        me.transferOrderForm.dataId = data.id;
        me.transferOrderForm.offest = 0;
        me.transferOrderForm.editors.result.reset();
        me.loadTransferMaterial();
        me.tabs.open('transferTab');
    },
    prevMaterial() {
        let me = this;
        me.transferOrderForm.offest--;
        me.loadTransferMaterial(true);
    },
    nextMaterial() {
        let me = this;
        me.transferOrderForm.offest++;
        me.loadTransferMaterial(true);
    },
    loadTransferMaterial(showResult) {
        let me = this;
        let form = me.transferOrderForm;
        if (!form.dataId) {
            return jmaa.msg.error('请选择调拨单'.t());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadTransferMaterial',
            args: {
                ids: [form.dataId],
                offset: form.offest,
                warehouseId: data.warehouse_id,
            },
            onsuccess: function (r) {
                $.extend(r.data, form.orderData);
                form.setData(r.data);
                if (showResult) {
                    data.result = {msg: '读取物料'.t() + r.data.material_id[1]};
                }
                form.editors.transfer_qty.setReadonly(r.data.stock_rule === 'lot' && r.data.lot_out_qty)
            }
        });
    },
    transferMaterial() {
        let me = this;
        let form = me.transferOrderForm;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let input = me.dom.find('.transfer-input');
        let code = input.val();
        let data = form.getRaw();
        if (!code) {
            return jmaa.msg.error('请先扫描标签'.t());
        }
        if (!data.transfer_qty) {
            return jmaa.msg.error('请输入调拨数量'.t());
        }
        let locationId = data.location_id;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'transferMaterial',
            args: {
                ids: [form.dataId],
                code,
                materialId: data.material_id,
                warehouseId: data.warehouse_id,
                locationId: locationId,
                qty: data.transfer_qty,
                printFlag: data.print_flag ? data.print_flag : false,
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
                $.extend(r.data.data, me.transferOrderForm.orderData);
                me.transferOrderForm.setData(r.data.data);
                me.transferOrderForm.editors.location_id.setValue(locationId);
                input.val('');
                // todo 打印标签
                let printMap = r.data.printMap
                if (printMap && printMap.data) {
                    jmaa.print(printMap);
                }
            }
        });
    },
    showLineList() {
        let me = this;
        me.lineList.load();
    },
    showDetailsList() {
        let me = this;
        me.detailsList.load();
    },
    scanTransfer() {
        let me = this;
        let input = me.dom.find('.transfer-input');
        let code = input.val();
        me.submitCode(code);
    },
    submitCode(code) {
        let me = this;
        let form = me.transferOrderForm;
        if (!form.dataId) {
            return jmaa.msg.error('请选择调拨单'.t());
        }
        let stockRule = me.transferOrderForm.editors.stock_rule.getRawValue();
        if (!form.valid() && 'sn' === stockRule) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let locationId = data.location_id
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'transfer',
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
                $.extend(r.data.data, me.transferOrderForm.orderData);
                me.transferOrderForm.setData(r.data.data);
                if (r.data.action == 'split') {
                    me.splitLabel(form, r.data.split);
                }
                if ('sn' === r.data.data.stock_rule) {
                    me.dom.find('.transfer-input').val('');
                }
                form.editors.transfer_qty.setReadonly(r.data.data.stock_rule === 'lot' && r.data.data.lot_out_qty)
                form.editors.location_id.setValue(locationId);
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
                                <editor name="sn" type="char", label="标签条码" readonly="1" colspan="2"></editor>
                                <editor name="qty" type="float", label="标签数量" readonly="1" decimals="${splitData.accuracy}"></editor>
                                <editor name="to_transfer_qty" type="float" label="待调拨数量" readonly="1" decimals="${splitData.accuracy}"></editor>
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
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['transfer_order_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=detailScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', '!=', 'done']);
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
    loadLineList(list, callback) {
        let me = this;
        let criteria = [['transfer_order_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=materialScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', '!=', 'done']);
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
                relatedField: 'line_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    },
    showDetails() {
        let me = this;
        let hide = me.dom.find('.field-group').hasClass('collapsed');
        me.dom.find('.list-panel').css('height', hide ? 'calc(100% - 166px)' : 'calc(100% - 288px)');
    },
    filterWarehouse(criteria) {
        let me = this;
        let values = me.transferOrderForm.editors.source_warehouse_ids.getValue();
        criteria.push(['id', 'in', values])
        return criteria;
    },
    deleteDetails(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.msg.confirm({
            title: '确认删除'.t(),
            content: '确认删除?'.t(),
            submit() {
                jmaa.rpc({
                    model: "wms.transfer_order_details",
                    module: me.module,
                    method: 'deleteDetails',
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
})
