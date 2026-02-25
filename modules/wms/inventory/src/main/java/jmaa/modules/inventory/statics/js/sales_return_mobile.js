//@ sourceURL=sales_return_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.orderList.load();
        me.dom.find('[name=detailScope]').on('change', function () {
            me.detailsList.load();
        });
        me.dom.find('[name=materialScope]').on('change', function () {
            me.lineList.load();
        });
    },
    loadOrderList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-sales-return-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchOrder',
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
    resetSearchOrder() {
        let me = this;
        me.dom.find('.search-sales-return-input').val('');
        me.orderList.load();
    },
    searchOrder() {
        let me = this;
        me.orderList.load();
    },
    openDetails() {
        let me = this;
        let data = me.orderList.getSelectedData()[0];
        me.salesReturnForm.dataId = data.id;
        me.salesReturnForm.setData(data);
        me.salesReturnForm.offest = 0;
        me.salesReturnForm.editors.message.reset();
        me.changePage("details");
        let status = me.salesReturnForm.editors.status.getRawValue();
        if (status === 'done'){
            me.dom.find('.return-material,.auto-confirm,.stock-in').hide();
        } else {
            me.dom.find('.return-material,.auto-confirm,.stock-in').show();
        }
    },
    loadReturnMaterial(showResult) {
        let me = this;
        let form = me.salesReturnForm;
        if (!form.dataId) {
            return jmaa.msg.error('请选择销售发货单'.t());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadReturnMaterial',
            args: {
                ids: [form.dataId],
                warehouseId: data.warehouse_id,
                offset: form.offest,
            },
            onsuccess: function (r) {
                form.setData(r.data);
                if (showResult === true && r.data.material_id) {
                    data.message = {msg: '读取物料'.t() + r.data.material_id[1]};
                }
                let status = form.editors.status.getRawValue()
                if (status === 'done'){
                    me.dom.find('.return-material,.auto-confirm,.stock-in').hide();
                } else {
                    me.dom.find('.return-material,.auto-confirm,.stock-in').show();
                }
            }
        });
    },
    scanCode() {
        let me = this;
        let form = me.salesReturnForm;
        let status = form.editors.status.getRawValue();
        if (status === 'done'){
            return jmaa.msg.error("单据已完成");
        }
        let warehouseId = form.editors.warehouse_id.getRawValue();
        let autoConfirm = me.dom.find('#check-auto').is(":checked")
        let input = me.dom.find('.sales-return-input');
        let code = input.val();
        if (!code) {
            return jmaa.msg.error('请扫描标签'.t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [form.dataId],
                code,
                warehouseId,
                autoConfirm,
            },
            onerror(r) {
                input.val('')
                if (r.code === 1000) {
                    form.editors.message.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                form.editors.message.setValue({msg: r.data.message});
                if (autoConfirm) {
                    input.val('');
                } else {
                    input.val(code);
                }
                form.editors.qty.setReadonly(r.data.data.stock_rule === 'sn' || (r.data.data.stock_rule === 'lot' && r.data.data.lot_in_qty))
            }
        });
    },
    returnMaterial() {
        let me = this;
        let form = me.salesReturnForm
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let input = me.dom.find('.sales-return-input');
        let code = input.val();
        if (!code) {
            return jmaa.msg.error('请扫描标签'.t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'receive',
            args: {
                ids: [form.dataId],
                code,
                warehouseId: data.warehouse_id,
                qty: data.qty,
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                input.val('');
                form.setData(r.data.data);
                form.editors.message.setValue({msg: r.data.message});
            }
        });
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['return_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=detailScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', 'not in ', ['done','to-stock']]);
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
                if (me.salesReturnForm.editors.status.getRawValue() ==='done'){
                    setTimeout(function () {
                        me.dom.find('.delete-detail').hide();
                    }, 500);
                }
            }
        });
    },
    loadLineList(list, callback) {
        let me = this;
        let criteria = [['return_id', '=', me.orderList.getSelected()[0]]];
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
    showLineList() {
        let me = this;
        me.lineList.load();
    },
    showDetailsList() {
        let me = this;
        me.detailsList.load();
    },
    deleteDetail(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.msg.confirm({
            title: '确认删除'.t(),
            content: '确认删除?'.t(),
            submit() {
                jmaa.rpc({
                    model: "wms.sales_return_details",
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
        });
    },
    stockIn() {
        let me = this;
        let form = me.salesReturnForm
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockIn',
            args: {
                ids: [form.dataId]
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.dom.find('.return-material,.auto-confirm,.stock-in').hide()
                me.salesReturnForm.editors.status.setValue("done")
                me.detailsList.load();
            }
        });
    },
})
