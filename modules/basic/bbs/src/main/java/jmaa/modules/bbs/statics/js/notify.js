$(function () {
    jmaa.component("JNotify", {
        init() {
            let me = this;
            me.dom.find('.badge').html('');
            me.dom.on('click', () => me.load());
            me.dom.parent().on('click', '.feed-item', function () {
                me.openItem($(this));
            }).on('click', '.btn-send', function () {
                me.openChannel();
            }).on('hide.bs.dropdown', function (e) {
                if (e.clickEvent) {
                    let target = $(e.clickEvent.originalEvent.target);
                    if (target.attr('role') == 'tab') {
                        me.dom.parent().find('[role=tab]').removeClass('active');
                        target.addClass('active');
                        me.filterItems(target.attr("data-tab"));
                        e.preventDefault();
                    }
                }
            });
            me.poll();
            setInterval(() => me.poll(), 60000);
            setInterval(function () {
                me.dom.parent().find('[format-time]').each(function () {
                    let time = $(this);
                    let dt = time.attr('format-time');
                    time.html(formatDate(dt));
                })
            }, 60000);
            me.media = new Audio('/web/jmaa/modules/bbs/statics/new.mp3');
        },
        openItem(item) {
            let me = this;
            let id = item.attr('data-id');
            if (id) {
                jmaa.rpc({
                    model: 'bbs.message_feed',
                    method: 'markRead',
                    args: {
                        ids: [id]
                    },
                    onsuccess(r) {
                        me.poll();
                    }
                });
            }
            window.app.ws.openTab(item.attr("data-menu"), jmaa.web.getTenantPath() + item.attr('data-link'));
        },
        openChannel() {
            window.app.ws.openTab('消息'.t(), jmaa.web.getTenantPath() + `/view#model=bbs.message&views=custom&view=custom`);
        },
        load() {
            let me = this;
            me.poll();
            jmaa.rpc({
                model: 'bbs.message_feed',
                method: 'loadFeed',
                args: {},
                onsuccess(r) {
                    me.data = r.data;
                    me.filterItems('all');
                }
            });
        },
        createLink(data) {
            let views = data.views;
            let back = '';
            if (!views) {
                views = "form";
            } else {
                let pair = views.split('|');
                if (pair.length > 1) {
                    views = pair[0] + "&key=" + pair[1];
                }
                back = views.replace(/form/g, '').split(',')[0];
            }
            return `/view#model=${data.model}&views=${views}&back=${back}&view=${data.view}${data.res_id ? `&id=${data.res_id}` : ''}`;
        },
        filterItems(type) {
            let me = this;
            let dropdown = me.dom.parent().find('.feed-message');
            dropdown.html(`<div class="dropdown-item dropdown-header">${'加载中'.t()}</div>`);
            let html = [];
            for (let row of me.data) {
                if (type == 'all' || type == row.type) {
                    let link = me.createLink(row);
                    let avatar;
                    if (row.type == 'chat') {
                        avatar = row.avatar ? `<div class="avatar" style="background:url(${jmaa.web.getTenantPath()}/attachment/${row.avatar}) no-repeat center/cover"></div>`
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
                                        ${row.unread ? `<span class="feed-unread"></span>` : ''}
                                        <div class="flex-fill"></div>
                                        <div class="feed-dt" format-time="${row.last_dt}">${formatDate(row.last_dt)}</div>
                                    </div>
                                    <div class="m-content">
                                        <span class="text-truncate">${row.description}</span>
                                        <div class="flex-fill"></div>
                                    </div>
                                </div>
                            </div>
                            <div class="dropdown-divider"></div>`);
                }
            }
            if (html.length) {
                dropdown.html(html.join(''));
            } else {
                dropdown.html(`<div class="dropdown-header">${'没有消息'.t()}</div>`);
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
                        me.dom.find('.badge').html(count > 0 ? count : '');
                    }
                }
            });
        }
    });
    window.notify = new JNotify({dom: $('[data-widget=notify]')});
});
