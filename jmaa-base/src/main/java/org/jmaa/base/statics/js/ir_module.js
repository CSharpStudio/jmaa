//@ sourceURL=ir_module.js
jmaa.view({
    install: function (e, id, target, view) {
        this.run({ids: [id], label: '应用安装中', method: 'install', showLog: true});
    },
    uninstall: function (e, id, target, view) {
        var me = this;
        jmaa.showDialog({
            title: '确认卸载'.t(),
            init(d) {
                d.body.html('正在分析应用依赖...'.t());
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'getDependOn',
                    args: {
                        ids: [id]
                    },
                    onsuccess: function (r) {
                        let html = "<div>确认卸载以下应用？</div>";
                        for (let m of r.data) {
                            html += `<div class="mt-2">${m.label}(${m.name})</div>`
                        }
                        d.body.html(`<div class="m-3">${html}</div>`);
                    }
                });
            },
            submit(d) {
                me.run({ids: [id], label: '应用卸载中', method: 'uninstall'});
            }
        });
    },
    upgrade: function () {
        let arg = arguments[1];
        let id = typeof arg == 'string' ? arg : arg.getSelected()[0];
        this.run({ids: [id], label: '应用升级中', method: 'upgrade', showLog: true});
    },
    run: function (opt) {
        var me = this;
        jmaa.mask(opt.label.t());
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: opt.method,
            args: {
                ids: opt.ids
            },
            onerror: function (e) {
                jmaa.mask();
                jmaa.msg.error(e);
            },
            onsuccess: function (r) {
                if (r.data.action === 'js') {
                    eval(r.data.script);
                }
            }
        });
    },
    showLog() {
        let me = this;
        jmaa.rpc({
            model: 'ir.module.log',
            module: me.module,
            method: 'search',
            args: {
                criteria: [['state', '=', true]],
                fields: ['name', 'content', 'log_time', 'level'],
            },
            onsuccess: function (r) {
                top.window.jmaa.showDialog({
                    title: '更新日志',
                    init(d) {
                        d.body.css('padding', '1rem');
                        let html = `<table class="table table-bordered"><thead><tr><th>标题</th><th>内容</th><th>时间</th></tr></thead><tbody>`;
                        for (let v of r.data.values) {
                            html += `<tr><td>${v.name}</td><td style="white-space: pre-line" ${v.level == 'error' ? ' class="text-danger"' : ''}>${v.content}</td><td>${v.log_time}</td></tr>`;
                        }
                        html += `</tbody></table>`;
                        d.body.html(html);
                    },
                    cancel() {
                        top.window.location.reload();
                    }
                });
            }
        });
    }
});
