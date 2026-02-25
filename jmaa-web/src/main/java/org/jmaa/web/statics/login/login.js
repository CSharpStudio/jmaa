import './animation.js';

$(function () {
    //使用jwt令牌登录
    let login = jmaa.web.getUrlParam('app');
    let secret = jmaa.web.getUrlParam('jwt');
    if (login && secret) {
        $('.btn-login').attr('disabled', false);
        jmaa.rpc({
            url: jmaa.web.getTenantPath() + '/jwt',
            data: {
                method: 'jwt',
                params: {
                    login,
                    secret,
                },
            },
            success: function (rs) {
                $('.btn-login').attr('disabled', false);
                if (rs.error) {
                    console.log(rs.error);
                    $('.login-error').html(rs.error.message);
                } else {
                    jmaa.web.cookie('ctx_token', rs.result);
                    let url = jmaa.web.getUrlParam('url') || jmaa.web.getTenantPath();
                    window.location.href = url + window.location.hash;
                }
            },
            error: function (rs) {
                $('.btn-login').attr('disabled', false);
                console.log(rs);
                alert('发生错误：'.t() + rs.responseText);
            },
        });
    }

    //处理文本翻译
    $('[placeholder]').each(function () {
        let el = $(this);
        el.attr('placeholder', el.attr('placeholder').t());
    });
    $('t').each(function () {
        let el = $(this);
        el.replaceWith(el.text().t());
    });

    function require(value, selector, error) {
        if (!value) {
            $(selector).addClass('is-invalid').focus().closest('.form-group').append(`<span class="invalid-feedback d-block">${error}</span>`);
            return false;
        }
        return true;
    }

    function clean() {
        $('.is-invalid').removeClass('is-invalid');
        $('.invalid-feedback').remove();
        $('.login-error').html('');
    }

    $('body').on('click', '.btn-login', function () {
        //登录按钮事件
        clean();
        let login = $('.login-account').val();
        let pwd = $('.login-password').val();
        let valid = require(pwd, '.login-password', '请输入密码'.t()) &&
            require(login, '.login-account', '请输入账号/手机号/邮箱'.t());
        if (!valid) {
            return;
        }
        $('.btn-login').attr('disabled', true);
        let remember = $('.login-remember').is(':checked');
        let force = $('.login-force').is(':checked');
        jmaa.rpc({
            url: jmaa.web.getTenantPath() + '/rpc/login',
            data: {
                method: 'login',
                params: {
                    login,
                    password: window.btoa(unescape(encodeURIComponent(pwd))),
                    remember,
                    force,
                },
            },
            success(rs) {
                $('.btn-login').attr('disabled', false);
                if (rs.error) {
                    //7104修改默认密码，7106密码已过期
                    if (rs.error.code === 7104 || rs.error.code === 7106) {
                        $('.reset-account').val(login);
                        $('.login-form').hide();
                        $('.reset-form').show();
                        $('.reset-password').focus();
                        if (rs.error.code === 7106) {
                            $('.login-error').html(rs.error.message);
                        }
                        return;
                    }
                    console.log(rs.error);
                    $('.login-error').html(rs.error.message);
                    $('.login-account').focus();
                    if (rs.error.code === 7201) {
                        $('.login-force').parent().show();
                    } else {
                        $('.login-password').val('');
                    }
                } else {
                    $('.login-password').val('');
                    let r = rs.result;
                    let opt = remember ? {expires: 7} : {};
                    jmaa.web.cookie('ctx_token', r.token, opt);
                    jmaa.web.cookie('ctx_lang', r.lang[0], opt);
                    localStorage.setItem('user_theme', r.theme);
                    localStorage.setItem('user_info', JSON.stringify({
                        id: r.id,
                        login: r.login,
                        name: r.name,
                        tz: r.tz,
                        lang: r.lang,
                        image: r.image
                    }));
                    if (r.company && r.company.length) {
                        jmaa.web.cookie('ctx_company', r.company[0], opt);
                        localStorage.setItem('company_info', JSON.stringify({id: r.company[0], name: r.company[1]}));
                    }
                    let url = jmaa.web.getUrlParam('url') || jmaa.web.getTenantPath();
                    window.location.replace(url + window.location.hash);
                }
            },
            error(rs) {
                $('.btn-login').attr('disabled', false);
                console.log(rs);
                alert('发生错误：'.t() + rs.responseText);
            }
        });
    }).on('click', '.btn-reset', function () {
        //修改密码按钮事件
        clean();
        let login = $('.reset-account').val();
        let oldPwd = $('.login-password').val();
        let newPwd = $('.reset-password').val();
        let confirmPwd = $('.reset-confirm').val();
        let valid = require(newPwd, '.reset-password', '请输入新密码'.t()) &&
            require(confirmPwd, '.reset-confirm', '请输入确认密码'.t()) &&
            require(login, '.reset-account', '请输入账号/手机号/邮箱'.t());
        if (!valid) {
            return;
        }
        if (confirmPwd !== newPwd) {
            $('.reset-confirm').addClass('is-invalid').focus().closest('.form-group')
                .append(`<span class="invalid-feedback d-block">${'与新密码不一致'.t()}</span>`);
            return;
        }
        $('.btn-reset').attr('disabled', true);
        $('.reset-password, .reset-confirm').val('');
        jmaa.rpc({
            url: jmaa.web.getTenantPath() + '/rpc/loginChangePassword',
            data: {
                id: Date.now(),
                jsonrpc: '2.0',
                method: 'loginChangePassword',
                params: {
                    login,
                    oldPassword: window.btoa(unescape(encodeURIComponent(oldPwd))),
                    newPassword: window.btoa(unescape(encodeURIComponent(newPwd))),
                },
            },
            success: function (rs) {
                $('.btn-reset').attr('disabled', false);
                if (rs.error) {
                    console.log(rs.error);
                    $('.login-error').html(rs.error.message);
                    $('.reset-password').focus();
                } else {
                    $('.reset-form').hide();
                    $('.login-form').show();
                    $('.login-password').val('').focus();
                }
            },
            error(rs) {
                $('.btn-reset').attr('disabled', false);
                console.log(rs);
                alert('发生错误：'.t() + rs.responseText);
            }
        });
    }).on('click', '.clear-cache', function () {
        //清理浏览器缓存
        window.localStorage.clear();
        window.sessionStorage.clear();
        // 清除cookie
        jmaa.web.cookie('ctx_lang', '', {expires: -1});
        jmaa.web.cookie('ctx_token', '', {expires: -1});
        jmaa.web.cookie('ctx_company', '', {expires: -1});
        jmaa.web.cookie('ctx_company_ids', '', {expires: -1});
        // 刷新页面
        window.location.reload();
    }).on('click', '.back-to-login', function () {
        //修改密码返回登录按钮事件
        $('.reset-account,.reset-password,.reset-confirm').val('');
        $('.login-error').html('');
        $('.login-form').show();
        $('.reset-form').hide();
    }).on('click', '.link-page .back', function () {
        $('.link-page').remove();
    }).on('click', '.footer-items a', function (e) {
        let a = $(this);
        $('body').append(`<div class="link-page">
                <div class="p-header">
                    <a class="back">${'返回'.t()}</a>
                    <div class="title">${a.text()}</div>
                </div>
                <div class="p-body">
                    <iframe src="${a.attr('href')}"/>
                </div>
            </div>`);
        return false;
    });

    //模拟tab移动焦点
    let tabIdx = 0;
    $('.form-control, .btn').each(function () {
        $(this).attr('tab-idx', tabIdx++);
    });
    $('.form-control').keypress(function (e) {
        if (e.which == 13) {
            if ($(this).hasClass('login-password')) {
                let login = $('.login-account').val();
                let pwd = $('.login-password').val();
                if (login && pwd) {
                    $('.btn-login').click();
                }
            }
            $(`[tab-idx=${Number($(this).attr("tab-idx")) + 1}]`).focus();
            e.preventDefault();
        }
    });

    $('.login-account').focus();
});
