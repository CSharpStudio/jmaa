//@ sourceURL=sales_delivery_mobile.js
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
        let keyword = me.dom.find('.search-sales-delivery-input').val();
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
        me.dom.find('.search-sales-delivery-input').val('');
        me.orderList.load();
    },
    searchOrder() {
        let me = this;
        me.orderList.load();
    },
    openDetails() {
        let me = this;
        let data = me.orderList.getSelectedData()[0];
        me.salesDeliveryForm.dataId = data.id;
        me.salesDeliveryForm.setData(data);
        me.salesDeliveryForm.offest = 0;
        me.salesDeliveryForm.editors.message.reset();
        me.changePage("details");
        // 切换的时候有点卡顿,加个延时
        setTimeout(() => me.loadDeliveryMaterial(), 500);
    },
    loadDeliveryMaterial(showResult) {
        let me = this;
        let form = me.salesDeliveryForm;
        if (!form.dataId) {
            return jmaa.msg.error('请选择销售发货单'.t());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadDeliveryMaterial',
            args: {
                ids: [form.dataId],
                warehouseId: data.warehouse_id,
                offset: form.offest,
            },
            onsuccess: function (r) {
                form.setData(r.data);
                if (showResult === true && r.data.material_id) {
                    data.result = {msg: '读取物料'.t() + r.data.material_id[1]};
                }
                let status = form.editors.status.getRawValue()
                if (status == 'done'){
                    me.dom.find('.delivery-material,.auto-confirm,.stock-out').hide();
                } else {
                    me.dom.find('.delivery-material,.auto-confirm,.stock-out').show();
                }
            }
        });
    },
    scanCode() {
        let me = this;
        let form = me.salesDeliveryForm;
        let status = form.editors.status.getRawValue();
        if (status === 'done'){
            return jmaa.msg.error("单据已完成");
        }
        let warehouseId = form.editors.warehouse_id.getRawValue();
        let autoConfirm = me.dom.find('#check-auto').is(":checked")
        let input = me.dom.find('.sales-delivery-input');
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
                if (r.data.action === 'split') {
                    // 这里有数据,就拆分
                    me.splitLabel(form, r.data.split,code);
                }
            }
        });
    },
    splitLabel(form, data,code) {
        let me = this;
        let input = me.dom.find('.sales-delivery-input');
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${code}`,
            init(dialog) {
                let fields = {
                    sn: {name: 'sn', type: 'char', label: '标签条码', readonly: true},
                    qty: {name: 'qty', type: 'float', label: '标签数量', readonly: true},
                    deficit_qty: {name: 'deficit_qty', type: 'float', label: '待发数量', readonly: true},
                    split_qty: {name: 'split_qty', type: 'float', label: '拆分数量'},
                    print_old: {name: 'print_old', type: 'boolean', label: '打印原标签', defaultValue: true},
                };
                let arch = `<form cols="3">
                                <field name="sn"></field>
                                <field name="qty" decimals="${data.accuracy}"></field>
                                <field name="deficit_qty" decimals="${data.accuracy}"></field>
                                <field name="split_qty" decimals="${data.accuracy}"></field>
                                <field name="print_old"></field>
                            </form>`;
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    fields: fields,
                    arch: arch,
                    view: me
                });
                dialog.splitForm.setData(data);
                dialog.splitForm.dom.enhanceWithin();
            },
            submit(dialog) {
                dialog.busy(true);
                let printOld = dialog.splitForm.editors.print_old.getValue()? dialog.splitForm.editors.print_old.getValue(): false
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    args: {
                        sn: code,
                        splitQty: dialog.splitForm.editors.split_qty.getValue(),
                        printOld: printOld,
                    },
                    onsuccess: function (r) {
                        input.val(r.data.newSn)
                        form.editors.commit_qty.setValue(r.data.data[0].qty);
                        form.editors.message.setValue({msg: '拆分新标签:'.t() + r.data.newSn});
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    prevMaterial() {
        let me = this;
        me.salesDeliveryForm.offest--;
        me.loadDeliveryMaterial(true);
    },
    nextMaterial() {
        let me = this;
        me.salesDeliveryForm.offest++;
        me.loadDeliveryMaterial(true);
    },
    deliveryMaterial() {
        let me = this;
        let form = me.salesDeliveryForm
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let input = me.dom.find('.sales-delivery-input');
        let code = input.val();
        if (!code) {
            return jmaa.msg.error('请扫描标签'.t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'deliveryMaterial',
            args: {
                ids: [form.dataId],
                code,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
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
                form.setData(r.data.data);
                form.editors.message.setValue({msg: r.data.message});
            }
        });
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['delivery_id', '=', me.orderList.getSelected()[0]]];
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
                if (me.salesDeliveryForm.editors.status.getRawValue() ==='done'){
                    setTimeout(function () {
                        me.dom.find('.delete-detail').hide();
                    }, 500);
                }
            }
        });
    },
    loadLineList(list, callback) {
        let me = this;
        let criteria = [['delivery_id', '=', me.orderList.getSelected()[0]]];
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
                    model: "wms.sales_delivery_details",
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
        });
    },
    stockOut() {
        let me = this;
        let form = me.salesDeliveryForm
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOut',
            args: {
                ids: [form.dataId]
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.detailsList.load();
            }
        });
    },
})
