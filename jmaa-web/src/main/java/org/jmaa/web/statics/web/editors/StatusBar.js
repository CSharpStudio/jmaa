jmaa.editor('statusbar', {
    css: 'e-statusbar',
    getTpl() {
        let me = this;
        let status = [];
        for (let value of me.values.reverse()) {
            status.push(`<button type="button" class="btn arrow_button disabled text-uppercase" data-value="${value}">${me.options[value]}</button>`);
        }
        return `<div class="statusbar_status">${status.join('')}</div>`;
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
        let values = dom.attr('values');
        me.values = values ? values.split(',') : Object.keys(me.field.options);
        dom.html(me.getTpl());
    },
    onValueChange(handler) {
        const me = this;
        me.dom.on('change', function (e) {
            handler(e, me);
        });
    },
    getValue() {
        return this.dom.attr('data-value');
    },
    setValue(value) {
        let me = this;
        if (value != me.getValue()) {
            me.dom.find(`button`).removeClass('arrow_button_current');
            me.dom.find(`button[extra]`).remove();
            if (me.values.includes(value)) {
                me.dom.find(`button[data-value=${value}]`).addClass('arrow_button_current');
            } else {
                me.dom.find('.statusbar_status').prepend(`<button type="button" class="btn arrow_button arrow_button_current disabled text-uppercase" extra data-value="${value}">${me.options[value]}</button>`);
            }
            me.dom.attr('data-value', value).trigger('change');
        }
    }
});
