/**
 * 自定义控件
 */
jmaa.component("JCustom", {
    init() {
        let me = this;
        if (me.arch) {
            let arch = jmaa.utils.parseXML(me.arch), custom = arch.children('custom');
            if (custom.length > 0) {
                $.each(custom[0].attributes, function (i, attr) {
                    if (attr.name === 'class') {
                        me.dom.addClass(attr.value);
                    } else {
                        let v = encodeURI(attr.value);
                        me.dom.attr(attr.name, v);
                    }
                });
                me.dom.append(custom.prop('innerHTML'));
            }
        }
        me.dom.triggerHandler('init', [me]);
    }
});
