jmaa.editor('card-item', {
    noEdit: true,
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', function (e) {
            handler(e, me);
        });
    },
    setValue(value) {
        let me = this;
        if (Array.isArray(value)) {
            me.dom.html(`<span data-value="${value[0]}">${value[1]}</span>`);
        } else if (me.field.type == 'selection') {
            me.dom.html(`<span data-value="${value}">${me.field.options[value]}</span>`);
        } else {
            if (value == undefined || value == null) {
                value = "";
            }
            me.dom.html(`<span>${value}</span>`);
        }
        me.dom.trigger('change');
    },
    getValue() {
        let me = this;
        let value = me.dom.find('span').attr('data-value');
        if (value) {
            return [value, me.dom.find('span').html()];
        }
        return me.dom.find('span').html();
    },
    getRawValue() {
        let me = this;
        return me.dom.find('span').attr('data-value') || me.dom.find('span').html();
    }
});
