/**
 * 页面
 */
jmaa.component('JView', {
    /**
     * 构建实例
     * @param opt
     */
    __init__(opt) {
        const me = this;
        window.view = me;
        jmaa.utils.apply(true, me, opt);
        me.urlHash = jmaa.web.getParams(window.location.hash.substring(1));
        me.render();
        let i = me.init();
        if (i instanceof Promise) {
            i.then(() => me.dom.triggerHandler('init', [me]));
        } else {
            me.dom.triggerHandler('init', [me]);
        }
        $(document).on('click', function (e) {
            let el = $(e.target);
            if (el.is('input[type=text],input[type=number],textarea,.date-edit,.date-edit .fa') && !el.prop('disabled')) {
                $('.focus').removeClass('focus');
                el.addClass('focus');
            } else {
                $('.focus').focus();
            }
        }).on('click', '[focus-to]', function () {
            let el = $(this).attr('focus-to');
            me.setFocus(el);
        });
        setTimeout(() => $('.focus').focus(), 1000);
    },
    setFocus(el) {
        $('.focus').removeClass('focus');
        setTimeout(() => {
            $(el).attr('readonly', true).addClass('focus').focus();
            setTimeout(() => $(el).attr('readonly', false), 100);
        }, 500);
    },
    render() {
        let me = this;
        //me.dom.find('.ui-loader').show();
        let arch = jmaa.utils.parseXML(me.views.mobile.arch);
        let mobile = arch.children('mobile');
        if (!mobile.length) {
            mobile = arch;
        }
        mobile.find('t').each(function () {
            let tag = $(this);
            let text = tag.html().t();
            tag.replaceWith(text);
        });
        me.renderTabs(mobile);
        me.renderList(mobile);
        me.renderForm(mobile);
        me.renderWidget(mobile);
        me.dom.append(mobile.children()).find('.ui-page').page();
        me.dom.on('keyup', '[t-enter]', function (e) {
            if (e.keyCode == 13) {
                const enter = $(this).attr('t-enter');
                const fn = new Function('return this.' + enter).call(me, e);
                if (fn instanceof Function) {
                    fn.call(me, e);
                }
            }
        }).on('click', '[t-click]', function (e) {
            const btn = $(this);
            if (btn.attr('disabled')) {
                return;
            }
            const click = btn.attr('t-click');
            const gap = eval(btn.attr('gap') || 500);
            btn.attr('disabled', true);
            btn.attr('clicking', "1");
            const fn = new Function('return this.' + click).call(me, e);
            if (fn instanceof Function) {
                fn.call(me, e);
            }
            setTimeout(function () {
                if (btn.attr("clicking") === "1") {
                    btn.attr('disabled', false);
                }
            }, gap);
        }).on('click', '.home-link', function (e) {
            top.window.$.mobile.changePage('#main');
        }).on('click', '.back-link', function (e) {
            status.pop();
            let prev = status.pop() || 'main';
            me.changePage(prev);
        }).on('click', '.jui-tabs-nav a', function () {
            $(this).parents('.jui-tabs-nav').find('.jui-tab-active').removeClass('jui-tab-active');
            $(this).addClass('jui-tab-active');
        });
        $(document).on("pagebeforechange", function (event, data) {
            if (typeof data.toPage === "string" && data.toPage.startsWith("#")) {
                me.urlHash.page = data.toPage.substr(1);
                status.push(me.urlHash.page);
                window.location.hash = $.param(me.urlHash);
            }
        });
        let status = [];
        me.urlHash = jmaa.web.getParams(window.location.hash.substring(1));
        if (me.urlHash.page) {
            $.mobile.changePage("#" + me.urlHash.page, {transition: "none"});
        } else {
            $.mobile.changePage("#main", {transition: "none"});
        }
    },
    changePage(toPage, action) {
        $.mobile.changePage("#" + toPage, {transition: "slide"});
        if (action) {
            setTimeout(action, 100);
        }
    },
    /**
     * 解析生成JList组件
     */
    renderList(dom) {
        let me = this;
        let items = dom.find('list');
        items.each(function () {
            let item = $(this);
            let dom = $('<div></div>');
            let model = item.attr('model');
            let arch = item.prop('outerHTML');
            let ref = item.attr('ref') || '';
            let createList = function (fields) {
                let form = new JList({
                    dom,
                    arch,
                    fields,
                    view: me,
                    model: model || me.model,
                    module: me.module,
                });
                if (ref) {
                    if (me[ref]) {
                        console.error('ref[{0}]已存在:{1}'.formatArgs(ref, arch));
                    } else {
                        me[ref] = form;
                    }
                }
            }
            if (model && model != me.model) {
                me.loadFields(model).then(r => {
                    createList(r.fields);
                });
            } else {
                createList(me.fields);
            }
            item.replaceWith(dom);
        });
    },
    /**
     * 解析生成JForm组件
     */
    renderForm(dom) {
        let me = this;
        let items = dom.find('form');
        items.each(function () {
            let item = $(this);
            let dom = $('<div></div>');
            let arch = item.prop('outerHTML');
            let model = item.attr('model');
            let ref = item.attr('ref');
            let createForm = function (fields) {
                let form = dom.JForm({
                    arch,
                    fields,
                    view: me,
                    model: model || me.model,
                    module: me.module,
                });
                if (ref) {
                    if (me[ref]) {
                        console.error('ref[{0}]已存在:{1}'.formatArgs(ref, arch));
                    } else {
                        me[ref] = form;
                    }
                }
                return form;
            }
            if (model && model != me.model) {
                me.loadFields(model).then(r => {
                    createForm(r.fields).dom.enhanceWithin();
                });
            } else {
                createForm(me.fields);
            }
            item.replaceWith(dom);
        });
    },
    /**
     * 解析生成JTabs组件
     */
    renderTabs(dom) {
        let me = this;
        let items = dom.find('tabs');
        for (let i = items.length; i > 0; i--) {
            let item = $(items[i - 1]);
            let ref = item.attr('ref');
            let tabs = item.JTabs({
                view: me
            });
            item.replaceWith(tabs.dom);
            if (ref) {
                if (me[ref]) {
                    console.error('ref[{0}]已存在:{1}'.formatArgs(ref, item.prop('outerHTML')));
                } else {
                    me[ref] = tabs;
                }
            }
        }
    },
    renderWidget(dom) {
        let me = this;
        let widgets = dom.find('[widget]');
        widgets.each(function () {
            let item = $(this);
            let ref = item.attr('ref');
            let name = item.attr('widget');
            let widget = new jmaa.widgets[name]({dom: item});
            if (ref) {
                if (me[ref]) {
                    console.error('ref[{0}]已存在:{1}'.formatArgs(ref, item.prop('outerHTML')));
                } else {
                    me[ref] = widget;
                }
            }
        });
    },
    /**
     * 获取声明在JView上的方法
     */
    getFunction(method) {
        let fn = this[method];
        if (!fn) {
            throw Error('找不到方法:' + method);
        }
        return fn;
    },
    /**
     * 调用loadFields服务
     * @example
     * me.loadView('rbac.user').then((fields)=>{
     *     ...
     * });
     */
    loadFields(model) {
        return new Promise((resolve, reject) => {
            jmaa.rpc({
                model: "ir.ui.view",
                module: "base",
                method: "loadFields",
                args: {
                    model: model
                },
                onsuccess: function (r) {
                    resolve(r.data);
                },
                onerror: (error) => {
                    reject(error);
                    console.error(error);
                }
            });
        });
    },
    /**
     * 调用loadView服务
     * @example
     * me.loadView('rbac.user', 'grid,form').then((view)=>{
     *     ...
     * });
     */
    loadView(model, type, key) {
        return new Promise((resolve, reject) => {
            jmaa.rpc({
                model: "ir.ui.view",
                module: "base",
                method: "loadView",
                args: {
                    model: model,
                    type: type,
                    key: key
                },
                onsuccess: function (r) {
                    resolve(r.data);
                },
                onerror: (error) => {
                    reject(error);
                    console.error(error);
                }
            });
        });
    },
});

$(function () {
    let theme = localStorage.getItem('user_theme');
    if (theme) {
        $('html').removeClass().addClass(theme + " ui-mobile");
    }
    let user = JSON.parse(localStorage.getItem("user_info") || '{}');
    let company = JSON.parse(localStorage.getItem("company_info") || '{}');
    window.env = {
        user,
        company,
    };
    const ps = jmaa.web.getParams(window.location.hash.substring(1));
    jmaa.rpc({
        model: 'ir.ui.view',
        method: 'loadView',
        args: {
            model: ps.model,
            type: ps.view,
            key: ps.key,
        },
        onsuccess(r) {
            if (r.data.resource) {
                $('head').append(r.data.resource);
            }
            r.data.dom = $('body');
            new JView(r.data);
        },
    });
});

/**
 * 定义扩展的JView视图
 *
 * @example
 * jmaa.view({
 * })
 *
 * @param {Object} define
 */
jmaa.view = function (define) {
    if (typeof define === "function") {
        define = define();
    }
    define.extends = define.extends || 'JView';
    jmaa.define('JView', define);
};
//适配
jmaa.column = function () {
}
