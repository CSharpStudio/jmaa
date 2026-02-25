jmaa.editor('scanner', {
    css: 'e-scanner',
    getTpl() {
        let me = this;
        let placeholder = me.placeholder ? ` placeholder="${jmaa.utils.decode(me.placeholder)}"` : '';
        return `<a class="scan-icon">
                    <i/>
                </a>
                <input type="text"${placeholder}/>
                <button type="button" class="clear-input"></button>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.placeholder = me.nvl(dom.attr('placeholder'), me.placeholder);
        dom.html(me.getTpl()).addClass("form-control");
        let css = dom.attr('css');
        if (css) {
            dom.find('input').addClass(css);
        }
        new jmaa.widgets["scanner"]({dom});
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
});
