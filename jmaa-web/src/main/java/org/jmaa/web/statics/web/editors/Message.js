/**
 * 消息
 */
jmaa.editor('message', {
    css: 'e-message',
    /**
     * 默认加载10行
     */
    limit: 10,
    emojis: ['😊', '😃', '😆', '😂', '😉', '😎', '😜', '😋', '😝', '😳', '😐', '😕', '😞', '😱', '😲', '😨', '😠', '😈', '😘', '😇', '😢', '😭', '❤️', '💔', '😍', '👳', '👍', '👎', '👌', '💩', '🙈', '🙉', '🙊', '🐞', '😺', '🐻', '🐌', '🐗', '🍀', '🌹', '🔥', '☀️', '⛅', '🌈', '☁️', '⚡️', '⭐', '🍪', '🍕', '🍔', '🍟', '🎂', '🍰', '☕', '🍌', '🍣', '🍙', '🍺', '🍷', '🍸', '🍹', '🍻', '👻', '💀', '👽', '🎉', '🏆', '🔑', '📌', '📯', '🎵', '🎺', '🎸', '🏃', '🚲', '⚽', '🏈', '🎱', '🎬', '🎤', '🧀'],
    /**
     * 控件模板
     * @returns
     */
    getTpl() {
        return `<div id="${this.getId()}">
                    <div class="header">
                        <div class="toolbar">
                            <button auth="read" class="btn-default btn-send btn-chat" label="发送消息"></button>
                            <button auth="read" class="btn-default btn-note btn-chat" label="记录备注"></button>
                        </div>
                        <div class="rbar">
                            <button type="button" class="btn btn-subscribe"></button>
                            <div class="followers">
                                <div class="follower btn btn-link">
                                    <i class="fa fa-user"></i>
                                    <span class="follower-count"></span>
                                </div>
                                <div class="dropdown-menu follower-dropdown">
                                </div>
                            </div>
                            <div role="pager" class="ml-4"></div>
                        </div>
                    </div>
                    <div class="chatter d-none">
                        <i></i>
                        <div class="to-followers"></div>
                        <div class="avatar"></div>
                        <div class="chat-body">
                            <input type="text" class="chat-input">
                            <div class="toolbar">
                                <div class="input-group">
                                    <button type="button" data-toggle="dropdown" class="btn btn-circle btn-icon btn-emojis"><i class="fa fa-smile"></i></button>
                                    <div class="dropdown-menu face-dropdown"></div>
                                </div>
                            </div>
                        </div>
                        <i></i>
                        <div class="mt-1">
                            <button type="button" class="btn btn-flat btn-info btn-post"></button>
                        </div>
                    </div>
                    <div class="body"></div>
                </div>`;
    },
    /**
     * 初始化多对一编译控件
     */
    init() {
        let me = this;
        let dom = me.dom;
        me.limit = me.nvl(eval(dom.attr('limit')), me.limit);
        me.dom.html(me.getTpl()).on('click', '.btn-subscribe', function () {
            if (!me.design) {
                let btn = $(this);
                me.messageSubscribe(btn.hasClass('unfollow') ? 'messageUnsubscribe' : 'messageSubscribe');
            }
        }).on('click', '.emoji', function () {
            let item = $(this);
            let input = me.dom.find('.chat-input');
            input.val(input.val() + item.html()).focus();
        }).on('mouseenter', '.btn-subscribe', function () {
            let btn = $(this);
            if (btn.hasClass('following')) {
                btn.removeClass('following').addClass('unfollow').html('取消关注'.t());
            }
        }).on('mouseleave', '.btn-subscribe', function () {
            let btn = $(this);
            if (btn.hasClass('unfollow')) {
                btn.removeClass('unfollow').addClass('following').html('已关注'.t());
            }
        }).on('click', '.btn-add-follower', function () {
            if (me.view.auths.includes('update')) {
                me.addFollower();
            }
        }).on('click', '.btn-remove-follower', function () {
            let uid = $(this).attr('data-id');
            if (me.view.auths.includes('update')) {
                me.messageSubscribe('messageUnsubscribe', [uid]);
            }
        }).on('click', '.btn-chat', function () {
            if (!me.design) {
                let btn = $(this);
                if (btn.hasClass('active')) {
                    btn.removeClass('active');
                    me.dom.find('.chatter').addClass('d-none');
                } else {
                    me.dom.find('.btn-chat.active').removeClass('active');
                    btn.addClass('active');
                    me.dom.find('.chatter').removeClass('d-none');
                    if (btn.hasClass('btn-send')) {
                        me.dom.find('.to-followers').css('height', 'auto');
                        me.dom.find('.chat-input').attr('placeholder', '向关注者发送消息...'.t()).focus();
                        me.dom.find('.btn-post').html('发送'.t()).attr('notify', '1');
                    } else {
                        me.dom.find('.to-followers').css('height', 0);
                        me.dom.find('.chat-input').attr('placeholder', '记录内部备注...'.t()).focus();
                        me.dom.find('.btn-post').html('记录'.t()).attr('notify', null);
                    }
                }
            }
        }).on('click', '.btn-post', function () {
            let btn = $(this);
            me.postMessage(btn.attr('notify'));
        });
        if (me.view.auths.includes('update')) {
            me.dom.find('.follower').attr('data-toggle', 'dropdown');
        }
        me.initPager();
        me.initToolbar();
        me.initChatter();
        me.initEmojis();
    },
    initChatter() {
        let me = this;
        me.dom.find('.to-followers').html('发送至：关注者'.t());
        if (env.user.image) {
            me.dom.find('.chatter .avatar').css('background', `url(${jmaa.web.getTenantPath()}/attachment/${env.user.image}) no-repeat center/cover`);
        } else {
            me.dom.find('.chatter .avatar').html(env.user.name[0].toUpperCase());
        }
    },
    initEmojis() {
        let me = this;
        let html = [];
        for (let e of me.emojis) {
            html.push(`<span class="emoji">${e}</span>`);
        }
        me.dom.find('.face-dropdown').html(html.join(''));
    },
    initToolbar() {
        let me = this;
        let tb = me.dom.find('.header .toolbar');
        new JToolbar({
            dom: tb,
            arch: `<toolbar>${tb.html()}</toolbar>`,
            auths: 'read',
            defaultButtons: 'reload',
            target: me,
            view: me,
            design: me.design,
        });
    },
    initPager() {
        let me = this;
        me.pager = new JPager({
            dom: me.dom.find('[role=pager]'),
            limit: me.limit,
            pageChange(e, pager) {
                if (!me.design) {
                    me.data = null;
                    me.load();
                }
            },
            counting(e, pager) {
                if (!me.design) {
                    me.countData(pager);
                }
            },
        });
    },
    addFollower() {
        let me = this;
        jmaa.showDialog({
            title: '邀请关注者'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    fields: {user_id: {name: `user_id`, type: 'many2one'}},
                    arch: `<form cols="1" logAccess="0">
                                <field name="user_id" label="收件人" editor="user-tags" required="1"></field>
                                <editor type="boolean" name="notify" label="发送消息"></editor>
                                <editor type="text" t-visible="notify" name="content" label="消息" required="1"></editor>
                          </form>`,
                    model: 'bbs.follower.invite',
                    view: me,
                });
                dialog.form.create({notify: true});
                jmaa.rpc({
                    model: 'bbs.follower.invite',
                    method: 'getInviteMessage',
                    args: {
                        resModel: me.model,
                        resId: me.owner.dataId,
                    },
                    onsuccess(r) {
                        dialog.form.editors.content.setValue(r.data);
                    }
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'followerInvite',
                    args: {
                        ids: [me.owner.dataId],
                        userIds: data.user_id,
                        notify: data.notify,
                        message: data.content,
                    },
                    onsuccess(r) {
                        dialog.close();
                        me.loadFollower();
                    }
                })
            }
        });
    },
    postMessage(notify) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'addMessage',
            args: {
                ids: [me.owner.dataId],
                body: me.dom.find('.chat-input').val(),
                notify: Boolean(notify)
            },
            onsuccess(r) {
                me.dom.find('.chat-input').val('');
                me.dom.find('.chatter').addClass('d-none');
                me.dom.find('.btn-chat.active').removeClass('active');
                me.loadMessage();
            },
        });
    },
    messageSubscribe(method, userIds) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method,
            args: {
                ids: [me.owner.dataId],
                userIds,
            },
            onsuccess(r) {
                me.loadFollower();
            },
        });
    },
    renderMessage(data) {
        let me = this;
        let html = [];
        if (data.values.length > 0) {
            for (let row of data.values) {
                let tracking = [];
                for (let t of row.tracking_values) {
                    tracking.push(`<li>${t.fieldLabel}: ${t.oldValue} ⇨ ${t.newValue}</li>`);
                }
                let content = row.subject;
                if (content) {
                    content += "<br/>";
                }
                content += row.body.replaceAll('\n', '<br/>');
                let avatar = row.author_image ? `<div class="avatar" style="background: url(${jmaa.web.getTenantPath()}/attachment/${row.author_image[0].id}) no-repeat center/cover"></div>`
                    : `<div class="avatar">${row.author_id[1][0].toUpperCase()}</div>`;
                html.push(`<div class="message">
                    ${avatar}
                    <div class="m-body">
                        <div class="m-header">
                            ${row.author_id[1]}
                            <span class="text-muted text-xs pl-3">${row.create_date}</span>
                        </div>
                        <div class="m-content">
                            ${content}
                            ${tracking.length ? `<ul>${tracking.join('')}</ul>` : ''}
                        </div>
                    </div>
                </div>`);
            }
            me.pager.update(data);
        } else {
            html = `<div class="empty">${'没有数据'.t()}</div>`;
            me.pager.noData();
        }
        me.dom.find('.body').html(html);
    },
    countData(pager) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'countByField',
            args: {
                relatedField: me.field.name,
                criteria: me.getFilter(),
            },
            context: {
                active_test: false,
                company_test: false,
            },
            onsuccess(r) {
                pager.update({
                    total: r.data,
                });
            },
        });
    },
    getFilter() {
        let me = this;
        let criteria = [['model', '=', me.owner.model], ['res_id', '=', me.owner.dataId], ['message_type', '=', 'comment']];
        let tFilter = me.dom.attr('t-filter');
        if (tFilter) {
            const fn = me.view[tFilter];
            if (!fn) {
                console.error("未定义t-filter方法:" + tFilter);
            } else {
                criteria = fn.call(me.view, criteria, me);
            }
        }
        return criteria;
    },
    load() {
        let me = this;
        me.loadMessage();
        me.loadFollower();
    },
    reload() {
        this.load();
    },
    loadMessage() {
        let me = this;
        me.dom.find('.body').html(`<div class="empty">${'数据加载中'.t()}</div>`);
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: me.field.name,
                criteria: me.getFilter(),
                offset: me.pager.getOffset(),
                limit: me.pager.getLimit(),
                fields: ['subject', 'body', 'author_id', 'tracking_values', 'create_date', 'author_image'],
                order: 'create_date desc',
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                me.renderMessage(r.data);
            },
        });
    },
    loadFollower() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: "message_follower_ids",
                criteria: [['res_model', '=', me.owner.model], ['res_id', '=', me.owner.dataId]],
                limit: 1000,
                fields: ['name', 'user_id', 'image'],
            },
            onsuccess(r) {
                me.renderFollower(r.data);
            },
        });
    },
    renderFollower(data) {
        let me = this;
        me.follower = data.values;
        let uid = env.user.id;
        let user = data.values.find(d => d.user_id == uid);
        let btn = me.dom.find('.btn-subscribe');
        if (user) {
            btn.html('已关注'.t()).addClass('following');
        } else {
            btn.html('关注'.t()).removeClass('following unfollow');
        }
        me.dom.find('.follower-count').html(data.values.length);
        let items = [`<button type="button" class="btn-add-follower dropdown-item">${'添加关注者'.t()}</button>`];
        if (me.follower.length) {
            items.push('<div role="separator" class="dropdown-divider"></div>');
        }
        for (let row of me.follower) {
            let avatar = row.image ? `<div class="avatar" style="background: url(${jmaa.web.getTenantPath()}/attachment/${row.image[0].id}) no-repeat center/cover"></div>`
                : `<div class="avatar">${row.name[0].toUpperCase()}</div>`;
            items.push(`<div class="follower-item dropdown-item">
                            <div class="d-flex w-100 align-items-center">
                                ${avatar}
                                <span>${row.name}</span>
                                <div class="flex-grow-1"></div>
                                <button type="button" title="移除此关注者" data-id="${row.user_id}" class="btn btn-icon btn-flat btn-remove-follower"><i class="fa fa-times"></i></button>
                            </div>
                        </div>`);
        }
        $('.follower-dropdown').html(items.join(''));
    },
    /**
     * 获取数据
     */
    getValue() {
        return [];
    },
    /**
     * 设置值
     * @param v
     */
    setValue() {
        let me = this;
        me.pager.reset();
        me.load();
    },
});
jmaa.editor('user-tags', {
    extends: 'editors.many2many-tags',
    init() {
        let me = this;
        me.callSuper();
        me.dom.find('.btn-group').hide();
    },
    filterCriteria() {
        let me = this;
        if (me.keyword) {
            return [["present", "like", me.keyword]];
        }
        return [["id", "=", '-']];
    },
    renderItems(values) {
        let me = this;
        me.data = values;
        if (values.length) {
            me.callSuper(values);
        } else if (me.keyword) {
            me.dom.find('.lookup-info').html('没有数据'.t());
        } else {
            me.dom.find('.lookup-info').html('请输入'.t());
        }
        me.placeDropdown();
    },
});

