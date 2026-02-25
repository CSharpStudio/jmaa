jmaa.define('JApp', {
    __init__() {
        let me = this;
        me.initWorkspace();
        me.initLinkMenu();
        me.initMenu();
        me.initMenuSearch();
        me.initCompany();
        me.initUpload();
        me.initUser();
        me.initHome();
        $(window).on('resize', function () {
            me.updateTabScrollBtn();
        });
    },
    updateLicense() {
        jmaa.showDialog({
            title: '授权'.t(),
            css: 'sm',
            init(dialog) {
                dialog.body.html(`<div class="card-body row">
                    <div class="col-md-12">
                        <form class="grid" role="form-body" style="grid-template-columns: repeat(1, 1fr);">
                            <div style="grid-column:span 1;grid-row:span 1;" class="form-group col-12">
                                <label>公司</label><span class="text-danger"> *</span>
                                <div data-field="company" data-label="原密码">
                                    <input type="text" autocomplete="off" class="form-control" id="company-input"/>
                                </div>
                                <span class="invalid-feedback"></span>
                            </div>
                            <div style="grid-column:span 1;grid-row:span 1;" class="form-group col-12">
                                <label>授权码</label><span class="text-danger"> *</span>
                                <div data-field="license">
                                    <textarea rows="3" autocomplete="off" class="form-control" id="license-input"></textarea>
                                </div>
                                <span class="invalid-feedback"></span>
                            </div>
                        </form>
                    </div>
                </div>`);
            },
            submit(dialog) {
                let company = dialog.body.find('#company-input').val();
                let license = dialog.body.find('#license-input').val();
                if (!company) {
                    jmaa.msg.error('公司不能为空');
                    return;
                }
                if (!license) {
                    jmaa.msg.error('授权不能为空');
                    return;
                }
                jmaa.rpc({
                    model: 'ir.http',
                    method: 'updateLicense',
                    args: {
                        company,
                        license,
                    },
                    onsuccess() {
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    },
                });
            },
        });
    },
    initWorkspace() {
        let me = this;
        // iframe
        me.ws = $('[data-widget=jui-frame]').JFrame({
            onTabClick(item) {
                return item;
            },
            onTabChanged(item) {
                if (item == 'home') {
                    $('[role=app-menu] .app-menus').empty();
                    $('[role=app-menu] .app-menu').html(`<a href="#" class="nav-app-link nav-link pl-1"><p>${'首页'.t()}</p></a>`);
                    me.menu.updateMenuScroll();
                    window.location.hash = '';
                } else if (item) {
                    let tabId = item.attr('href');
                    let frame = $(tabId + ' iframe');
                    if (frame.length) {
                        let src = frame.attr('src');
                        let menu = frame.attr('menu');
                        if (!menu) {
                            menu = item.attr('id').substring(4);
                            frame.attr('menu', menu);
                        }
                        window.location.hash = `u=${encodeURIComponent(src.replace(window.location.origin, ''))}&t=${encodeURIComponent(item.html().trim())}&m=${menu}`;
                        let m = me.menu.data[menu];
                        if (m) {
                            let active = m.path || menu;
                            me.menu.showAppMenu(m.topMenu || menu);
                            $('.nav-sidebar .active').removeClass('active');
                            $(`.nav-sidebar .nav-link[data-id=${active}]`).addClass('active');
                            me.menu.activeNavMenu(active);
                        }
                    } else {
                        window.location.hash = '';
                    }
                }
                me.updateTabScrollBtn();
                return item;
            },
            onTabCreated(item) {
                return item;
            },
            onTabClosing(item, callback) {
                let tabId = item.find('a.nav-link[role=tab]').attr('href');
                let frame = $(tabId + ' iframe');
                if (frame.length) {
                    let w = frame[0].contentWindow;
                    let v = w.view;
                    if (v && v.isDirty()) {
                        if (!item.hasClass('active')) {
                            let parent = item.parent();
                            if (!parent.find('.to-active').length) {
                                item.addClass('to-active');
                            }
                            setTimeout(() => {
                                let toActive = parent.find('.to-active').removeClass('to-active');
                                if (toActive.length) {
                                    me.ws.switchTab(toActive.find('a.nav-link'));
                                }
                            }, 50);
                        }
                        w.jmaa.msg.confirm({
                            title: '提示'.t(),
                            content: '数据未保存，是否继续？'.t(),
                            submit() {
                                callback();
                            }
                        });
                    } else {
                        callback();
                    }
                } else {
                    callback();
                }
            },
            autoIframeMode: true,
            autoItemActive: false,
            autoShowNewTab: true,
            autoDarkMode: false,
            allowDuplicates: false,
            loadingScreen: false,
            useNavbarItems: true,
            allowReload: false,
        });
    },
    updateTabScrollBtn() {
        let tabs = $('[role=tablist]');
        let visible = false;
        if (tabs.scrollLeft() > 0) {
            visible = true;
        } else {
            let last = tabs.children('li').last();
            if (last.length && last.position().left + last.width() > tabs.width()) {
                visible = true;
            }
        }
        if (visible) {
            $('[data-widget=jui-frame-scrollleft],[data-widget=jui-frame-scrollright]').show();
        } else {
            $('[data-widget=jui-frame-scrollleft],[data-widget=jui-frame-scrollright]').hide();
        }
    },
    initLinkMenu() {
        let me = this;
        if (jmaa.web.cookie('ctx_trace')) {
            $('[data-widget=performance-analysis]').addClass('active');
        }
        $(document).on('click', '[data-widget=user-logout]', function () {
            me.logout();
        }).on('click', '[data-widget=debug-exit]', function () {
            jmaa.web.cookie('ctx_trace', '', {expires: -1});
            jmaa.web.cookie('ctx_debug', false);
            window.location.reload();
        }).on('click', '[data-widget=performance-analysis]', function () {
            let link = $(this);
            me.performanceAnalysis(link);
        }).on('click', '[data-widget=lang-collect]', function () {
            me.langCollect();
        }).on('click', '[data-widget=about]', function () {
            me.showAbout();
        }).on('click', '[data-widget=user-upload]', function () {
            me.upload.open();
        }).on('click', '[data-widget=file-download]', function () {
            me.download();
        }).on('click', '.theme-items label', function (e) {
            e.preventDefault();
            e.stopPropagation();
            me.updateTheme($(this));
        });
    },
    updateTheme(select) {
        $('.theme-items .active').removeClass('active');
        select.addClass('active');
        let theme = select.find('input').attr("checked", true).val();
        $('html').removeClass().addClass(theme);
        localStorage.setItem('user_theme', theme);
        jmaa.rpc({
            module: 'base',
            model: 'rbac.user',
            method: 'updatePersonal',
            args: {
                values: {theme}
            }
        });
    },
    download() {
        let getFileSize = function (size) {
            if (size < 1024) {
                return size + ' B';
            } else if (size < 1024 * 1024) {
                return (size / 1024).toFixed(2) + ' K';
            } else if (size < 1024 * 1024 * 1024) {
                return (size / 1024 / 1024).toFixed(2) + ' M';
            }
            return (size / 1024 / 1024 / 1024).toFixed(2) + ' G';
        }
        jmaa.showDialog({
            title: '文件下载'.t(),
            init(dialog) {
                jmaa.rpc({
                    model: 'res.download',
                    method: 'readDownload',
                    args: {
                        fields: ['name', 'path', 'image', 'file'],
                    },
                    onsuccess(r) {
                        let html = [];
                        for (let v of r.data) {
                            let src = v.image ? jmaa.web.getTenantPath() + "/attachment/" + v.image[0].id : '';
                            let size = v.file && v.file[0] ? ` (${getFileSize(v.file[0].size)})` : '';
                            html.push(`<div style="align-items:center;" class="card">
                                <div style="width: 200px;height: 200px;display: flex;align-items: center;justify-content: center;">
                                    <img style="max-width: 200px;max-height: 200px" src="${src}">
                                </div>
                                <a class="mb-2" target="_blank" href="${v.path}">${v.name}${size}</a>
                            </div>`)
                        }
                        dialog.body.addClass('p-4 row').css('column-gap', '20px').html(html.join(''));
                    },
                });
            },
        });
    },
    langCollect() {
        if (top.window.langCollect) {
            jmaa.rpc({
                model: 'res.lang',
                method: 'addWords',
                args: {
                    words: Object.keys(top.window.langCollect),
                },
                onsuccess(r) {
                    jmaa.msg.show('成功保存%s条'.t().replace('%s', r.data));
                },
            });
        } else {
            jmaa.msg.show('没有收集到数据'.t());
        }
    },
    showAbout() {
        let me = this;
        jmaa.showDialog({
            title: '关于'.t(),
            css: 'sm',
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.http',
                    method: 'loadAbout',
                    onsuccess(r) {
                        dialog.body.addClass('p-3').html(r.data);
                        dialog.body.find('.update-license').on('click', function () {
                            me.updateLicense();
                        });
                    },
                });
            },
        });
    },
    performanceAnalysis(link) {
        if (link.hasClass('active')) {
            link.removeClass('active')
            jmaa.web.cookie('ctx_trace', '', {expires: -1});
        } else {
            jmaa.showDialog({
                title: '管理员登录'.t(),
                css: 'modal-sm',
                init(dialog) {
                    dialog.form = dialog.body.JForm({
                        arch: `<form cols="1" style="padding:20px">
                            <editor name="login" type="char" required="1" label="管理员账号"></editor>
                            <editor name="password" type="password" required="1" label="密码"></editor>
                         </form>`,
                    });
                },
                submit(dialog) {
                    if (!dialog.form.valid()) {
                        return;
                    }
                    let data = dialog.form.getData();
                    jmaa.rpc({
                        model: 'rbac.jwt',
                        module: 'base',
                        method: "getAdminJwt",
                        args: {
                            login: data.login,
                            password: window.btoa(unescape(encodeURIComponent(data.password)))
                        },
                        onsuccess(r) {
                            link.addClass('active')
                            jmaa.web.cookie('ctx_trace', r.data);
                            dialog.close();
                        },
                        onerror(e) {
                            jmaa.msg.error(e);
                        }
                    })
                }
            });
        }
    },
    logout() {
        jmaa.rpc({
            model: 'rbac.user',
            method: 'logout',
            args: {},
            onsuccess(r) {
                jmaa.web.cookie('ctx_token', '', {expires: -1});
                window.location.replace(jmaa.web.getTenantPath() + '/login');
            },
        });
    },
    initMenu() {
        this.menu = new JMenu({dom: $('ul[role=menu]')});
    },
    initMenuSearch() {
        this.menuSearch = new JMenuSearch({dom: $('[data-widget=menu-search]')});
    },
    initCompany() {
        new JCompany({dom: $('[data-widget=company]')});
    },
    initUpload() {
        this.upload = new JUpload({dom: $('[data-widget=upload]')});
    },
    initUser() {
        let icon = $('.user-icon');
        let n = icon.html();
        n = ((n || '').trim()[0] || '').toUpperCase();
        icon.html(n);
        jmaa.rpc({
            model: 'rbac.user',
            method: 'getPersonal',
            args: {
                fields: ['image']
            },
            onsuccess(r) {
                if (r.data.image && r.data.image[0]) {
                    icon.html('').css('background', `url(${jmaa.web.getTenantPath()}/attachment/${r.data.image[0].id}) no-repeat center/cover`);
                }
            }
        });
        $('[data-widget=user-account]').attr('href', jmaa.web.getTenantPath() + '/view#model=rbac.user&views=form&key=personal&view=form');
    },
    initHome() {
        const contentWrapperHeight = parseFloat($('.content-wrapper').css('height'));
        const navbarHeight = $('.content-wrapper.jui-frame-mode .nav').outerHeight();
        $('[data-widget=home-page]').attr('src', jmaa.web.getTenantPath() + '/home').height(contentWrapperHeight - navbarHeight);
    },
    load() {
        let me = this;
        me.loadMenu(function () {
            let params = jmaa.web.getParams(window.location.hash.substring(1));
            if (params.u) {
                app.ws.createTab(params.t, params.u, params.m, true);
            }
        });
    },
    loadMenu(callback) {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.menu',
            method: 'loadMenu',
            args: {},
            onsuccess(r) {
                for (let d in r.data) {
                    let m = r.data[d];
                    if (m.name) {
                        m.name = m.name.t();
                    }
                }
                me.menu.setData(r.data);
                me.menuSearch.setData(r.data);
                callback && callback();
            },
        });
    }
});
let loadApp = function () {
    jmaa.create('JFullScreen');
    jmaa.create('JIconLib');
    window.app = jmaa.create('JApp');
    app.load();
    $('t').each(function () {
        let el = $(this);
        el.replaceWith(el.text().t());
    });
};
$(function () {
    window.isDebug = eval(jmaa.web.cookie('ctx_debug'));
    let theme = localStorage.getItem('user_theme');
    if (theme) {
        $('html').removeClass().addClass(theme);
        $('.theme-items .active').removeClass('active');
        $(`.theme-items input[value=${theme}]`).attr("checked", true).closest('label').addClass('active');
    }
    let user = JSON.parse(localStorage.getItem("user_info") || '{}');
    let company = JSON.parse(localStorage.getItem("company_info") || '{}');
    window.env = {user, company};

    function updateLang(lang, version, callback) {
        jmaa.mask('<div><div>加载语言包，请稍等</div><div>loading language, please wait a moment</div></div>');
        jmaa.rpc({
            model: 'res.lang',
            method: 'getLocalization',
            onsuccess(r) {
                localStorage.setItem('langver-' + lang, version);
                localStorage.setItem('lang-' + lang, JSON.stringify(r.data));
                jmaa.mask();
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
