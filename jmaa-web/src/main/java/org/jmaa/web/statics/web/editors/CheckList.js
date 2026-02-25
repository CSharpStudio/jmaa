jmaa.editor('checklist', {
    extends: 'editors.radio',
    css: 'e-checklist',
    type: 'checkbox',
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', function (e) {
            handler(e, me);
        });
    },
    getValue() {
        let me = this;
        let values = [];
        me.dom.find("input:checkbox:checked").each(function () {
            values.push($(this).val());
        });
        return values.join();
    },
    setValue(value) {
        let me = this;
        if (value != this.getValue()) {
            me.dom.find('input').prop("checked", false);
            if (value) {
                let values = value.split(',');
                for (let key of values) {
                    me.dom.find("input[value=" + key + "]").prop('checked', true);
                }
            }
            me.dom.trigger('change');
        }
    },
});

jmaa.searchEditor('checklist', {
    extends: "editors.checklist",
    getCriteria() {
        let me = this;
        let values = [];
        me.dom.find("input:checkbox:checked").each(function () {
            values.push($(this).val());
        });
        if (values.length > 0) {
            return [[me.name, 'in', values]];
        }
        return [];
    },
    getText() {
        let me = this;
        let text = [];
        me.dom.find("input:checkbox:checked").each(function () {
            text.push(me.options[$(this).val()]);
        });
        return text.join();
    },
});
