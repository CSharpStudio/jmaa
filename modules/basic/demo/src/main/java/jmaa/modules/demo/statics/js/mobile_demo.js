//@ sourceURL=field_type.js
jmaa.view({
    init() {
        let me = this;
        me.keyword = '';
        me.list1.load();
        me.list2.load();
    },
    onListClick(e, target, id) {
        let me = this;
        me.changePage("page-form");
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'read',
            args: {
                ids: [id],
                fields: me.form.getFields(),
            },
            context: {
                usePresent: me.form.getUsePresent(),
            },
            onsuccess: function (r) {
                me.form.loadData(r.data[0]);
            }
        });
    },
    searchList(e) {
        let me = this;
        let input = $(e.target);
        me.keyword = input.val();
        input.val('');
        if (input.attr('for') == 'list1') {
            me.list1.load();
        } else {
            me.list2.load();
        }
    },
    loadList(list, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'search',
            args: {
                criteria: [['f_char', 'like', me.keyword]],
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
    onListInit() {
        console.log('list init')
    },
    onListLoad(e, list) {
        console.log('list load')
    },
    onListReload(e, list) {
        let me = this;
        me.keyword = '';
    },
    showPage(e, target) {
        let me = this;
        let to = $(e.target).attr('to');
        me.changePage(to);
    }
});
