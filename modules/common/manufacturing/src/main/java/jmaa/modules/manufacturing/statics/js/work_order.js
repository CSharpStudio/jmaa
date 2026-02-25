//@ sourceURL=work_order.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let data = form.getRaw();
        let readonly = !['draft', 'suspend'].includes(data.status);
        if (readonly) {
            for (let editor in form.editors) {
                if (!['material_name_spec', 'material_category', 'unit_id'].includes(editor)) {
                    form.editors[editor].readonly(readonly);
                }
            }
        }
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
    },
    cancelOrder(e, target) {
        this.changeStatus(target.getSelected(), '取消', 'cancel');
    },
    suspendOrder(e, target) {
        this.changeStatus(target.getSelected(), '暂停', 'suspend');
    },
    releaseOrder(e, target) {
        this.saveAndChangeStatus(target, '发放', 'release');
    },
});
