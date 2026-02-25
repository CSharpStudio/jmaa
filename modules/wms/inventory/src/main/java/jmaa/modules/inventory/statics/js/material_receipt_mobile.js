//@ sourceURL=material_receipt_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.receiptList.load();
        me.dom.find('[name=scope]').on('change', function () {
            me.detailsList.load();
        });
    },
    focusOnList() {
        let me = this;
        me.setFocus('.search-receipt-input');
        me.receiptList.load();
    },
    searchReceipt() {
        let me = this;
        me.receiptList.load();
    },
    resetSearchReceipt() {
        let me = this;
        me.receiptList.load();
    },
    loadReceiptList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-receipt-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchReceipt',
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
    openDetails() {
        let me = this;
        let data = me.receiptList.getSelectedData()[0];
        me.receiptId = data.id;
        me.dom.find('[data-field=code]').html(data.code);
        me.dom.find('[data-field=delivery_note]').html(data.delivery_note);
        me.dom.find('[data-field=supplier_id]').html(data.supplier_id[1]);
        me.receiptForm.editors.result.reset();
        me.tabs.open('receiptTab');
        me.changePage("details");
        me.setFocus('.code-input');
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
                criteria: [['receipt_id', '=', me.receiptId]],
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
        let criteria = [['receipt_id', '=', me.receiptId]];
        let scope = me.dom.find('[name=scope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', '=', 'receive']);
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
    scanReceipt() {
        let me = this;
        me.sendReceipt();
    },
    sendReceipt(confirm) {
        let me = this;
        let code = me.codeScanner.getValue();
        if (!code) {
            return;
        }
        if (!me.receiptId) {
            return jmaa.msg.error('请选择收料单'.t());
        }
        let form = me.receiptForm;
        let auto = me.dom.find('#check-auto').is(":checked");
        let action = confirm ? 'confirm' : auto ? 'auto' : '';
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'receiptByCode',
            args: {
                ids: [me.receiptId],
                code,
                action,
                receiveQty: data.confirm_qty,
                giftQty: data.confirm_gift_qty,
            },
            onerror(r) {
                me.mediaError.play();
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
                me.codeScanner.setValue('');
                me.setFocus('.code-input');
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                form.editors.confirm_qty.readonly(r.data.data.lock_qty);
                data.result = {msg: r.data.message};
                if (r.data.submit) {
                    me.mediaSubmit.play();
                    me.codeScanner.setValue('');
                    me.setFocus('.code-input');
                } else {
                    me.mediaScan.play();
                }
            }
        });
    },
    submitCode() {
        let me = this;
        if (!me.receiptForm.valid()) {
            return jmaa.msg.error(me.receiptForm.getErrors());
        }
        let code = me.codeScanner.getValue();
        if (!code) {
            return jmaa.msg.error('标签条码不能为空'.t());
        }
        me.sendReceipt(true);
    },
    selectAll() {
        let me = this;
        me.materialList.dom.find('[type=checkbox]').prop('checked', true);
    },
    toInspect() {
        let me = this;
        let selected = [];
        me.materialList.dom.find('.rownum [type=checkbox]:checked').each(function () {
            let itemId = $(this).val();
            let data = me.materialList.data.find(d => d.id == itemId);
            selected.push(data.material_id[0]);
        });
        if (!selected.length) {
            return jmaa.msg.error("请勾选要报检的物料".t());
        }
        me.sendInspect(selected);
    },
    toInspectAll() {
        let me = this;
        me.sendInspect();
    },
    sendInspect(selected) {
        let me = this;
        jmaa.showDialog({
            title: '报检'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    cols: 1,
                    fields: {
                        comment: {name: 'comment', type: 'text', label: '备注'}
                    },
                    arch: `<form><field name="comment"></field></form>`
                });
                dialog.form.dom.enhanceWithin();
            },
            submit(dialog) {
                let comment = dialog.form.getData().comment;
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'createStockIn',
                    args: {
                        ids: [me.receiptId],
                        materialIds: selected,
                        comment
                    },
                    onsuccess: function (r) {
                        dialog.close();
                        if (r.data.exempted) {
                            return jmaa.msg.show(r.data.message, {delay: 8000});
                        }
                        jmaa.msg.show(r.data.message);
                    }
                });
            }
        });
    },
    showIqcList() {
        let me = this;
        me.iqcList.load();
    },
    loadIqcList(list, callback) {
        let me = this;
        let criteria = [['related_id', '=', me.receiptId]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                relatedField: 'iqc_sheet_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    }
});

