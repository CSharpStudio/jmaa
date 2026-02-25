//@ sourceURL=material_stock_in_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.stockInList.load();
        me.dom.find('[name=scope]').on('change', function () {
            me.detailsList.load();
        });
    },
    searchStockIn() {
        let me = this;
        me.stockInForm.editors.result.reset()
        me.stockInList.load()
    },
    focusOnCode() {
        let me = this;
        me.dom.find('.code-input').focus();
    },
    scanCode() {
        let me = this;
        me.sendCode();
    },
    submitCode() {
        let me = this;
        if (!me.stockInForm.valid()) {
            return jmaa.msg.error(me.stockInForm.getErrors());
        }
        let code = me.codeScanner.getValue();
        if (!code) {
            me.mediaError.play();
            return jmaa.msg.error('标签条码不能为空'.t());
        }
        me.sendCode(true);
    },
    sendCode(confirm) {
        let me = this;
        let code = me.codeScanner.getValue();
        if (!code) {
            return;
        }
        let form = me.stockInForm;
        let raw = form.getRaw();
        let autoConfirm = me.dom.find('#check-auto').is(":checked");
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockIn',
            args: {
                ids: [me.stockInId],
                code,
                submit: confirm || autoConfirm,
                location: raw.location,
                materialId: raw.material_id,
                confirmQty: !confirm && autoConfirm ? null : raw.confirm_qty,
            },
            onerror(r) {
                me.mediaError.play();
                me.codeScanner.setValue('');
                if (r.code == 1000) {
                    raw.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
                me.focusOnCode();
            },
            onsuccess: function (r) {
                let data = r.data.data;
                data.location = form.editors.location.getValue();
                form.setData(data);
                form.setEditorRequired("location", data.location_manage);
                if (r.data.submit) {
                    me.mediaSubmit.play();
                    me.codeScanner.setValue('');
                    form.editors.confirm_qty.resetValue();
                } else {
                    me.mediaScan.play();
                    me.codeScanner.setValue(code);
                }
                raw.result = {msg: r.data.message};
                me.focusOnCode();
            }
        });
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['stock_in_id', '=', me.stockInId]];
        let scope = me.dom.find('[name=scope]:checked').val();
        if (scope != 'all') {
            criteria.push(['status', '=', scope]);
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
    searchStockInDetails() {
        let me = this;
        me.detailsList.load();
    },
    loadStockInList(list, callback) {
        let me = this;
        let code = me.dom.find('.search-order-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByCode',
            args: {
                code,
                criteria: [],
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data})
            }
        });
    },
    openStockDetails() {
        let me = this;
        me.changePage("details");
        let data = me.stockInList.getSelectedData()[0];
        me.stockInId = data.id;
        me.dom.find(".stock-in-code").html(data.code);
        me.stockInForm.setData({});
        me.codeScanner.setValue('');
        me.detailsList.load();
    },
})
