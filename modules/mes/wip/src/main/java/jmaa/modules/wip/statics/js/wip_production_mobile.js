//@ sourceURL=wip_production_mobile.js
jmaa.view({
    searchByCode() {
        let me = this;
        me.wipList.load();
    },
    searchProcess() {
        let me = this;
        me.processList.load();
    },
    searchModule() {
        let me = this;
        me.moduleList.load();
    },
    searchDefect() {
        let me = this;
        me.defectList.load();
    },
    searchRepair() {
        let me = this;
        me.repairList.load();
    },
    openDetail() {
        let me = this;
        let selected = me.wipList.getSelected();
        if (selected.length) {
            me.wipId = selected[0];
            me.changePage("detail");
            me.tabs.open('processTab');
            me.processList.load();
        }
    },
    loadProcessList(list, callback) {
        let me = this;
        me.loadList(list, callback, "searchProcess");
    },
    loadModuleList(list, callback) {
        let me = this;
        me.loadList(list, callback, "searchModule");
    },
    loadDefectList(list, callback) {
        let me = this;
        me.loadList(list, callback, "searchDefect");
    },
    loadRepairList(list, callback) {
        let me = this;
        me.loadList(list, callback, "searchRepair");
    },
    loadList(list, callback, method) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method,
            args: {
                wipId: me.wipId,
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
    loadWipList(list, callback) {
        let me = this;
        let code = me.dom.find('.search-input').val();
        if (!code) {
            callback({data: []});
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByCode',
            args: {
                code,
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
    }
});
