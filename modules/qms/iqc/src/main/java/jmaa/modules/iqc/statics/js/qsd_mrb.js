//@ sourceURL=qsd_mrb.js
jmaa.editor("mrb-inspect-items", {
    extends: 'editors.one2many',
    filterCriteria() {
        return [];
    },
    countData(pager) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'countInspectItems',
            args: {
                ids: [me.owner.dataId],
                criteria: me.getFilter(),
            },
            context: {
                active_test: false,
                company_test: false,
            },
            onsuccess(r) {
                pager.update({
                    total: r.data,
                });
            },
        });
    },
    searchData(grid, callback, data, settings) {
        let me = this;
        if (me.data && me.data.length > 0) return false;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchInspectItems',
            args: {
                ids: [me.owner.dataId],
                criteria: me.getFilter(),
                offset: grid.pager.getOffset(),
                limit: grid.pager.getLimit(),
                fields: grid.getFields(),
                order: grid.getSort(),
            },
            context: {
                usePresent: true,
                active_test: false,
                company_test: false,
            },
            onsuccess(r) {
                me.renderData(r.data, callback);
            },
        });
    },
});
