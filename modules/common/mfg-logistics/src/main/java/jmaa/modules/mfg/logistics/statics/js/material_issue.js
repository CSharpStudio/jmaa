//@ sourceURL=material_issue.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = !['draft', 'reject'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        form.setReadonly(readonly);
    },
    commitOrder(e, target) {
        let me = this;
        if (target.dataId) {
            values = target.getSubmitData();
        }
        jmaa.rpc({
            model: me.model,
            method: 'commit',
            args: {
                ids: target.getSelected(),
                values
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t());
                me.load();
            }
        });
    },
});
