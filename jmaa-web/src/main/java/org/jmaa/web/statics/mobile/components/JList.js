/**
 * 列表控件
 */
jmaa.component('JList', {
    cols: 2,
    getTpl() {
        return `<div class="ui-list-view">
                    <div class="pull-down">
                        <div class="pull-down-content">${'刷新'.t()}</div>
                    </div>
                    <ul data-role="listview" class="ui-listview"></ul>
                    <div class="ui-loading-item"></div>
                </div>`;
    },
    /**
     * ajax绑定数据
     * @param card
     * @param callback 数据绑定回调函数
     */
    ajax: jmaa.emptyFn,
    active: false,
    timeout: 5000,
    /**
     * 初始化控件
     */
    init() {
        let me = this;
        let dom = me.dom;
        if (!me.arch) {
            throw new Error('未定义arch');
        }
        dom.html(me.getTpl()).addClass('jui-list');
        me.fieldNames = [];
        me.selected = [];
        me._presentFields = [];
        let arch = jmaa.utils.parseXML(me.arch);
        let list = arch.children('list');
        if (list.length > 0) {
            me.active = eval(list.attr('active') || me.active);
            me.limit = eval(list.attr('limit') || 10);
            let loadMethod = list.attr('load-method');
            if (loadMethod) {
                const fn = view.getFunction(loadMethod);
                me.ajax = function (list, cb) {
                    fn.call(view, list, cb);
                };
            }
            $.each(list[0].attributes, function (i, attr) {
                if (attr.name === 'class') {
                    me.dom.addClass(attr.value);
                } else {
                    const v = jmaa.utils.encode(attr.value);
                    me.dom.attr(attr.name, v);
                }
            });
            me.onEvent('init', list.attr('on-init'));
            me.onEvent('load', list.attr('on-load'));
            me.onEvent('reload', list.attr('on-reload'));
            me.onEvent('selected', list.attr('on-select'));
            me.onEvent('itemClick', list.attr('on-item-click'));
            me.initTemplate(list);
        }
        dom.on('click', '.ui-list-item', function () {
            let item = $(this);
            if (me.active) {
                me.dom.find('.ui-listview-active').removeClass('ui-listview-active');
                item.addClass('ui-listview-active');
            }
            me.selected = [item.attr('data-id')];
            dom.triggerHandler('itemClick', [me, item.attr('data-id')]);
            dom.triggerHandler('selected', [me, me.selected]);
        }).on("click", "[t-click]", function (e) {
            e.preventDefault();
            e.stopPropagation();
            let ele = $(this), item = ele.parents(".list-item"), click = ele.attr("t-click");
            let fn = new Function("return this." + click).call(view, e, item.attr("data-id"), me);
            if (fn instanceof Function) {
                fn.call(view, e, item.attr("data"), me);
            }
        });
        if (eval(list.attr('reload') || 1)) {
            me.onPullDown();
        }
        if (eval(list.attr('pull-up') || 1)) {
            me.onPullUp();
        }
        me.dom.triggerHandler('init', [me]);
        if (eval(list.attr('load-on-init') || 0)) {
            me.load();
        }
    },
    initTemplate(list) {
        let me = this;
        let card = list.children('card');
        if (!card.length) {
            card = list;
        }
        let items = [];
        card.find('field').each(function () {
            let el = $(this);
            let name = el.attr('name');
            let field = name == '$rownum' ? {} : me.fields[name];
            if (!field) {
                throw new Error('模型' + me.model + '找不到字段' + name);
            }
            if (!field.deny) {
                if (name != '$rownum') {
                    me.fieldNames.push(name);
                    if (field.type === 'many2many' || field.type === 'one2many' || field.type === 'many2one') {
                        me._presentFields.push(name);
                    }
                }
                items.push(el);
            }
        });
        let tpl = card.children('template');
        if (tpl.length) {
            me.tpl = juicer(tpl.html().replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">"));
        } else {
            me.dataRoot = true;
            me.cols = Math.min(3, me.nvl(eval(card.attr('cols')), me.cols));
            for (let item of items) {
                let colspan = Math.min(item.attr('colspan') || 1, me.cols);
                let rowspan = item.attr('rowspan') || 1;
                let css = ['card-item', `grid-colspan-${colspan}`, `grid-rowspan-${rowspan}`];
                let name = item.attr('name');
                let field = name == '$rownum' ? {name: '$rownum', label: '行号', type: 'integer'} : me.fields[name];
                let render = item.attr('render');
                if (!render) {
                    render = jmaa.renders[field.type] ? field.type : 'default';
                }
                let style = item.attr('style') || '';
                if (style) {
                    style = ` style="${style}"`;
                }
                let code = '$${data|$render,"' + render + '","' + name + '"}';
                let nolabel = eval(item.attr('nolabel') || 0);
                let label = nolabel ? "" : `<label>${(item.attr('label') || field.label || field.name).t()}：</label>`
                item.replaceWith(`<div class="${css.join(' ')}"${style}>${label}${code}</div>`);
            }
            card.find('[colspan],[rowspan]').each(function () {
                let el = $(this);
                let colspan = el.attr('colspan') || 1;
                let rowspan = el.attr('rowspan') || 1;
                el.addClass(`grid-colspan-${colspan} grid-rowspan-${rowspan}`);
            });
            me.tpl = juicer(`<div class="d-grid grid-template-columns-${me.cols}">${card.html()}</div>`);
        }
    },
    /**
     * 加载数据
     */
    load() {
        let me = this, dom = me.dom, list = dom.find('[data-role=listview]');
        if (me.loading) {
            return;
        }
        me.loading = true;
        let timeout = setTimeout(function () {
            me.loading = false;
        }, me.timeout);
        me.offset = 0;
        dom.find(".ui-loading-item").empty();
        list.prepend(`<li class="ui-loading-item">${'数据加载中'.t()}</li>`);
        me.ajax(me, function (e) {
            if (e.data.length > 0) {
                let html = [];
                let rownum = 1;
                $.each(e.data, function (i, d) {
                    for (let key in this) {
                        let field = me.fields[key];
                        if (field && field.type === 'selection') {
                            let value = this[key];
                            let values = value ? value.split(',') : [];
                            if (values.length > 1) {
                                let opt = [];
                                for (let v of values) {
                                    opt.push(field.options[v]);
                                }
                                this[key] = [value, opt.join(',')];
                            } else {
                                this[key] = [value, field.options[value]];
                            }
                        }
                    }
                    d.$rownum = rownum++;
                    let content = me.tpl.render(me.dataRoot ? {data: this} : this);
                    html.push(`<li class="ui-list-item${(i == 0) ? ' ui-first-child' : ''}" data-id="${this['id']}"><div class="list-item">${content}</div></li>`);
                });
                me.offset = e.data.length;
                list.html(html.join(''));
                me.data = e.data;
                me.active && dom.find(`[data-id=${me.selected[0]}]`).addClass('ui-listview-active');
            } else {
                list.empty();
                dom.find(".ui-loading-item").html('没有数据'.t());
                me.selected = [];
                me.data = [];
            }
            dom.triggerHandler('load', [me, e.data]);
            me.loading = false;
            clearTimeout(timeout);
        });
    },
    /**
     * 获取选中id
     */
    getSelected() {
        return this.selected;
    },
    /**
     * 获取选中数据
     */
    getSelectedData() {
        const me = this;
        const data = [];
        for (const s of me.selected) {
            for (const d of me.data) {
                if (d.id == s) {
                    data.push($.extend({}, d));
                }
            }
        }
        return data;
    },
    /**
     * 设置选中ids
     */
    setSelected(ids) {
        let me = this;
        me.selected = [];
        me.selected.push(...ids);
        if (me.active) {
            me.dom.find('.ui-listview-active').removeClass('ui-listview-active');
            for (let id of me.selected) {
                me.dom.find(`[data-id=${id}]`).addClass('ui-listview-active');
            }
        }
        me.dom.triggerHandler('selected', [me, me.selected]);
    },
    /**
     * 获取控件字段
     */
    getFields() {
        return this.fieldNames;
    },
    /**
     * 获取使用present的字段
     */
    getUsePresent() {
        return this._presentFields;
    },
    /**
     * 向上拉加载更多数据
     */
    onPullUp() {
        let me = this, list = me.dom.find('[data-role=listview]');
        me.dom.scroll(function () {
            if (!me.loading && me.dom[0].scrollTop + me.dom[0].clientHeight > me.dom[0].scrollHeight - 5) {
                me.loading = true;
                let timeout = setTimeout(() => me.loading = false, me.timeout);
                list.find('.ui-loading-item').remove();
                list.append(`<li class="ui-loading-item">${'数据加载中'.t()}</li>`);
                me.ajax(me, function (e) {
                    list.find('.ui-loading-item').remove();
                    if (e.data.length > 0) {
                        let html = '';
                        let rownum = me.offset + 1;
                        $.each(e.data, function (i, d) {
                            for (let k in this) {
                                let field = me.fields[k];
                                if (field && field.type === 'selection') {
                                    let v = this[k];
                                    this[k] = [v, field.options[v]];
                                }
                            }
                            d.$rownum = rownum++;
                            let content = me.tpl.render(me.dataRoot ? {data: this} : this);
                            html += `<li class="ui-list-item${(i + 1 == e.data.length) ? ' ui-last-child' : ''}" data-id="${this['id']}"><div class="list-item">${content}</div></li>`;
                        });
                        list.append(html);
                        me.offset += e.data.length;
                        for (let d of e.data) {
                            me.data.push(d);
                        }
                    } else {
                        list.append(`<li class="ui-loading-item">${'没有更多数据'.t()}</li>`);
                    }
                    me.dom.triggerHandler('load', [me, e.data]);
                    me.loading = false;
                    clearTimeout(timeout);
                });
            }
        });
    },
    /**
     * 向下拉刷新数据
     */
    onPullDown() {
        let me = this;
        //分别设置滑动距离，开始位置，结束位置，和模拟数据的定时器
        let disY, startY, endY, timer;
        let pull = me.dom.find('.pull-down');
        me.dom.find('.ui-list-view').on('touchstart', function (e) {
            if (me.dom[0].scrollTop > 5) {
                startY = -1;
            } else {
                startY = e.originalEvent.changedTouches[0].pageY;
            }
        }).on('touchmove', function (e) {
            if (startY < 0 || me.loading) {
                return;
            }
            endY = e.originalEvent.changedTouches[0].pageY;
            disY = endY - startY;
            if (disY > 50) {
                disY = 50;
            }
            pull.css({
                height: disY + 'px'
            });
        }).on('touchend', function (e) {
            if (startY < 0 || me.loading) {
                return;
            }
            clearInterval(timer);
            endY = e.originalEvent.changedTouches[0].pageY;
            disY = endY - startY;
            if (disY < 30) {
                timer = setInterval(function () {
                    disY -= 5;
                    if (disY < 5) {
                        clearInterval(timer);
                        pull.css({
                            height: 0
                        });
                    } else {
                        pull.css({
                            height: disY + 'px'
                        });
                    }
                }, 100)
            } else {
                me.dom.find('.pull-down').css({
                    height: 0
                });
                me.dom.triggerHandler('reload', [me]);
                me.load();
            }
        });
    }
});
/**
 * 获取指定索引的值
 */
juicer.register('$index', function (data, idx) {
    if (data === null || data === undefined) {
        return '';
    }
    if (data) {
        return data[idx];
    }
    return data;
});
/**
 * *2many字段显示成tags
 */
juicer.register('$tags', function (data) {
    if (data) {
        let tags = []
        for (let d of data) {
            tags.push(d[1]);
        }
        return tags.join(',');
    }
    return data;
});
juicer.register('$render', function (data, render, field) {
    let fn = jmaa.renders[render];
    if (!fn) {
        throw new Error("找不到render:" + render);
    }
    return fn(data[field], data, field);
});
