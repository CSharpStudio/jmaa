//@ sourceURL=purchase_order.js
jmaa.view({
    createLine(e, target) {
        let me = this;
        let data = me.form.getData();
        if (!data.supplier_id) {
            me.form.setInvalid("supplier_id", "不能为空");
            return jmaa.msg.error('请先选择供应商'.t());
        }
        target.create();
    },
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
        let readonly = !['draft', 'reject'].includes(data.status);
        if (readonly) {
            form.setReadonly(true);
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
            criteria.push(['material_id.code', 'like', keyword])
        }
        return criteria;
    },
    filterMaterial(criteria, target) {
        let me = this;
        let data = me.form.getRaw();
        criteria.push(['supplier_material_ids.supplier_id', '=', data.supplier_id]);
        return criteria;
    }
});
