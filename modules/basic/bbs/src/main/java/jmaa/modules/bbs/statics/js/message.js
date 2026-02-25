//@ sourceURL=message.js
jmaa.view({
    emojis: ['😊', '😃', '😆', '😂', '😉', '😎', '😜', '😋', '😝', '😳', '😐', '😕', '😞', '😱', '😲', '😨', '😠', '😈', '😘', '😇', '😢', '😭', '❤️', '💔', '😍', '👳', '👍', '👎', '👌', '💩', '🙈', '🙉', '🙊', '🐞', '😺', '🐻', '🐌', '🐗', '🍀', '🌹', '🔥', '☀️', '⛅', '🌈', '☁️', '⚡️', '⭐', '🍪', '🍕', '🍔', '🍟', '🎂', '🍰', '☕', '🍌', '🍣', '🍙', '🍺', '🍷', '🍸', '🍹', '🍻', '👻', '💀', '👽', '🎉', '🏆', '🔑', '📌', '📯', '🎵', '🎺', '🎸', '🏃', '🚲', '⚽', '🏈', '🎱', '🎬', '🎤', '🧀'],
    init() {
        let me = this;
        me.initEmojis();
        new JSelect({dom: me.dom.find('.message-filter')});
        me.initPager();
        me.dom.on('click', '.bbs-link-item', function () {
            me.openMessage();
        }).on('click', '.bbs-channel-item,.feed-item', function () {
            me.openChannel($(this).attr('data-id'));
        }).on('click', '.btn-toggle', function () {
            $(this).parents('.aside-group').toggleClass("collapsed");
        }).on('click', '.btn-link', function () {
            let item = $(this);
            top.app.ws.openTab(item.attr("data-menu"), jmaa.web.getTenantPath() + item.attr('data-link'));
        }).on('click', '.mark-read', function () {
            me.markRead([$(this).attr('data-id')]);
        }).on('click', '.btn-mark-read', function () {
            let ids = [];
            if (me.data) {
                for (let row of me.data) {
                    ids.push(row.id);
                }
            }
            me.markRead(ids);
        }).on('change', '.message-filter', function () {
            me.loadMessage();
        }).on('click', '.btn-join', function () {
            let type = $(this).attr('data-type');
            let item = me.dom.find(`.join-channel[type=${type}]`).removeClass('d-none');
            item.find('input').val('');
            item.find('.dropdown-menu').removeClass('show');
        }).on('keyup', '.join-channel input', function () {
            let input = $(this);
            clearTimeout(me.keyupTimer);
            me.keyupTimer = setTimeout(function () {
                me.joinChannel(input);
            }, 500);
        }).on('click', '.chat-items .btn-leave', function () {
            let item = $(this).closest('.bbs-channel-item');
            me.leaveChannel([item.attr('data-id')], function () {
                jmaa.msg.show('你取消了与[{0}]的对话'.t().formatArgs(item.find('.bbs-channel-item-name').html()));
                item.remove();
            });
        }).on('click', '.channel-items .btn-leave', function () {
            let item = $(this).closest('.bbs-channel-item');
            me.leaveChannel([item.attr('data-id')], function () {
                jmaa.msg.show('你退出了频道：'.t() + item.find('.bbs-channel-item-name').html());
                item.remove();
            });
        }).on('click', '.tab-item', function () {
            let type = $(this).attr('tab');
            if (type == 'chat') {
                me.openChatTab();
            } else if (type == 'channel') {
                me.openChannelTab();
            } else {
                me.openMessageTab();
            }
        }).on('input', '.thread-textarea', function () {
            let text = $(this);
            let count = text.val().split('\n').length;
            text.css('height', (42 + Math.min(count - 1, 5) * 22) + 'px');
        }).on('keydown', '.thread-textarea', function (e) {
            if (!me.isSmall && e.code == 'Enter' && !e.shiftKey) {
                me.sendMessage();
                e.preventDefault();
            }
        }).on('click', '.btn-thread-send', function () {
            me.sendMessage();
        }).on('click', '.btn-thread-back', function () {
            me.backTab();
        });
        me.updateSize();
        $(window).on('resize', function () {
            me.updateSize();
        });
        if (me.urlHash.id) {
            me.openChannel(me.urlHash.id);
        } else {
            me.openMessage();
        }
        setInterval(function () {
            me.dom.find('[format-time]').each(function () {
                let time = $(this);
                let dt = time.attr('format-time');
                time.html(formatDate(dt));
            })
        }, 60000);
        setInterval(function () {
            me.loadFeed();
            if (me.urlHash.id) {
                me.pollMessage(me.urlHash.id);
            }
        }, 60000);
        me.loadFeed();
    },
    backTab() {
        let me = this;
        let type = me.dom.find('.thread-header .title').attr('data-type');
        if (type == 'chat') {
            me.openChatTab();
        } else {
            me.openChannelTab();
        }
    },
    openChatTab() {
        let me = this;
        me.showMessageContent();
        me.dom.find('.tab-item').removeClass('active');
        me.dom.find('.tab-content').removeClass('active');
        me.dom.find('.tab-item[tab=chat]').addClass('active');
        me.dom.find('.chat-tab').addClass('active');
        me.loadFeed();
    },
    openChannelTab() {
        let me = this;
        me.showMessageContent();
        me.dom.find('.tab-item').removeClass('active');
        me.dom.find('.tab-content').removeClass('active');
        me.dom.find('.tab-item[tab=channel]').addClass('active');
        me.dom.find('.channel-tab').addClass('active');
        me.loadFeed();
    },
    openMessageTab() {
        let me = this;
        me.openMessage();
        me.dom.find('.tab-item').removeClass('active');
        me.dom.find('.tab-content').removeClass('active');
        me.dom.find('.tab-item[tab=message]').addClass('active');
        me.dom.find('.message-tab').addClass('active');
    },
    loadFeed() {
        let me = this;
        jmaa.rpc({
            model: 'bbs.channel',
            module: me.module,
            method: 'fetchFeed',
            onsuccess(r) {
                me.renderFeed(r.data);
                me.renderChannel(r.data);
            }
        })
    },
    renderChannel(data) {
        let me = this;
        let channels = [];
        let chats = [];
        for (let row of data) {
            let item = me.createChannelItem(row);
            if (row.channel_type == 'channel') {
                channels.push(item);
            } else {
                chats.push(item);
            }
        }
        me.dom.find('.channel-items').html(channels.join(''));
        me.dom.find('.chat-items').html(chats.join(''));
    },
    renderFeed(data) {
        let me = this;
        let channels = [];
        let chats = [];
        for (let row of data) {
            let avatar = row.avatar ? `<div class="avatar" style="background:url(${jmaa.web.getTenantPath()}/attachment/${row.avatar}) no-repeat center/cover"></div>`
                : `<div class="avatar">${((row.name || '').trim()[0] || '').toUpperCase()}</div>`;
            let item = `<div class="feed-item message" data-id="${row.id}" data-type="${row.type}">
                        ${avatar}
                        <div class="m-body">
                            <div class="m-header${row.count ? '' : ' text-muted'}">
                                <div class="text-truncate">${row.name}</div>
                                ${row.count ? `<span class="feed-counter">${row.count}</span>` : ''}
                                ${row.unread ? `<span class="feed-unread"></span>` : ''}
                                <div class="flex-fill"></div>
                                <div class="feed-dt">${formatDate(row.last_dt)}</div>
                            </div>
                            <div class="m-content">
                                <span class="text-truncate">${row.description}</span>
                                <div class="flex-fill"></div>
                            </div>
                        </div>
                    </div>`;
            if (row.channel_type == 'channel') {
                channels.push(item);
            } else {
                chats.push(item);
            }
        }
        me.dom.find('.channel-content-items').html(channels.join(''));
        me.dom.find('.chat-content-items').html(chats.join(''));
    },
    showMessageContent() {
        let me = this;
        delete me.urlHash.id;
        window.location.hash = $.param(me.urlHash);
        me.dom.find('.thread-content').hide();
        me.dom.find('.message-content').show();
    },
    sendMessage() {
        let me = this;
        let text = me.dom.find('.thread-textarea');
        let body = text.val();
        text.val('').css('height', '42px');
        if (body) {
            jmaa.rpc({
                model: "bbs.channel",
                module: me.module,
                method: 'postMessage',
                args: {
                    ids: [me.urlHash.id],
                    body,
                    fields: ['body', 'author_id', 'create_date', 'author_image'],
                },
                context: {
                    usePresent: true,
                },
                onsuccess(r) {
                    me.pollMessage(me.urlHash.id);
                },
            });
        }
    },
    updateSize() {
        let me = this;
        if ($(window).width() < 740) {
            me.dom.addClass('size-small');
            me.isSmall = true;
        } else {
            me.dom.removeClass('size-small');
            me.isSmall = false;
            if (!me.urlHash.id) {
                me.openMessageTab();
            }
        }
    },
    initEmojis() {
        let me = this;
        let html = [];
        for (let e of me.emojis) {
            html.push(`<span class="emoji">${e}</span>`);
        }
        me.dom.find('.emojis-dropdown').html(html.join('')).on('click', '.emoji', function () {
            let item = $(this);
            let input = me.dom.find('.thread-textarea');
            input.val(input.val() + item.html()).focus();
        });
        if (env.user.image) {
            me.dom.find('.thread-footer .profile').css('background', `url(${jmaa.web.getTenantPath()}/attachment/${env.user.image}) no-repeat center/cover`);
        } else {
            me.dom.find('.thread-footer .profile').html(env.user.name[0].toUpperCase());
        }
    },
    joinChannel(input) {
        let me = this;
        let keyword = input.val();
        if (!keyword) {
            return;
        }
        let menu = input.parent().find('.dropdown-menu').removeClass('show');
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchUser',
            args: {
                keyword,
            },
            onsuccess(r) {
                if (r.data.length) {
                    let html = [];
                    for (let d of r.data) {
                        html.push(`<div class="lookup-item" data-id="${d.id}">${d.present}</div>`);
                    }
                    menu.html(html.join(''));
                    menu.addClass('show');
                    menu.find('[data-id]').on('click', function () {
                        let item = $(this);
                        jmaa.rpc({
                            model: 'bbs.channel',
                            module: me.module,
                            method: 'getChannel',
                            args: {
                                userId: item.attr('data-id')
                            },
                            onsuccess(r) {
                                input.parent().addClass('d-none');
                                let channelItem = me.createChannelItem(r.data);
                                me.dom.find(`.chat-items [data-id=${r.data.id}]`).remove();
                                me.dom.find('.chat-items').prepend(channelItem);
                                me.openChannel(r.data.id);
                            }
                        });
                    });
                }
            }
        });
    },
    leaveChannel(ids, callback) {
        let me = this;
        jmaa.rpc({
            model: 'bbs.channel',
            module: me.module,
            method: 'leaveChannel',
            args: {
                ids
            },
            onsuccess(r) {
                callback && callback();
            }
        });
    },
    initPager() {
        let me = this;
        me.messagePager = new JPager({
            dom: me.dom.find('.message-pager'),
            limit: 50,
            pageChange: function (e, pager) {
                pager.update(e)
                me.loadMessage();
            },
            counting: function (e, pager) {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'countMessage',
                    args: {
                        criteria: me.getMessageCriteria(),
                    },
                    onsuccess: function (r) {
                        pager.update({
                            total: r.data
                        });
                    }
                });
            }
        });
    },
    markRead(ids) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'markRead',
            args: {
                ids,
            },
            onsuccess(r) {
                me.loadMessage();
                top.window.notify.poll();
            }
        });
    },
    createChannelItem(row) {
        let avatar = row.avatar ? `<div class="avatar" style="background: url(${jmaa.web.getTenantPath()}/attachment/${row.avatar}) no-repeat center/cover"></div>`
            : `<div class="avatar">${((row.name || '').trim()[0] || '').toUpperCase()}</div>`;
        return `<div class="bbs-channel-item" data-id="${row.id}" data-type="${row.channel_type}">
                ${avatar}
                ${row.unread ? `<span class="bbs-channel-unread"></span>` : ''}
                <div class="bbs-channel-item-name">${row.name}</div>
                <div class="bbs-channel-item-commands">
                    <button type="button" class="btn btn-icon btn-leave">
                        <i class="fa fa-times"></i>
                    </button>
                </div>
            </div>`;
    },
    openMessage() {
        let me = this;
        me.showMessageContent();
        me.messagePager.reset();
        me.loadMessage();
    },
    getMessageCriteria() {
        let me = this;
        let criteria = [];
        let value = me.dom.find('.message-filter').val();
        if ('all' != value) {
            criteria.push(['is_read', '=', false]);
        }
        return criteria;
    },
    loadMessage() {
        let me = this;
        me.dom.find('.message-tab-body').html(`<div class="text-center">${'加载中'.t()}</div>`);
        let pager = me.messagePager;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchMessage',
            args: {
                criteria: me.getMessageCriteria(),
                fields: ['subject', 'body', 'create_date', 'author_id', 'author_image'],
                limit: pager.getLimit(),
                offset: pager.getOffset(),
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                me.data = r.data.values;
                if (r.data.values.length > 0) {
                    pager.update(r.data);
                } else {
                    pager.noData();
                }
                let items = [];
                for (let row of r.data.values) {
                    let views = row.views;
                    let feed = '';
                    if (row.feed) {
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
                        let link = `/view#model=${row.model}&views=${views}&back=${back}&view=${row.view}&id=${row.res_id}&top=1`;
                        feed = `<div class="btn-link" data-link="${link}" data-menu="${row.menu}">${row.feed}</div>`;
                    }
                    let avatar = row.author_image && row.author_image.length ?
                        `<div class="avatar" style="background:url(${jmaa.web.getTenantPath()}/attachment/${row.author_image[0].id}) no-repeat center/cover"></div>` :
                        `<div class="avatar">${((row.author_id[1] || '').trim()[0] || '').toUpperCase()}</div>`;
                    items.push(`<div class="message">
                        ${avatar}
                        <div class="m-body">
                            <div class="m-header">
                                ${row.author_id[1]}
                                <span class="text-muted text-xs pl-3">${row.create_date}</span>
                                ${!row.is_read ? `<div class="unread"></div><div class="flex-fill"></div><div class="mark-read" data-id="${row.id}">√${'标记为已读'.t()}</div>` : ''}
                            </div>
                            <div class="m-content">
                                ${feed}
                                ${row.subject ? `<div class="message-subject">${'主题'.t()}：${row.subject}</div>` : ''}
                                <div class="message-body">
                                    ${(row.body || '').replaceAll('\n', '<br/>')}
                                </div>
                            </div>
                        </div>
                    </div>`);
                }
                me.dom.find('.message-tab-body').html(items.join(''));
            }
        });
    },
    openChannel(id) {
        let me = this;
        me.urlHash.id = id;
        window.location.hash = $.param(me.urlHash);
        me.dom.find('.message-list').html('<div class="flex-grow-1"></div>').removeAttr('last-id').removeAttr('last-date');
        jmaa.rpc({
            method: 'readChannel',
            model: 'bbs.channel',
            module: me.module,
            args: {
                ids: [id],
                fields: ['body', 'author_id', 'create_date', 'author_image'],
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                me.dom.find('.thread-header .title').html(r.data.name).attr('data-type', r.data.type);
                me.appendMessage(r.data.messages);
            }
        });
        me.dom.find('.message-content').hide();
        me.dom.find('.thread-content').show();
    },
    pollMessage(id) {
        let me = this;
        let list = me.dom.find('.message-list');
        let lastId = list.attr('last-id');
        jmaa.rpc({
            method: 'readMessage',
            model: 'bbs.channel',
            module: me.module,
            args: {
                ids: [id],
                fields: ['body', 'author_id', 'create_date', 'author_image'],
                lastId,
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                me.appendMessage(r.data);
            },
            error(r) {
                console.log(r);
            }
        })
    },
    appendMessage(data) {
        let me = this;
        if (data.length) {
            let list = me.dom.find('.message-list');
            let date = list.attr('last-date');
            let today = moment().format('yyyy-MM-DD');
            let html = [];
            for (let row of data) {
                let dt = moment(row.create_date).format('yyyy-MM-DD');
                if (date != dt) {
                    html.push(`<div class="separator-date">
                            <hr class="separator-line"/>
                            <span class="separator-label">${today == dt ? '今天'.t() : dt}</span>
                            <hr class="separator-line"/>
                        </div>`);
                    date = dt;
                }
                html.push(me.createMessageItem(row, today == dt));
            }
            list.append(html.join('')).attr('last-date', date).attr('last-id', data[data.length - 1].id);
            list.scrollTop(list.prop("scrollHeight"));
        }
    },
    createMessageItem(row, format) {
        let avatar = row.author_image ? `<div class="avatar" style="background: url(${jmaa.web.getTenantPath()}/attachment/${row.author_image[0].id}) no-repeat center/cover"></div>`
            : `<div class="avatar">${row.author_id ? row.author_id[1][0].toUpperCase() : ''}</div>`;
        return `<div class="message">
            ${avatar}
            <div class="m-body">
                <div class="m-header">
                    ${row.author_id[1]}
                    <span class="text-muted text-xs pl-3"${format ? ` format-time="${row.create_date}"` : ''}>${format ? formatDate(row.create_date) : row.create_date}</span>
                </div>
                <div class="m-content">
                    ${(row.body || '').replaceAll('\n', '<br/>')}
                </div>
            </div>
        </div>`;
    }
});
$(function () {
    $(document).on('mousedown', function (e) {
        if ($(e.target).closest('.join-channel').length == 0) {
            $('.join-channel').addClass('d-none');
        }
    });
});
