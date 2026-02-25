/**
 * 事件处理，可以使用view中的方法或者自身的方法绑定控件的事件
 */
jmaa.define('mixin.EventHandler', {
    /**
     * 注册事件
     * @param event
     * @param method
     */
    onEvent(event, method, fallback) {
        let me = this;
        if (me.design) {
            return;
        }
        let on = function (m) {
            if (typeof m == 'string') {
                let fn = me.getFunction(m);
                me.dom.on(event, fn);
            } else if (m instanceof Function) {
                me.dom.on(event, m);
            }
        }
        on(method || fallback);
    },
    /**
     * 获取view或者当前类型声明的方法
     * @param method
     * @returns {function}
     */
    getFunction(method) {
        let me = this;
        let owner = me.view || me;
        let fn = owner[method];
        if (!fn) {
            throw Error('找不到方法:' + method);
        }
        if (me.view) {
            fn = fn.bind(me.view);
        }
        return fn;
    },
});
