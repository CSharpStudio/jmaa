//@ sourceURL=field_type.js
jmaa.view({
    searchBy() {
        let me = this, input = me.dom.find('.search-input'), code = input.val();
        if (code) {
            me.search.remove("txt-search");
            code = code.trim();
            let criteria = ['|', ['f_char', 'like', code], ['f_text', 'like', code]];
            me.search.add("txt-search", "文本或大文本".t(), code, criteria);
            input.val('');
            me.load();
        }
    },
    test(e, t) {
        console.log(t.getSelected())
    },
    onGridInit() {
        console.log('grid-init', arguments)
    },
    onGridLoad() {
        console.log('grid-load', arguments)
    },
    onGridCreate() {
        console.log('grid-create', arguments)
        return new Promise(resolve => setTimeout(() => resolve({f_bool: true, f_char: 'new'}), 500));
    },
    onGridSave() {
        console.log('grid-save', arguments)
    },
    onGridValid() {
        console.log('grid-valid', arguments)
        return '验证不通过';
    },
    onEditLoad() {
        console.log('edit-load', arguments)
    },
    onGridSelected() {
        console.log('grid-selected', arguments)
    },
    onGridDblClick(e, grid, id) {
        let me = this;
        console.log('grid-dblclick', arguments)
        const btn = me.toolbar.dom.find("[name='edit']");
        if (btn.length > 0) {
            btn.click();
        } else {
            me.browse(e, grid);
        }
    },
    onFormInit() {
        console.log('form-init', arguments)
    },
    onFormLoad() {
        console.log('form-load', arguments)
    },
    onFormSelected() {
        console.log('form-selected', arguments)
    },
    onFormCreate(e, form, values) {
        console.log('form-create', arguments);
        return new Promise(resolve => setTimeout(() => resolve({f_bool: true, f_char: 'new'}), 500));
        //return {f_bool:true, f_char:'new'};
    },
    onFormValid() {
        console.log('form-valid', arguments)
    },
    onFormSave() {
        console.log('form-save', arguments)
    },
    setReadonly() {
        let me = this;
        me.readonly = !me.readonly;
        me.form.setReadonly(me.readonly);
    }
});
