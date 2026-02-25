jmaa.define('JApp', {
    __init__() {
        let me = this;
        me.dom = $('body').on('click', '.link-logout', function () {
            window.location.href = jmaa.web.getTenantPath() + '/login?url=' + jmaa.web.getTenantPath() + '/m';
        }).on('click', '.link-language', function () {
            me.editLanguage();
        }).on('click', '.link-password', function () {
            me.editPassword();
        }).on('click', '.link-about', function () {
            me.showAbout();
        }).on('click', '.user-content', function () {
            me.editUser();
        }).on('click', '#common .back-link', function () {
            let back = $(this).attr('back') || '#main';
            $.mobile.changePage(back);
        });
        me.initMenu(function () {
            $('[data-role=page]').removeClass('d-none');
            $('.ui-startup').remove();
        });
        me.initUser();
        if (top.NativeJS) {
            $('.link-update .app-version').html("当前版本:".t() + top.NativeJS.getVersion());
            $('.link-update').on('click', function () {
                top.NativeJS.checkUpdate();
            });
        } else {
            $('.link-update').remove();
        }
        me.ws = {
            openTab(menu, url) {
                me.openPage({
                    title: menu,
                    back: '#main',
                    init(body) {
                        body.html(`<iframe src="${url}"></iframe>`)
                    }
                });
            }
        };
        let hash = window.location.hash;
        if (hash) {
            if (hash.startsWith('#view')) {
                let url = decodeURIComponent(hash.substring(6));
                $.mobile.changePage("#view");
                me.dom.find('#view').html(`<iframe src="${url}"/>`);
                window.location.hash = $.param({view: url});
            } else if (hash.startsWith('#common')) {
                let method = decodeURIComponent(hash.substring(8));
                method && me[method].call(me);
            }
        }
    },
    editUser() {
        let me = this;
        me.openPage({
            title: '用户信息'.t(),
            back: '#user',
            hash: 'editUser',
            init(body) {
                body.html(`<div class="header-bar">
                    <div class="toolbar">
                        <button type="button" class="ui-btn">${'保存'.t()}</label>
                    </div>
                </div>
                <div class="form" style="height:calc(100% - 43px);"></div>`);
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        type: 'form',
                        key: 'personal-mobile',
                        model: 'rbac.user',
                    },
                    onsuccess(r) {
                        let form = body.find('.form').JForm({
                            model: r.data.model,
                            fields: r.data.fields,
                            arch: r.data.views.form.arch,
                            view: {urlHash: ''},
                        });
                        form.dom.enhanceWithin();
                        jmaa.rpc({
                            model: 'rbac.user',
                            method: 'getPersonal',
                            args: {
                                fields: form.getFields()
                            },
                            context: {
                                usePresent: true,
                            },
                            onsuccess(r) {
                                form.loadData(r.data);
                            }
                        });
                        body.find('.ui-btn').on('click', function () {
                            if (!form.valid()) {
                                return jmaa.msg.error(form.getErrors());
                            }
                            let data = form.getSubmitData();
                            jmaa.rpc({
                                model: 'rbac.user',
                                method: 'updatePersonal',
                                args: {
                                    values: data,
                                },
                                onsuccess(r) {
                                    if (data.image) {
                                        jmaa.rpc({
                                            model: 'rbac.user',
                                            method: 'getPersonal',
                                            args: {
                                                fields: ['image']
                                            },
                                            onsuccess(r) {
                                                let user = JSON.parse(localStorage.getItem("user_info") || '{}');
                                                user.image = r.data.image && r.data.image.length ? r.data.image[0].id : null;
                                                localStorage.setItem('user_info', JSON.stringify(user));
                                                let icon = me.dom.find('.user-icon');
                                                if (user.image) {
                                                    icon.html('').css('background', `url(${jmaa.web.getTenantPath()}/attachment/${user.image}) no-repeat center/cover`);
                                                } else {
                                                    icon.removeAttr('style').html(((user.name || '').trim()[0] || '').toUpperCase());
                                                }
                                            }
                                        });
                                    }
                                    jmaa.msg.show('操作成功'.t());
                                }
                            });
                        });
                    }
                })
            }
        });
    },
    editLanguage() {
        let me = this;
        me.openPage({
            title: '切换语言'.t(),
            back: '#setting',
            hash: 'editLanguage',
            init(body) {
                let user = JSON.parse(localStorage.getItem("user_info") || '{}');
                jmaa.rpc({
                    model: 'res.lang',
                    method: 'getInstalled',
                    onsuccess(r) {
                        let html = [];
                        for (let item of Object.keys(r.data)) {
                            html.push(`<a class="list-item link" data-value="${item}">
                                <span class="title">
                                    ${r.data[item]}
                                </span>
                                ${user.lang[0] == item ? `<div class="text-end">${'使用中'.t()}</div>` : ''}
                            </a>`);
                        }
                        body.html(`<div class="list-view">${html.join('')}</div>`);
                        let updating = false;
                        body.find('.list-view').on('click', '.list-item', function () {
                            if (updating) {
                                return;
                            }
                            updating = true;
                            body.find('.text-end').remove();
                            let lang = $(this).attr('data-value');
                            let langName = $(this).find('.title').html();
                            jmaa.rpc({
                                model: 'rbac.user',
                                method: 'updatePersonal',
                                args: {
                                    values: {lang},
                                },
                                onsuccess() {
                                    updating = false;
                                    user.lang = [lang, langName];
                                    localStorage.setItem('user_info', JSON.stringify(user));
                                    window.location.reload();
                                },
                                onerror(err) {
                                    updating = false;
                                    jmaa.msg.error(err);
                                },
                            });
                        });
                    },
                });
            }
        });
    },
    editPassword() {
        let me = this;
        me.openPage({
            title: '修改密码'.t(),
            back: '#setting',
            hash: 'editPassword',
            init(body) {
                let form = $('<div></div>').JForm({
                    arch: `<form cols="1">
                            <editor name="old_pwd" type="password" label="原密码" required="1"></editor>
                            <editor name="new_pwd" type="password" label="新密码" required="1"></editor>
                            <editor name="confirm_pwd" type="password" label="确认密码" required="1"></editor>
                            <button type="button" class="btn-confirm ui-btn ui-flat">${'确认'.t()}</button>
                        </form>`
                });
                body.html(form.dom.enhanceWithin());
                form.dom.on('click', '.btn-confirm', function () {
                    if (!form.valid()) {
                        return jmaa.msg.error(form.getErrors());
                    }
                    let data = form.getData();
                    if (data.new_pwd != data.confirm_pwd) {
                        let msg = '确认密码与新密码不一致'.t();
                        form.setInvalid('confirm_pwd', msg);
                        return jmaa.msg.error(msg);
                    }
                    jmaa.rpc({
                        model: 'rbac.user',
                        method: 'changePassword',
                        args: {
                            oldPassword: window.btoa(unescape(encodeURIComponent(data.old_pwd))),
                            newPassword: window.btoa(unescape(encodeURIComponent(data.new_pwd)))
                        },
                        onsuccess(r) {
                            form.create();
                            jmaa.msg.show('操作成功'.t());
                        }
                    });
                });
            },
        });
    },
    showAbout() {
        let me = this;
        me.openPage({
            title: '关于'.t(),
            back: '#setting',
            hash: 'showAbout',
            init(body) {
                body.html('加载中'.t());
                jmaa.rpc({
                    model: 'ir.http',
                    method: 'loadAbout',
                    onsuccess(r) {
                        body.html(`<div class="p-3">${r.data}</div>`);
                        body.find('.update-license').hide();
                    },
                });
            }
        });
    },
    initUser() {
        let me = this;
        let icon = me.dom.find('.user-icon');
        let user = JSON.parse(localStorage.getItem("user_info") || '{}');
        let company = JSON.parse(localStorage.getItem("company_info") || '{}');
        if (user.image) {
            icon.html('').css('background', `url(${jmaa.web.getTenantPath()}/attachment/${user.image}) no-repeat center/cover`);
        } else {
            icon.html(((user.name || '').trim()[0] || '').toUpperCase());
        }
        me.dom.find('.user-account').html(user.login);
        me.dom.find('.user-lang').html(user.lang[1]);
        me.dom.find('.user-company').html(company.name);
        //TODO 头像编辑
    },
    initMenu(callback) {
        let me = this;
        $('.startup-info').html('加载菜单'.t());
        jmaa.rpc({
            model: 'ir.ui.menu.mobile',
            method: 'loadMenu',
            args: {},
            onsuccess(r) {
                for (const d in r.data) {
                    const m = r.data[d];
                    if (m.name) {
                        m.name = m.name.t();
                    }
                }
                me.renderMenu(r.data);
                callback();
            },
        });
    },
    renderMenu(data) {
        let me = this;
        let html = [];
        let getSub = function (sub) {
            let h = [];
            let colors = ['#660066', '#af26a7', '#c29425', '#e26c24', '#036173', '#a51111', '#2f6fcb', '#086f9e', '#7c1f3e'];
            for (let id of sub) {
                let m = data[id];
                let icon;
                if (m.icon) {
                    icon = `<img src="${m.icon}">`;
                } else {
                    let n = (m.name.trim()[0] || '').toUpperCase();
                    let c = 0;
                    for (let i = 0; i < m.name.length; i++) {
                        c += m.name.charCodeAt(i);
                    }
                    icon = `<div class="m-icon" style="background:${colors[c % colors.length]}">${n}</div>`
                }
                h.push(`<div class="menu-item ui-btn" data-url="${m.url}" data-id="${id}">
                        ${icon}
                        <span class="menu-text">${m.name}</span>
                    </div>`);
            }
            return h.join('');
        }
        for (let id of data.root) {
            let m = data[id];
            html.push(`<div class="menu-group">
                        <label>${m.name}</label>
                        <div class="menu-items">
                            ${getSub(m.sub || [])}
                        </div>
                    </div>`);
        }
        me.dom.find('.menu-groups').on('click', '.menu-item', function () {
            let btn = $(this);
            let url = btn.attr('data-url');
            let menu = btn.attr('data-id');
            me.openView(menu, url);
        }).find('.main-menus').html(html.join(''));
    },
    openView(menu, url) {
        let me = this;
        let frames = me.dom.find('#view iframe').addClass('d-none')
        let view = me.dom.find(`#view iframe[menu=${menu}]`).removeClass('d-none')
        if (view.length) {
            setTimeout(() => {
                let el = view.contents().find('.focus');
                setTimeout(() => {
                    el.attr('readonly', true).focus();
                    setTimeout(() => el.attr('readonly', false), 100);
                }, 500);
            }, 500);
        } else {
            me.dom.find('#view').append(`<iframe src="${url}" menu="${menu}"/>`);
        }
        if (frames.length > 5) {
            me.dom.find('#view iframe.d-none:first').remove();
        }
        $.mobile.changePage("#view");
        window.location.hash = $.param({view: url});
    },
    openPage(opt) {
        let me = this;
        me.dom.find('#common .title').html(opt.title);
        if (opt.init) {
            opt.init(me.dom.find('#common .ui-content'));
        }
        if (opt.back) {
            me.dom.find('#common .back-link').attr('back', opt.back);
        }
        $.mobile.changePage("#common");
        if (opt.hash) {
            window.location.hash = $.param({common: opt.hash});
        }
    },
});
$(function () {
    function loadApp() {
        $('t').each(function () {
            let el = $(this);
            el.replaceWith(el.text().t());
        });
        window.app = new JApp();
    }

    function updateLang(lang, version, callback) {
        $('.startup-info').html('加载语言包'.t());
        jmaa.rpc({
            model: 'res.lang',
            method: 'getLocalization',
            onsuccess(r) {
                localStorage.setItem('langver-' + lang, version);
                localStorage.setItem('lang-' + lang, JSON.stringify(r.data));
                //jmaa.mask();
                delete window.langData;
                callback();
            },
        });
    }

    let lang = jmaa.web.cookie('ctx_lang');
    jmaa.rpc({
        model: 'res.lang',
        method: 'getVersion',
        onsuccess(r) {
            let version = localStorage.getItem('langver-' + lang);
            if (version != r.data) {
                updateLang(lang, r.data, loadApp);
            } else {
                loadApp();
            }
        },
    });
});
