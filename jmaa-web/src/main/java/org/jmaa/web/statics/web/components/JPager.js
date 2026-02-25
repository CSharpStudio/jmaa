/**
 * 分页控件
 */
jmaa.component("JPager", {
    /**
     * 默认分页大小
     */
    limit: 50,
    /**
     * 获取模板
     * @returns {string}
     */
    getTpl() {
        return `<div class="btn-group page-size">
                    <div class="pager-from input-group input-group-sm"><span>1</span></div>
                    <span class="size-to">-</span>
                    <div class="pager-to input-group input-group-sm"></div>
                    <span class="size-separator">/</span>
                    <div class="pager-total"><span>?</span></div>
                </div>
                <div class="btn-group ml-2">
                    <button type="button" auth="read" class="btn btn-sm btn-default pager-prev">
                        <i class="fa fa-angle-left"></i>
                    </button>
                    <button type="button" auth="read" class="btn btn-sm btn-default pager-next">
                        <i class="fa fa-angle-right"></i>
                    </button>
                </div>`;
    },
    /**
     * 初始化控件
     */
    init() {
        let me = this, dom = me.dom;
        me.from = 1;
        me.to = me.limit;
        dom.html(me.getTpl()).addClass('jui-pager')
            .on('click', 'div.pager-from', function (e) {
                let el = $(this);
                if (!el.hasClass('edit')) {
                    el.html('<input type="text" class="form-control" style="width:3rem;">');
                    let input = el.find('input');
                    input.val(me.from);
                    input.focus();
                    input.select();
                    let submit = function () {
                        el.removeClass('edit');
                        let val = parseInt(input.val());
                        if (!isNaN(val) && val > 0) {
                            if (me.to - val > 2000) {
                                jmaa.msg.error("一页不能超过2000条数据");
                                return;
                            }
                            me.from = val;
                            el.html('<span>' + val + '</span>');
                            if (me.from > me.to) {
                                me.to = me.from;
                                el.find('div.pager-to').html('<span>' + me.to + '</span>');
                            }
                            me.setLimit(me.to - me.from + 1);
                            dom.triggerHandler('pageChange', [me]);
                        } else {
                            el.html('<span>' + me.from + '</span>');
                        }
                    }
                    input.on('blur', function () {
                        submit();
                    }).on('keypress', function (event) {
                        if (event.keyCode == "13") {
                            submit();
                        }
                    });
                    el.addClass('edit');
                }
            }).on('click', 'div.pager-to', function (e) {
            let el = $(this);
            if (!el.hasClass('edit')) {
                el.html('<input type="text" class="form-control" style="width:3rem;">');
                let input = el.find('input');
                input.val(me.to);
                input.focus();
                input.select();
                let submit = function () {
                    el.removeClass('edit');
                    let val = parseInt(input.val());
                    if (!isNaN(val) && val > 0) {
                        if (val - me.from > 2000) {
                            jmaa.msg.error("一页不能超过2000条数据");
                            return;
                        }
                        me.to = val;
                        el.html('<span>' + val + '</span>');
                        if (me.to < me.from) {
                            me.from = me.to;
                            el.find('div.pager-from').html('<span>' + me.from + '</span>');
                        }
                        me.setLimit(me.to - me.from + 1);
                        dom.triggerHandler('pageChange', [me]);
                    } else {
                        el.html('<span>' + me.to + '</span>');
                    }
                }
                input.on('blur', function () {
                    submit();
                }).on('keypress', function (event) {
                    if (event.keyCode == "13") {
                        submit();
                    }
                });
                el.addClass('edit');
            }
        }).on('click', 'div.pager-total', function (e) {
            if (!me._counting) {
                me._counting = true;
                dom.triggerHandler('counting', [me]);
                setTimeout(() => me._counting = false, 500);
            }
        }).on('click', 'button.pager-prev', function (e) {
            if (me.from > me.limit) {
                me.from -= me.limit;
            } else {
                me.from = 1;
            }
            me.to = me.from + me.limit - 1;
            dom.triggerHandler('pageChange', [me]);
        }).on('click', 'button.pager-next', function (e) {
            me.from += me.limit;
            me.to = me.from + me.limit - 1;
            dom.triggerHandler('pageChange', [me]);
        });
        me.onPageChange(me.pageChange);
        me.onCounting(me.counting);
        me.onLimitChange(me.limitChange);
        if (me.buttonOnly) {
            me.dom.find(".page-size").remove();
        }
        if (me.noCounting) {
            me.dom.addClass('no-counting');
        }
        me.dom.triggerHandler('init', [me]);
    },
    /**
     * 注册分页大小变更事件
     * @param handler 处理函数
     */
    onLimitChange(handler) {
        this.dom.on("limitChange", handler);
    },
    /**
     * 注册分页变更事件
     * @param handler 处理函数
     */
    onPageChange(handler) {
        this.dom.on("pageChange", handler);
    },
    /**
     * 注册统计总数事件
     * @param handler 处理函数
     */
    onCounting(handler) {
        this.dom.on("counting", handler);
    },
    /**
     * 设置分页大小
     * @param limit
     */
    setLimit(limit) {
        let me = this;
        me.limit = limit;
        me.dom.triggerHandler('limitChange', [me]);
    },
    /**
     * 获取分页大小
     * @returns {*}
     */
    getLimit() {
        return this.limit;
    },
    /**
     * 获取数据偏移量
     * @returns {number}
     */
    getOffset() {
        return this.from - 1;
    },
    /**
     * 更新分页按钮状态
     * @param opts
     */
    update(opts) {
        let me = this;
        let dom = me.dom;
        dom.show();
        dom.find(".page-size").show();
        let next, total;
        if (opts.values) {
            let len = me.getOffset() + opts.values.length;
            if (opts.hasNext === false) {
                me.to = len;
                next = false;
                total = len;
            } else {
                me.to = len;
                next = true;
            }
        } else {
            if (opts.from) {
                me.from = opts.from;
            }
            if (opts.to) {
                me.to = opts.to;
            }
            next = opts.next;
            total = opts.total;
        }
        dom.find('div.pager-from').html('<span>' + me.from + '</span>');
        dom.find('div.pager-to').html('<span>' + me.to + '</span>');
        if (next != undefined && next != null) {
            dom.find('button.pager-next').attr('disabled', !next);
        }
        dom.find('button.pager-prev').attr('disabled', me.from === 1);
        if (total > 0) {
            dom.find('div.pager-total').html('<span>' + total + '</span>');
        }
    },
    /**
     * 重置
     */
    reset() {
        let me = this, dom = me.dom;
        me.from = 1;
        me.to = me.limit;
        dom.find('div.pager-from').html('<span>' + me.from + '</span>');
        dom.find('div.pager-to').html('<span>' + me.to + '</span>');
        dom.find('div.pager-total').html('<span>?</span>');
        dom.find('button.pager-next').removeClass('disabled');
    },
    /**
     * 隐藏，当没有多页时，不显示
     */
    hide() {
        this.dom.hide();
    },
    noData() {
        this.dom.find(".page-size").hide();
        this.dom.find('button').attr('disabled', true);
    }
});
