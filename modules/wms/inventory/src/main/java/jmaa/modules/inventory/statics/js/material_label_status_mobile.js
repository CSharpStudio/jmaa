//@ sourceURL=material_label_status_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.dom.find('.search-label-input').focus();
    },
    searchLabel() {
        let me = this;
        me.list.load();
    },
    loadList(list, callback) {
        let me = this;
        let input = me.dom.find('.search-label-input');
        let code = input.val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByCode',
            args: {
                code,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }, onerror(r) {
                callback({data: []});
                jmaa.msg.error(r);
            }
        });
    }
});
