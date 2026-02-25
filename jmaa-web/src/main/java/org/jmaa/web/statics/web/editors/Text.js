jmaa.editor('text', {
    css: 'e-text',
    getTpl() {
        let me = this;
        let placeholder = me.placeholder ? ` placeholder="${jmaa.utils.decode(me.placeholder)}"` : '';
        return `<textarea id="${this.getId()}"${placeholder} focusable rows="${this.rows}" class="form-control"/>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.rows = me.nvl(dom.attr("rows"), me.rows, 3);
        me.placeholder = dom.attr('placeholder');
        dom.html(me.getTpl());
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'textarea', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        if (readonly) {
            me.dom.find('textarea').attr('disabled', true);
        } else {
            me.dom.find('textarea').removeAttr('disabled');
        }
    },
    getValue() {
        return this.dom.find('textarea').val();
    },
    setValue(value) {
        if (value != this.getValue()) {
            this.dom.find('textarea').val(value).trigger('change');
        }
    }
});

jmaa.searchEditor('text', {
    extends: "editors.text",
    getCriteria() {
        let value = this.getValue(), values = value.split(';');
        if (values.length > 1) {
            return [[this.name, 'in', values]];
        }
        if (value) {
            return [[this.name, '=', value]];
        }
        return [];
    },
    getText() {
        return this.getValue();
    },
});
