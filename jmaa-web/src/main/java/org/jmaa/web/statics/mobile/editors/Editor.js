jmaa.define('editors.Editor', {
    extends: 'editors.Editor',
    onInit() {
        let me = this;
        me.dom.addClass(me.css);
        let init = me.dom.attr('on-init');
        if (init) {
            let v = me.view || window.view;
            const fn = new Function('return this.' + init).call(v, me);
            if (fn instanceof Function) {
                fn.call(v, me);
            }
        }
        let valueChange = me.dom.attr('on-value-change');
        if (valueChange) {
            let v = me.view || window.view;
            me.onValueChange(function (e, editor) {
                if (me.owner && me.owner.loading) return;
                const fn = new Function('return this.' + valueChange).call(v, e, editor);
                if (fn instanceof Function) {
                    fn.call(v, e, editor);
                }
            });
        }
        if (eval(me.dom.attr('focus'))) {
            me.setFocus();
        }
    },
    setFocus() {
        let me = this;
        me.dom.find('input').addClass('focus').focus();
    }
});
