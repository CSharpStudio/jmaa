//@ sourceURL=model.js
jmaa.view({
    render() {
        let me = this;
        if (me.auths.includes("update")) {
            me.auths.push({'ir.ui.view': ["update"]});
        }
        me.callSuper();
    },
    initViews() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'initDefaultViews',
            args: {
                ids: [me.form.dataId],
            },
            onsuccess: function (r) {
                me.form.editors.view_ids.load();
            }
        });
    },
    onCreateView() {
        let me = this;
        return {'model': me.form.getData().model};
    }
});

jmaa.editor("view-editor", {
    extends: "editors.one2many",
    searchData(grid, callback) {
        let me = this;
        let data = me.owner.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchView',
            args: {
                criteria: [['model', '=', data.model]],
                offset: grid.pager.getOffset(),
                limit: grid.pager.getLimit(),
                fields: grid.getFields(),
                order: grid.getSort(),
            },
            context: {
                usePresent: grid.getUsePresent(),
            },
            onsuccess: function (r) {
                me.renderData(r.data, callback);
            }
        });
    },
    deleteData() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'deleteView',
            args: {
                viewIds: me.grid.getSelected(),
            },
            onsuccess: function (r) {
                me.load();
            }
        });
    },
    submitEdit(dirty, data) {
        let me = this;
        data = data || dirty;
        if (data.id) {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'updateView',
                args: {
                    viewId: data.id,
                    values: dirty,
                },
                onsuccess: function (r) {
                    me.load();
                }
            });
        } else {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'createView',
                args: {
                    values: dirty,
                },
                onsuccess: function (r) {
                    me.load();
                }
            });
        }
    },
});
