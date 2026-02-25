jmaa.editor('char', {
    css: 'e-char',
    getTpl() {
        let me = this;
        let placeholder = me.placeholder ? ` placeholder="${jmaa.utils.decode(me.placeholder)}"` : '';
        return `<input type="text"${placeholder} focusable autocomplete="off" class="form-control" id="${this.getId()}"/>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.placeholder = me.nvl(dom.attr('placeholder'), me.placeholder);
        me.trim = me.nvl(eval(dom.attr('trim')), me.trim, me.field.trim);
        me.length = me.nvl(eval(dom.attr('length')), me.length, me.field.length);
        dom.html(me.getTpl());
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'input:text', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        this.dom.find('input').attr('disabled', readonly);
    },
    getValue() {
        let me = this;
        let value = me.dom.find('input').val();
        if (me.trim) {
            value = value.trim();
        }
        return value;
    },
    setValue(value) {
        let me = this;
        if (value != me.getValue()) {
            me.dom.find('input').val(value).trigger('change');
        }
    },
    valid() {
        let me = this;
        let value = this.getValue();
        if (me.length > 0 && value.length > me.length) {
            return '当前长度{0}超过最大长度{1}'.t().formatArgs(value.length, me.length);
        }
    },
});

jmaa.searchEditor('char', {
    extends: 'editors.char',
    getCriteria() {
        let value = this.getValue();
        if (value) {
            const values = value.trim().split(';');
            if (values.length > 1) {
                return [[this.name, 'in', values]];
            }
            if (value.endsWith("$")) {
                return [[this.name, '=', value.substr(0, value.length - 1)]];
            }
            return [[this.name, 'like', value.trim()]];
        }
        return [];
    },
    getText() {
        return this.getValue();
    },
});
