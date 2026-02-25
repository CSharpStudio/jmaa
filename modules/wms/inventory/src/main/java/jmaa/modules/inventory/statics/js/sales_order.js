//@ sourceURL=sales_order.js
jmaa.view({
    onCreateLine(e, form) {
        let line_no = 1;
        if (form.owner.data) {
            for (let row of form.owner.data) {
                if (Number(row.line_no) >= line_no) {
                    line_no = Number(row.line_no) + 1;
                }
            }
        }
        return {line_no}
    },
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let source = form.editors.origin.getRawValue();
        let readonly = source != 'manual' || !['draft', 'reject'].includes(status);
        if (readonly) {
            form.setReadonly(true);
        }
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
    },
});
