/** 组件的基类
 *  重写init方法开始
 */
jmaa.define('JComponent', {
    /**
     * 创建组件实例
     *
     * @param {Object} opt 初始参数
     */
    __init__(opt) {
        const me = this;
        if (opt === undefined) {
            opt = {};
        }
        const events = opt.on;
        delete opt.on;
        jmaa.utils.apply(true, me, opt);
        if (events) {
            for (const e in events) {
                const fn = events[e];
                if (typeof fn === 'function') {
                    me.on(e, fn);
                } else if (fn.selector) {
                    me.on(e, fn.selector, fn.fn);
                }
            }
        }
        let i = me.init();
        if (i instanceof Promise) {
            i.then(() => {
                me.onInit();
            });
        } else {
            me.onInit();
        }
    },
    /**
     * 模板方法，定义组件功能的入口，子类重写此方法开始。
     */
    init: jmaa.emptyFn,
    onInit: jmaa.emptyFn,
    /**
     * on (eventName, [fn], [{selector, fn}])
     * 注册事件到当前组件。
     *
     * @example myComponent.on('click', this.onClick)
     *
     * @returns this
     */
    on() {
        const me = this;
        const dom = me.dom || $('body');
        dom.on(...arguments);
        return me;
    },
    nvl() {
        for (let value of arguments) {
            if (value != null && value != undefined) {
                return value;
            }
        }
        return null;
    }
});
/**
 * 定义组件，默认继承自JComponent
 *
 * @param {String} name 名称
 * @param {Object|Function} define 定义
 * @returns class对象
 */
jmaa.component = function (name, define) {
    if (typeof define === 'function') {
        define = define();
    }
    define.extends = define.extends || ['JComponent', 'mixin.EventHandler'];
    define.xtype = define.xtype || name;
    return jmaa.define(name, define);
};
