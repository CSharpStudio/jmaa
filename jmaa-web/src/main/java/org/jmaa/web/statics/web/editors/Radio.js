jmaa.editor('radio', {
    css: 'e-radio',
    type: 'radio',
    getTpl() {
        let me = this;
        let name = me.name + "-group-" + jmaa.nextId();
        let options = [];
        for (let key in me.options) {
            let id = me.name + "-" + jmaa.nextId();
            options.push(`<input id="${id}" type="${me.type}" name="${name}" value="${key}"/>
                          <label for="${id}">${me.options[key]}</label>`)
        }
        return `<div id="${this.getId()}">
                    ${options.join('')}
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        let opt = dom.attr('options');
        if (opt) {
            me.options = eval("(" + opt + ")");
        } else {
            me.options = me.nvl(me.options, me.field.options || {});
        }
        dom.html(me.getTpl());
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'input', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        if (readonly) {
            this.dom.find('input').prop('disabled', true);
        } else {
            this.dom.find('input').prop('disabled', false);
        }
    },
    getValue() {
        let checked = this.dom.find('input:radio:checked');
        return checked.length ? checked.val() : null;
    },
    setValue(value) {
        let me = this;
        if (value != this.getValue()) {
            let text = me.options[value];
            if (text) {
                me.dom.find('input[value=' + value + ']').prop('checked', true).trigger('change');
            } else {
                me.dom.find('input').prop('checked', false).trigger('change');
            }
        }
    },
});

jmaa.searchEditor('radio', {
    extends: 'editors.radio',
    getCriteria() {
        let value = this.getValue();
        if (value) {
            return [[this.name, '=', value]];
        }
        return [];
    },
    getText() {
        return this.options[this.getValue()];
    },
});
