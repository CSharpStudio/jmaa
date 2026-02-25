jmaa.define('JPad', {
    __init__() {
        let me = this;
        me.initWorkspace();
        me.initLinkMenu();
        me.initCompany();
        me.initUpload();
        me.initUser();
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
        me.ws = {
            createTab(title, url, key) {
                title = title ? title.trim() : '';
                $('.app-menu').html(title);
                $('.m-menu').hide();
                $('.m-view').show();
                let frames = $('.m-view iframe').hide();
                let view = $(`.m-view iframe[key=${key}]`).show();
                if (!view.length) {
                    let frame = $(`<iframe src="${url}" key="${key}"/>`);
                    $('.m-view').append(frame);
                    frame.on('load', function () {
                        $(this).contents().find('body').addClass('mobile');
                    });
                }
                if (frames.length > 5) {
                    $('.m-view iframe:first').remove();
                }
                window.location.hash = `u=${encodeURIComponent(url.replace(window.location.origin, ''))}&t=${encodeURIComponent(title)}&m=${key}`;
            },
            openTab(title, url, key) {
                this.createTab(title, url, key);
            }
        };
    },
    initLinkMenu() {
        let me = this;
        if (jmaa.web.cookie('ctx_trace')) {
            $('[data-widget=performance-analysis]').addClass('active');
        }
        $(document).on('click', '[data-widget=user-logout]', function () {
            me.logout();
        }).on('click', '[data-widget=about]', function () {
            me.showAbout();
        }).on('click', '[data-widget=user-upload]', function () {
            me.upload.open();
        }).on('click', '[data-widget=file-download]', function () {
            me.download();
        }).on('click', '[data-widget=user-account]', function () {
            let a = $(this);
            me.ws.openTab(a.text(), a.attr('href'), a.attr('data-id'));
            return false;
        }).on('click', '.nav-menu', function () {
            $('.m-menu').show();
            $('.m-view').hide();
            return false;
        }).on('click', '.app-menu', function () {
            $('.m-menu').hide();
            $('.m-view').show();
            return false;
        }).on('click', '.sidebar-search-results a, .m-menus a', function () {
            let a = $(this).clone();
            if (a.attr('href') === undefined) {
                a = $(this).parents('a').clone();
            }
            a.find('.right, .search-path, .m-icon').remove();
            let title = a.find('span').text();
            if (title === '') {
                title = a.text();
            }
            me.ws.openTab(title, a.attr('href'), a.attr('data-id'));
            return false;
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
                                <img style="max-width: 200px;max-height: 200px" src="${src}">
                                <a class="mb-2" target="_blank" href="${v.path}">${v.name}${size}</a>
                            </div>`)
                        }
                        dialog.body.addClass('p-4 row').css('column-gap', '20px').html(html.join(''));
                    },
                });
            },
        });
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
    logout() {
        jmaa.rpc({
            model: 'rbac.user',
            method: 'logout',
            args: {},
            onsuccess(r) {
                jmaa.web.cookie('ctx_token', '', {expires: -1});
                window.location.href = jmaa.web.getTenantPath() + '/login?url=/root/pad';
            },
        });
    },
    initMenu(data) {
        let me = this;
        for (let d in data) {
            let m = data[d];
            if (m.name) {
                m.name = m.name.t();
            }
        }
        new JMenuSearch({dom: $('.menu-search')}).setData(data);
        let getMenus = function (ids) {
            let h = [];
            let colors = ['#660066', '#af26a7', '#c29425', '#e26c24', '#036173', '#a51111', '#2f6fcb', '#086f9e', '#7c1f3e'];
            for (let id of ids) {
                let m = data[id];
                if (m.url && m.name) {
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
                    h.push(`<a class="menu-item" href="${m.url}" data-id="${id}">
                        ${icon}
                        <span class="menu-text">${m.name}</span>
                    </a>`);
                } else if (m.sub) {
                    h.push(getMenus(m.sub));
                }
            }
            return h.join('');
        }
        let html = {left: [], right: []};
        let flag = true;
        for (let key of data.root) {
            let m = data[key];
            flag ? html.left.push(`<div class="menu-group">
                    <label>${m.name}</label>
                    <div class="menu-items">
                        ${getMenus(m.sub)}
                    </div>
                </div>`) : html.right.push(`<div class="menu-group">
                    <label>${m.name}</label>
                    <div class="menu-items">
                        ${getMenus(m.sub)}
                    </div>
                </div>`);
            flag = !flag;
        }
        $('.m-menus').html(`<div class="col-lg-6">${html.left.join('')}</div><div class="col-lg-6">${html.right.join('')}</div>`);
    },
    initCompany() {
        new JCompany({dom: $('[data-widget=company]')});
    },
    initUpload() {
        this.upload = new JUpload({dom: $('[data-widget=upload]')});
    },
    initUser() {
        let icon = $('.user-icon');
        let n = ((icon.html() || '').trim()[0] || '').toUpperCase();
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
    load() {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.menu',
            method: 'loadMenu',
            args: {},
            onsuccess(r) {
                me.initMenu(r.data);
                let params = jmaa.web.getParams(window.location.hash.substring(1));
                if (params.u) {
                    let uniqueName = params.m;
                    if (!uniqueName) {
                        uniqueName = btoa(params.u).replace(/=/g, '');
                    }
                    app.ws.createTab(params.t, params.u, uniqueName, true);
                }
            },
        });
    },
});
let loadApp = function () {
    window.app = jmaa.create('JPad');
    app.load();
    $('t').each(function () {
        let el = $(this);
        el.replaceWith(el.text().t());
    });
};
$(function () {
    window.isDebug = eval(jmaa.web.cookie('ctx_debug'));
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
