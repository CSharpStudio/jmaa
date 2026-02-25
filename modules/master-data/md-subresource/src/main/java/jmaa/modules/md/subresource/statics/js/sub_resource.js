//@ sourceURL=sub_resource.js
jmaa.editor('work-resource', {
    extends: 'editors.one2many',
    searchData(grid, callback) {
        let me = this;
        jmaa.rpc({
            model: me.owner.model,
            module: me.module,
            method: 'searchWorkResource',
            args: {
                ids: [me.owner.dataId]
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                callback({
                    data: r.data,
                });
            },
        });
    },
})
