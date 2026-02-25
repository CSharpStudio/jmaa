//@ sourceURL=purchase_order.js
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
        let data = form.getRaw();
        let readonly = data.origin != 'manual' || !['draft', 'reject'].includes(data.status);
        if (readonly) {
            form.setReadonly(true);
            form.editors.line_ids.readonly(false);
            form.editors.line_ids.dom.find('.btn-edit-group').hide();
            form.editors.line_ids.dom.find('[t-click=agreeDeliver]').show();
        } else {
            form.editors.line_ids.dom.find('.btn-edit-group').show();
            form.editors.line_ids.dom.find('[t-click=agreeDeliver]').hide();
        }
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
    },
    searchLine() {
        let me = this;
        me.form.editors.line_ids.load();
    },
    lineFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchInput').val();
        if (keyword) {
            criteria.push( ['material_id.code', 'like', keyword])
        }
        return criteria;
    },
});
