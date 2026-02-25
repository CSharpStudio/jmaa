$(function () {
    $('html,body').css("height", "100%");
    let errorCount = 0;

    window.addEventListener('error', function (event) {
        if (event.target.tagName === 'SCRIPT') {
            //addError('script加载失败:' + event.target.src);
        }
        console.log('错误:' + window.location.href, event);
    }, true);

    function openView(menu) {
        let app = $(`<div class="app">
                        <div class="name">${menu.name}</div><div class="error" style="display: none"></div>
                        <iframe style="display: block;border:0;width:100%;height:600px" src="${menu.url + "&test=1"}"></iframe>
                    </div>`);
        $('.apps').append(app);
        let w = app.find('iframe')[0].contentWindow;
        let addError = function (error) {
            app.find('.error').append(`<div>${error}</div>`).show();
            $('.error_count').html(++errorCount);
        }
        w.onerror = function (message, source, lineno, colno, error) {
            if (error) {
                addError(error.stack);
            }
            console.log('错误:' + w.location.href, message);
        }
        w.postError = function (message) {
            if (typeof message == "String") {
                addError(message);
            } else {
                addError(JSON.stringify(message));
            }
        }
        w.addEventListener('unhandledrejection', function (event) {
            addError(event.reason.stack);
            console.log('错误:' + w.location.href, event);
        });
        w.onload = function () {
            w.$('body').on('init', function () {
                if (w.env) {
                    w.env.testing = true;
                }
                setTimeout(() => {
                    let hash = w.jmaa.web.getParams(w.location.hash.substring(1));
                    if (/form/g.test(hash.views)) {
                        w.$('[name=create]').click();
                        setTimeout(() => {
                            w.$('.jui-form input:not([type=file])').addClass('testing').click().val('100');
                        }, 500);
                        setTimeout(() => {
                            w.$('form [name=create]').click();
                        }, 500);
                    }
                }, 500);
            });
        }
        setTimeout(() => {
            if (!app.find('.error').html()) {
                app.addClass('success');
                app.find('iframe').remove();
            } else {
                app.addClass('failure');
            }
        }, 8000)
    }

    function sleep(time) {
        return new Promise((resolve) => setTimeout(resolve, time));
    }

    async function test(menus) {
        let idx = 0;
        for (let m of menus) {
            openView(m);
            idx++;
            $('.run_count').html(idx);
            await sleep(500);
        }
    }

    jmaa.rpc({
        model: "ir.ui.menu",
        method: "loadMenu",
        args: {},
        onsuccess: function (r) {
            let menus = [];
            for (let d in r.data) {
                let m = r.data[d];
                if (m.url && /\/view#/g.test(m.url)) {
                    menus.push(m);
                }
            }
            let content = $(`<div class="summary"><b style="margin-right: 30px">自动冒烟测试</b><span>进度：</span><b class="run_count">0</b>/<b>${menus.length}</b><span style="margin-left: 10px">错误：</span><b class="error_count">0</b></div><div class="apps"></div>`);
            $('body').append(content);
            test(menus);
        }
    });
});
