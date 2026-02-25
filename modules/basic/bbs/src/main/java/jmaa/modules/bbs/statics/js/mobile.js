jmaa.define('JNotify', {
    getTpl() {
        return `<div class="header-bar">
                <div class="toolbar">
                    <button type="button" class="ui-btn btn-message">${'发消息'.t()}</button>
                </div>
            </div>
            <div class="feed-message">
                <div class="pull-down">
                    <div class="pull-down-content">${'刷新'.t()}</div>
                </div>
                <div class="notifiy-items"></div>
            </div>`;
    },
    __init__() {
        let me = this;
        me.data = [];
        $('.notify-page .ui-content').html(me.getTpl());
        let clicking;
        $('.link-notify').append('<span class="badge badge-warning"></span>').on('click', function () {
            if (!clicking) {
                me.loadFeed();
                me.poll();
                clicking = true;
                setTimeout(function () {
                    clicking = false;
                }, 1000);
            }
        });
        $('.notify-page .feed-message').on('click', '.feed-item', function () {
            me.openItem($(this));
        });
        $('.btn-message').on('click', function () {
            window.app.openPage({
                title: '消息'.t(),
                back: '#notify',
                init(body) {
                    body.html(`<iframe src="${jmaa.web.getTenantPath()}/view#model=bbs.message&views=custom&view=custom&top=1"></iframe>`)
                }
            });
        });
        me.loadFeed();
        me.onPullDown();
        me.poll();
        me.media = new Audio('/web/jmaa/modules/bbs/statics/new.mp3');
        setInterval(() => me.poll(), 60000);
    },
    openItem(item) {
        let me = this;
        let id = item.attr('data-id');
        me.markRead(id);
        window.app.openPage({
            title: item.attr("data-menu"),
            back: '#notify',
            init(body) {
                body.html(`<iframe src="${jmaa.web.getTenantPath() + item.attr('data-link')}"></iframe>`)
            }
        });
    },
    markRead(feedId) {
        let me = this;
        if (feedId) {
            jmaa.rpc({
                model: 'bbs.message_feed',
                method: 'markRead',
                args: {
                    ids: [feedId]
                },
                onsuccess(r) {
                    me.poll();
                }
            });
        }
    },
    loadFeed() {
        let me = this;
        $('.notifiy-items').html(`<div class="ui-loading-item">${'加载中'.t()}</div>`);
        jmaa.rpc({
            model: 'bbs.message_feed',
            method: 'loadFeed',
            args: {},
            onsuccess(r) {
                me.data = r.data;
                me.renderFeed();
            }
        });
    },
    createLink(data) {
        let views = data.views;
        let back = '';
        if (!views) {
            views = "form";
        } else {
            back = views.replace(/form/g, '').split(',')[0];
        }
        return `/view#model=${data.model}&views=${views}&back=${back}&view=${data.view}${data.res_id ? `&id=${data.res_id}` : ''}&top=1`;
    },
    renderFeed() {
        let me = this;
        let content = $('.notifiy-items');
        let html = [];
        for (let row of me.data) {
            let link = me.createLink(row);
            let avatar;
            if (row.type == 'chat') {
                avatar = row.avatar ? `<div class="avatar" style="background: url(${jmaa.web.getTenantPath()}/attachment/${row.avatar}) no-repeat center/cover"></div>`
                    : `<div class="avatar">${((row.name || '').trim()[0] || '').toUpperCase()}</div>`;
            } else {
                avatar = '<div class="avatar"><i class="fas"></i></div>';
            }
            html.push(`<div class="feed-item message"${row.id ? ` data-id="${row.id}"` : ''} data-type="${row.type}" data-link="${link}" data-menu="${row.menu}">
                ${avatar}
                <div class="m-body">
                    <div class="m-header${row.count ? '' : ' text-muted'}">
                        <div class="text-truncate">${row.name}</div>
                        ${row.count ? `<span class="feed-counter">${row.count}</span>` : ''}
                        <div class="flex-fill"></div>
                        <div class="feed-dt" format-time="${row.last_dt}">${formatDate(row.last_dt)}</div>
                    </div>
                    <div class="m-content">
                        <span class="text-truncate">${row.description}</span>
                        <div class="flex-fill"></div>
                    </div>
                </div>
            </div>`);
        }
        if (html.length) {
            content.html(html.join(''));
        } else {
            content.html(`<div class="ui-loading-item">${'没有消息'.t()}</div>`);
        }
    },
    poll() {
        let me = this;
        $.ajax({
            url: jmaa.web.getTenantPath() + '/bbs/polling',
            type: 'GET',
            success(rs) {
                if (rs && rs.length) {
                    if (me.lastId && rs[1] > me.lastId) {
                        me.media.play();
                    }
                    let count = Number(rs[0]);
                    me.lastId = rs[1];
                    $('.link-notify .badge').html(count > 0 ? count : '');
                }
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
        let dom = $('.notify-page .ui-content');
        let pull = dom.find('.pull-down');
        dom.on('touchstart', function (e) {
            if (dom[0].scrollTop > 5) {
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
                pull.css({
                    height: 0
                });
                me.loadFeed();
            }
        });
    },
});
$(function () {
    window.notify = new JNotify();
});
