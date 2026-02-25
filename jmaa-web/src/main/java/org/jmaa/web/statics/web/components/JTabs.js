/**
 * 标签页控件
 */
jmaa.component('JTabs', {
    scrollOffset: 40,
    contextMenu: false,
    getTpl() {
        return `<div class="tab-panel">
                    <div class="tab-header">
                        <span role="button" style="display:none" class="nav-link bg-light border-bottom" data-widget="scroll-left">
                            <span class="fas fa-angle-double-left"></span>
                        </span>
                        <div class="overflow-hidden tab-bar">
                            <ul class="nav nav-tabs" style="margin:0" role="tablist"></ul>
                        </div>
                        <span role="button" style="display:none" class="nav-link bg-light border-bottom" data-widget="scroll-right">
                            <span class="fas fa-angle-double-right"></span>
                        </span>
                    </div>
                    <div class="tab-content"></div>
                </div>`;
    },
    init() {
        const me = this;
        let mousedown = false;
        let mousedownInterval = null;
        let tabs = me.dom.children('tab').remove();
        me.dom.html(me.getTpl()).on('click', '.nav-item-close', function () {
            me.removeTab($(this).parent().attr('data'));
        }).on('click', '[role=tab]', function () {
            const tab = $(this);
            me.dom.triggerHandler('tabActive', [me, tab]);
        }).on('mousedown', '[data-widget=scroll-left]', function (e) {
            e.preventDefault();
            clearInterval(mousedownInterval);
            const scrollOffset = -me.scrollOffset;
            mousedown = true;
            me._navScroll(scrollOffset);
            mousedownInterval = setInterval(function () {
                me._navScroll(scrollOffset);
            }, 250);
        }).on('mousedown', '[data-widget=scroll-right]', function (e) {
            e.preventDefault();
            clearInterval(mousedownInterval);
            const scrollOffset = me.scrollOffset;
            mousedown = true;
            me._navScroll(scrollOffset);
            mousedownInterval = setInterval(function () {
                me._navScroll(scrollOffset);
            }, 250);
        }).on('mouseup', function () {
            if (mousedown) {
                mousedown = false;
                clearInterval(mousedownInterval);
                mousedownInterval = null;
            }
        }).on('tabActive', me.tabActive).on('tabCreate', me.tabCreate).on('tabRemove', me.tabRemove);
        tabs.each(function (i) {
            let tab = $(this);
            let id = tab.attr('id') || 'tab-' + jmaa.nextId();
            me.createTab(id, {
                title: tab.attr('label').t(),
                closable: eval(tab.attr('closable') || 0),
                init: function (t) {
                    t.html(tab.html());
                },
            });
            if (i == 0) {
                me.dom.find('#tab-' + id).addClass('active');
                me.dom.find(`#tabcontent-${id}`).addClass('show active');
            }
        });
        $(window).on('resize', function () {
            me.updateScroll();
        });
        setTimeout(() => {
            me.updateScroll();
        }, 1);
        me.dom.triggerHandler('init', [me]);
    },
    _navScroll(offset) {
        const me = this;
        const leftPos = me.dom.find('.tab-bar').scrollLeft();
        me.dom.find('.tab-bar').animate({scrollLeft: leftPos + offset}, 250, 'linear');
    },
    updateScroll() {
        const me = this;
        const tabs = me.dom.find('.tab-bar');
        let w = 0;
        tabs.find('li').each(function () {
            w += $(this).width();
        });
        if (w > tabs.width()) {
            me.dom.find('[data-widget=scroll-left],[data-widget=scroll-right]').show();
        } else {
            me.dom.find('[data-widget=scroll-left],[data-widget=scroll-right]').hide();
        }
    },
    createTab(id, opt) {
        let me = this;
        let nav = [`<li data="${id}" class="nav-item">`];
        if (opt.closable) {
            nav.push('<a role="button" class="nav-item-close"><i class="fas fa-times"></i></a>');
        }
        nav.push(`<a class="nav-link tab-head" id="tab-${id}" data-toggle="pill" href="#tabcontent-${id}" role="tab" aria-controls="tabcontent-${id}" aria-selected="true">${opt.title}</a></li>`);
        me.dom.find('[role=tablist]').append(nav.join(''));
        me.dom.find('.tab-content').append(`<div class="tab-pane fade" id="tabcontent-${id}" role="tabpanel" aria-labelledby="tab-${id}"></div>`);
        if (opt.init) {
            opt.init(me.dom.find(`#tabcontent-${id}`));
        }
    },
    openTab(id, opt) {
        let me = this;
        let tab = me.dom.find(`#tab-${id}`);
        if (tab.length > 0) {
            if (!tab.hasClass('active')) {
                me.dom.find('[role=tab]').removeClass('active');
                me.dom.find('[role=tabpanel]').removeClass('show active');
                tab.addClass('active');
                let tabPanel = me.dom.find(`#tabcontent-${id}`);
                tabPanel.addClass('show active');
                me.dom.triggerHandler('tabActive', [me, tab]);
            }
        } else {
            me.dom.find('[role=tab]').removeClass('active');
            me.dom.find('[role=tabpanel]').removeClass('show active');
            me.createTab(id, opt);
            tab = me.dom.find('#tab-' + id);
            tab.addClass('active');
            let tabPanel = me.dom.find(`#tabcontent-${id}`);
            tabPanel.addClass('show active');
            me.dom.triggerHandler('tabCreate', [me, tab]);
            me.dom.triggerHandler('tabActive', [me, tab]);
            if (me.contextMenu) {
                me.initTabMenu(id);
            }
            me.updateScroll();
        }
    },
    updateTab(id, title) {
        this.dom.find('#tab-' + id).html(title);
        this.updateScroll();
    },
    removeTab(id) {
        const me = this;
        me.dom.find('[data=' + id + ']').remove();
        me.dom.find('#tabcontent-' + id).remove();
        me.dom.triggerHandler('tabRemove', [me, id]);
        let tab = me.dom.find('[role=tab].active');
        if (tab.length == 0) {
            tab = me.dom.find('[role=tab]:first').addClass('active');
            me.dom.find('#' + tab.attr('aria-controls')).addClass('show active');
            me.dom.triggerHandler('tabActive', [me, tab]);
        }
        me.updateScroll();
    },
    removeOtherTab(id) {
        const me = this;
        me.dom.find('li:not([data=' + id + '])').remove();
        me.dom.find('.tab-pane:not(#tabcontent-' + id + ')').remove();
        me.dom.triggerHandler('tabRemove', [me]);
        const tab = me.dom.find('#tab-' + id);
        if (!tab.hasClass('active')) {
            tab.addClass('active');
            me.dom.find('#' + tab.attr('aria-controls')).addClass('show active');
            me.dom.triggerHandler('tabActive', [me, tab]);
        }
        me.updateScroll();
    },
    onTabActived(handler) {
        this.dom.on('tabActive', handler);
    },
    onTabCreated(handler) {
        this.dom.on('tabCreate', handler);
    },
    onTabRemoved(handler) {
        this.dom.on('tabRemove', handler);
    },
    enableTab(id, enabled) {
        // TODO
    },
    initTabMenu(id) {
        const me = this;
        me.dom.find('[data=' + id + ']').contextMenu({
            width: 110, // width
            itemHeight: 30, // 菜单项height
            autoHide: true,
            bgColor: '#fff', // 背景颜色
            color: '#0000', // 字体颜色
            fontSize: 12, // 字体大小
            hoverColor: '#ffff', // hover字体颜色
            hoverBgColor: '#32c5d2', // hover背景颜色
            menu: [
                {
                    // 菜单项
                    text: '关闭当前'.t(),
                    callback() {
                        me.removeTab(id);
                    },
                },
                {
                    text: '关闭其他'.t(),
                    callback() {
                        me.removeOtherTab(id);
                    },
                },
            ],
        });
    },
});
$.fn.JTabs = function (opt) {
    let com = $(this).data(name);
    if (!com) {
        com = new JTabs($.extend({dom: this}, opt));
        $(this).data(name, com);
    }
    return com;
};
