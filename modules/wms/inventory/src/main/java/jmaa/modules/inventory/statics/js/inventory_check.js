//@ sourceURL=inventory_balance.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = !['draft', 'reject'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        form.setReadonly(readonly);
    },
    searchLine() {
        let me = this;
        me.form.editors.line_ids.load();
    },
    searchDetails() {
        let me = this;
        me.form.editors.details_ids.load();
    },
    lineFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchLineInput').val();
        if (keyword) {
            criteria.push(['material_id.code', 'like', keyword]);
        }
        return criteria;
    },
    detailsFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchDetailsInput').val();
        if (keyword) {
            criteria.push(['material_id.code', 'like', keyword])
        }
        return criteria;
    },
    copyOrder(e) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'inventoryBalance',
            args: {
                ids: me.getSelected(),
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t());
                me.load();
            }
        });
    },
    generateData() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'generateData',
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t());
                me.load();
            }
        });
    }
})
