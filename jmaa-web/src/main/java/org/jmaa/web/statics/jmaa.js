window.jmaa = window.jmaa || {};

(function () {
    if (typeof String.prototype.replaceAll === 'undefined') {
        String.prototype.replaceAll = function (match, replace) {
            return this.replace(new RegExp(match, 'g'), () => replace);
        };
    }
    /** 格式化，如 '{0}({1})'.formatArgs(1,2) 或者 '{name}({code})'.formatArgs({name:'x',code:'y'}) */
    String.prototype.formatArgs = function (args) {
        if (arguments.length > 0) {
            let result = this;
            if (arguments.length == 1 && typeof (args) == "object") {
                for (let key in args) {
                    let reg = new RegExp("({" + key + "})", "g");
                    result = result.replace(reg, args[key]);
                }
            } else {
                for (let i = 0; i < arguments.length; i++) {
                    if (arguments[i] != undefined) {
                        let reg = new RegExp("({[" + i + "]})", "g");
                        result = result.replace(reg, arguments[i]);
                    }
                }
            }
            return result;
        } else {
            return this;
        }
    }
    /** 字符翻译，如 "名称".t() */
    String.prototype.t = function () {
        const str = String(this);
        if (!str || this == window) {
            return str;
        }
        if (!top.window.langData) {
            const lang = jmaa.web.cookie('ctx_lang');
            const data = localStorage.getItem('lang-' + lang);
            top.window.langData = data ? JSON.parse(data) : {};
        }
        const value = top.window.langData[str];
        if (!value && top.window.isDebug) {
            if (!top.window.langCollect) {
                top.window.langCollect = {};
            }
            top.window.langCollect[str] = 0;
        }
        return value || str;
    };
    /** 从数组移除指定项，如 [1,2].remove(1) 或者 [1,2].remove(item=>item==1) */
    Array.prototype.remove = function (e) {
        if (e instanceof Function) {
            let toDelete = this.filter(e);
            for (let item of toDelete) {
                this.splice(this.indexOf(item), 1);
            }
        } else {
            const idx = this.indexOf(e);
            if (idx > -1) {
                this.splice(idx, 1);
            }
        }
    };
    Array.prototype.equals = function (array) {
        if (this.length != array.length) {
            return false;
        }
        this.sort();
        array.sort();
        return this.every((v, i) => v === array[i]);
    }
    /** 格式化日期 如：new Date().format('yyyy-MM-dd HH:mm:ss') */
    Date.prototype.format = function (fmt) {
        const o = {
            'M+': this.getMonth() + 1, // 月份
            'D+': this.getDate(), // 日
            'd+': this.getDate(), // 日
            'h+': this.getHours() % 12 == 0 ? 12 : this.getHours() % 12, // 小时
            'H+': this.getHours(), // 小时
            'm+': this.getMinutes(), // 分
            's+': this.getSeconds(), // 秒
            'q+': Math.floor((this.getMonth() + 3) / 3), // 季度
            S: this.getMilliseconds(), // 毫秒
        };
        if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length));
        if (/(Y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length));
        for (const k in o) if (new RegExp('(' + k + ')').test(fmt)) fmt = fmt.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ('00' + o[k]).substr(('' + o[k]).length));
        return fmt;
    };
    /** 全局id */
    let globaleId = 0;
    /** 获取全局id */
    jmaa.nextId = function () {
        return globaleId++;
    };
    jmaa.emptyFn = function () {
    };
    /** 工具 */
    jmaa.utils = {
        /** 参考 jQuery.extend 方法，使用 defineMethod 处理 Function，实现类的继承方法可以通过 callParent 调用父类的方法 */
        apply: function () {
            let options;
            let name;
            let src;
            let copy;
            let copyIsArray;
            let clone;
            let target = arguments[0] || {};
            let i = 1;
            const length = arguments.length;
            let deep = false;
            if (typeof target === 'boolean') {
                deep = target;
                target = arguments[1] || {};
                i = 2;
            }
            if (typeof target !== 'object' && !jQuery.isFunction(target)) {
                target = {};
            }
            if (length === i) {
                target = this;
                --i;
            }
            for (; i < length; i++) {
                if ((options = arguments[i]) != null) {
                    for (name in options) {
                        if (name && name[0] === '$') {
                            continue;
                        }
                        src = target[name];
                        copy = options[name];
                        if (target === copy) {
                            continue;
                        }
                        if (deep && copy && (jQuery.isPlainObject(copy) || (copyIsArray = jQuery.isArray(copy)))) {
                            if (copyIsArray) {
                                copyIsArray = false;
                                clone = src && jQuery.isArray(src) ? jQuery.extend(deep, [], src) : [];
                            } else {
                                clone = src && jQuery.isPlainObject(src) ? jQuery.extend(deep, {}, src) : {};
                            }
                            target[name] = jQuery.extend(deep, clone, copy);
                        } else if (copy !== undefined) {
                            if (copy instanceof Function) {
                                jmaa.utils.defineMethod(target, name, copy);
                            } else {
                                target[name] = copy;
                            }
                        }
                    }
                }
            }
            return target;
        },
        /** 定义方法 */
        defineMethod: function (owner, name, body) {
            const clone = function (method) {
                let newMethod, prop;
                newMethod = function () {
                    return method.apply(this, arguments);
                };
                for (prop in method) {
                    if (method.hasOwnProperty(prop)) {
                        newMethod[prop] = method[prop];
                    }
                }
                return newMethod;
            };
            if (body.$owner) {
                const origin = body;
                body = clone(body);
                body.$origin = origin;
            }
            owner[name] = body;
            body.$name = name;
            body.$owner = owner.$class;
        },
        /** 解析xml */
        parseXML: function (xmlStr) {
            return $('<root>' + xmlStr + '</root>');
        },
        /** 16位随机id */
        randomId: function () {
            let guid = '';
            for (let i = 1; i <= 16; i++) {
                const n = Math.floor(Math.random() * 16.0).toString(16);
                guid += n;
            }
            return guid;
        },
        encode: function (s) {
            return s ? s.replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '%22') : s;
        },
        decode: function (s) {
            return s ? s.replaceAll('&lt;', '<').replaceAll('&gt;', '>').replaceAll('%22', '"') : s;
        },
    };
    jmaa.web = {
        /** 获取 url 参数的值 */
        getUrlParam: function (name) {
            const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
            const r = window.location.search.substr(1).match(reg);
            if (r != null) return unescape(r[2]);
            return null;
        },
        /** 获取 url 参数转换的对象 */
        getParams: function (search) {
            const o = {};
            const re = /([^&=]+)=([^&]*)/g;
            let m;
            while ((m = re.exec(search))) {
                o[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
            }
            return o;
        },
        /** 获取租户，从url解析 */
        getTenantPath: function () {
            const parts = window.location.pathname.substring(1).split('/');
            return '/' + parts[0];
        },
        /** cookie读写 */
        cookie: function (name, value, options) {
            if (typeof value !== 'undefined') {
                options = options || {};
                if (value === null) {
                    value = '';
                    options = $.extend({}, options);
                    options.expires = -1;
                }
                let expires = '';
                if (options.expires && (typeof options.expires === 'number' || options.expires.toUTCString)) {
                    let date;
                    if (typeof options.expires === 'number') {
                        date = new Date();
                        date.setTime(date.getTime() + options.expires * 24 * 60 * 60 * 1000);
                    } else {
                        date = options.expires;
                    }
                    expires = '; expires=' + date.toUTCString();
                }
                const path = options.path ? '; path=' + options.path : ';path=/';
                const domain = options.domain ? '; domain=' + options.domain : '';
                const secure = options.secure ? '; secure' : '';
                document.cookie = [name, '=', encodeURIComponent(value), expires, path, domain, secure].join('');
            } else {
                let cookieValue = null;
                if (document.cookie && document.cookie != '') {
                    const cookies = document.cookie.split(';');
                    for (let i = 0; i < cookies.length; i++) {
                        const cookie = jQuery.trim(cookies[i]);
                        if (cookie.substring(0, name.length + 1) == name + '=') {
                            cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                            break;
                        }
                    }
                }
                return cookieValue;
            }
        },
    };
    /** 请求模型的服务 */
    jmaa.rpc = function (opt) {
        opt = opt || {};
        const model = opt.model;
        const method = opt.method;
        const context = opt.context || {};
        const args = opt.args || {};
        const dialog = opt.dialog;
        const defaults = {
            url: jmaa.web.getTenantPath() + '/rpc/service?module=' + (opt.module || 'base'),
            type: 'POST',
            dataType: 'json',
            data: {
                id: Date.now(),
                jsonrpc: '2.0',
                method,
                params: {
                    args,
                    context,
                    model,
                },
            },
            contentType: 'application/json; charset=utf-8',
            onsuccess: function (data) {
            },
            onerror: function (err) {
                if (dialog) {
                    dialog.busy(false);
                }
                jmaa.msg.error(err);
            },
            success: function (rs, status, xhr) {
                if (rs.trace) {
                    console.log("Trace:", rs.trace);
                }
                if (rs.error) {
                    if (window.postError) {
                        window.postError(rs.error);
                    }
                    rs.error.request = defaults;
                    if (rs.error.code === 7100) {
                        // 未授权，刷新转跳到登录
                        top.window.location.reload();
                    } else {
                        options.onerror(rs.error);
                    }
                } else {
                    const result = rs.result;
                    options.onsuccess(result);
                    if (dialog) {
                        dialog.busy(false);
                    }
                    if (result && result.context && result.context.token) {
                        let token = jmaa.web.cookie('ctx_token');
                        if (token != result.context.token) {
                            jmaa.web.cookie('ctx_token', token);
                        }
                    }
                }
            },
            error: function (rs) {
                console.log(rs);
                const err = {data: {request: defaults, error: rs.responseText, status: rs.status}};
                if (window.postError) {
                    window.postError(err);
                }
                jmaa.msg.error(err);
            },
        };
        if (opt.params) {
            defaults.data.params = opt.params;
        }
        if (opt.data) {
            defaults.data = opt.data;
        }
        delete opt.context;
        delete opt.args;
        delete opt.module;
        delete opt.method;
        delete opt.model;
        delete opt.dialog;
        delete opt.params;
        const options = $.extend(true, defaults, opt);
        options.data = JSON.stringify(options.data);
        $.ajax(options);
    };
    jmaa.msg = {
        show(msg, opt) {
            $(document).Toasts(
                'create',
                $.extend(
                    {
                        class: 'msg bg-success',
                        position: 'bottomRight',
                        title: '成功'.t(),
                        icon: 'fa fa-exclamation-circle',
                        autohide: true,
                        delay: 2000,
                        body: `<div style="min-width:200px">${msg}</div>`,
                    },
                    opt,
                ),
            );
        },
        error(err, opt) {
            if (typeof err === 'string') {
                $(document).Toasts(
                    'create',
                    $.extend(
                        {
                            class: 'msg bg-danger',
                            title: '发生错误'.t(),
                            icon: 'fa fa-exclamation-triangle',
                            autohide: true,
                            delay: 5000,
                            body: `<div style="min-width:200px">${err}</div>`,
                        },
                        opt,
                    ),
                );
            } else if (err.data === undefined || [1000, 1020, 7102, 7110].indexOf(err.code) > -1) {
                // ValidationException
                $(document).Toasts(
                    'create',
                    $.extend(
                        {
                            class: 'msg bg-danger',
                            title: err.code == 1000 ? '验证不通过'.t() : '发生错误'.t(),
                            icon: 'fa fa-exclamation-triangle',
                            autohide: true,
                            delay: 5000,
                            body: `<div style="min-width:200px">${err.message ? err.message.replaceAll('\r\n', '<br/>') : err.message}</div>`,
                        },
                        opt,
                    ),
                );
            } else {
                console.error(err);
                // 其它类型错误
                jmaa.showDialog({
                    title: '发生错误'.t(),
                    bgClass: 'bg-secondary',
                    id: 'error-dialog',
                    init(dialog) {
                        dialog.body.addClass('m-3');
                        dialog.body.html(`<div>
                                <span>${'抱歉，系统发生错误，请尝试重新操作。'.t()}<br/>${'如果无法解决，请复制错误信息或者打开详情截屏发给系统管理员。'.t()}</span>
                                <div class="pt-3 pb-1">
                                    <button type="button" class="btn btn-flat btn-info" onclick="$('.detail').show()">${'详情'.t()}</button>
                                    <button type="button" class="btn btn-flat btn-info" onclick="navigator.clipboard.writeText($('.detail').html());alert('复制成功')">${'复制错误信息'.t()}</button>
                                </div>
                                <div style="display: none;word-wrap: break-word;white-space: pre-wrap;" class="detail">${JSON.stringify(err, null, "\t")}</div>
                            </div>`);
                    },
                });
            }
        },
        confirm(options) {
            const opt = $.extend(
                {
                    title: '确认'.t(),
                    content: '',
                    cancelButton: `<button autofocus class="btn ui-btn btn-default cancel">${'取消'.t()}</button>`,
                    submitButton: `<button class="btn ui-btn btn-primary submit">${'确认'.t()}</button>`,
                    submit: jmaa.emptyFn,
                    cancel: jmaa.emptyFn,
                },
                options,
            );
            const tpl = `<div class="jui-confirm">
                            <div class="confirm-mask"></div>
                            <div class="confirm">
                                <div class="confirm-header">${opt.title}</div>
                                <div class="confirm-body">${opt.content}</div>
                                <div class="confirm-footer">
                                    ${opt.cancelButton}
                                    ${opt.submitButton}
                                </div>
                            </div>
                        </div>`;
            let active = document.activeElement;
            $('.jui-confirm').remove();
            $(document.body).append(tpl);
            $('.btn.cancel').on('click', () => {
                opt.cancel();
                $('.jui-confirm').remove();
                $(active).focus();
            });
            $('.btn.submit').on('click', () => {
                opt.submit();
                $('.jui-confirm').remove();
                $(active).focus();
            });
        },
        speakText(text) {
            if (window.NativeJS) {
                window.NativeJS.speakText(text);
            } else if (window.speechSynthesis) {
                const utterance = new SpeechSynthesisUtterance(text);
                utterance.lang = 'zh-CN';
                utterance.rate = 1.2; // 语速（0.1-10，默认1）
                utterance.pitch = 1; // 音调（0-2，默认1）
                utterance.volume = 1; // 音量（0-1，默认1）
                window.speechSynthesis.speak(utterance);
            } else {
                console.error("不支持语音播报");
            }
        }
    };
    /** 所有定义的类型 */
    jmaa.types = {};
    /** 定义类，使用 extends 指定要继承的父类，如：
     * jmaa.define("a", {});
     * jmaa.define("b", { extends: "a"});
     * jmaa.define("c", { extends: ["b", "a"]});
     * jmaa.define("a", { extends: "a"});
     */
    jmaa.define = function (name, define) {
        const clz = function () {
            this.__init__(...arguments);
        };
        const prop = clz.prototype;
        const addBase = function (b) {
            const base = jmaa.types[b];
            if (base) {
                clz.$bases.push(base);
                jmaa.utils.apply(true, prop, base.prototype);
            } else {
                throw new Error(name + ' 不能扩展未定义的 ' + b);
            }
        };
        prop.$class = clz;
        if (typeof define === 'function') {
            define = define();
        }
        define = define || {};
        jmaa.utils.defineMethod(prop, '__init__', function () {
        });
        clz.$bases = [];
        if (typeof define.extends === 'string') {
            addBase(define.extends);
        } else if (jQuery.isArray(define.extends)) {
            for (let i = define.extends.length - 1; i >= 0; i--) {
                const b = define.extends[i];
                if (typeof b === 'string') {
                    addBase(b);
                }
            }
        }
        jmaa.utils.apply(true, prop, define);
        prop.callSuper = function () {
            if (!this.$super) {
                this.$super = {};
            }
            const stack = new Error().stack.split('\n')[2].trim().split(/\s+/)[1].trim().split('.');
            const name = stack[stack.length - 1];
            const bases = this.$super[name]?.$bases || this.$class.$bases;
            for (let i = bases.length - 1; i >= 0; i--) {
                let baseMethod = bases[i].prototype[name];
                if (baseMethod) {
                    const sp = this.$super[name];
                    this.$super[name] = bases[i];
                    if (baseMethod.$origin) {
                        this.$super[name] = baseMethod.$origin.$owner;
                        baseMethod = baseMethod.$origin;
                    }
                    const result = baseMethod.apply(this, arguments);
                    this.$super[name] = sp;
                    return result;
                }
            }
        };
        clz.$name = name;
        jmaa.types[name] = clz;
        let ns = name.split('.');
        let owner = window;
        let i = 0;
        while (i < ns.length - 1) {
            let m = owner[ns[i]];
            if (!m) {
                owner = owner[ns[i]] = {};
            } else {
                owner = m;
            }
            i++;
        }
        owner[ns[i]] = clz;
        return clz;
    };
    jmaa.create = function () {
        const name = arguments[0];
        const args = [];
        for (let i = 1; i < arguments.length; i++) {
            args.push(arguments[i]);
        }
        return new jmaa.types[name](...args);
    };
    jmaa.initView = function () {
        const ps = jmaa.web.getParams(window.location.hash.substring(1));
        $('title').text(jmaa.web.getParams(top.window.location.hash.substring(1)).t || ps.model);
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadView',
            args: {
                model: ps.model,
                type: ps.views,
                key: ps.key
            },
            async onsuccess(r) {
                if (r.data.resource) {
                    let res = $(`<root>${r.data.resource}</root>`);
                    let js = [];
                    let module = res.find('script[type=module]').remove();
                    $('head').append(res.html());
                    module.each(function () {
                        js.push($(this).attr('src'));
                    });
                    for (let src of js) {
                        await import(src);
                    }
                }
                r.data.dom = $('body');
                new JView(r.data);
            }
        });
    }
})();
