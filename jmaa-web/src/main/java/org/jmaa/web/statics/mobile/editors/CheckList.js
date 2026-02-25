jmaa.editor('checklist', {
    extends: 'editors.checklist',
    setValue(value) {
        let me = this;
        if (value != this.getValue()) {
            me.dom.find('input').prop("checked", false).removeClass('ui-checkbox-on').addClass('ui-checkbox-off');
            if (value) {
                let values = value.split(',');
                for (let key of values) {
                    let el = me.dom.find("input[value=" + key + "]").prop('checked', true);
                    me.dom.find(`[for=${el.attr('id')}]`).removeClass('ui-checkbox-off').addClass('ui-checkbox-on');
                }
            }
            me.dom.trigger('change');
        }
    },
});
