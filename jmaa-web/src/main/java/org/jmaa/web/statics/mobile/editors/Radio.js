jmaa.editor('radio', {
    extends: 'editors.radio',
    setValue(value) {
        let me = this;
        if (value != this.getValue()) {
            let text = me.options[value];
            me.dom.find('.ui-radio-on').removeClass('ui-radio-on').addClass('ui-radio-off');
            if (text) {
                let el = me.dom.find('input[value=' + value + ']').prop('checked', true).trigger('change');
                me.dom.find(`[for=${el.attr('id')}]`).removeClass('ui-radio-off').addClass('ui-radio-on');
            } else {
                me.dom.find('input').prop('checked', false).trigger('change');
            }
        }
    },
});
