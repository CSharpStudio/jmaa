//@ sourceURL=report.dataset.js
jmaa.component('report.dataset', {
    init() {
        let me = this;
        me.dom.on('click', '.btn-ds-add', function () {
            me.editDataSet();
        }).on('click', '.btn-ds-edit', function () {
            let id = $(this).closest('[data-id]').attr('data-id');
            me.editDataSet(id);
        }).on('click', '.btn-ds-remove', function () {
            let id = $(this).closest('[data-id]').attr('data-id');
            me.deleteDataSet(id);
        });
    },
    load() {
        let me = this;
        me.view.rpc(me.view.model, 'searchByField', {
            relatedField: 'dataset_ids',
            criteria: [['report_id', '=', me.view.custom.dataId]],
            limit: 1000,
            fields: ['code', 'name'],
            order: 'code',
        }).then(data => {
            me.data = data.values;
            let html = [];
            for (let row of me.data) {
                html.push(`<div class="ds-list-item" data-id="${row.id}">
                    <i class="fa fa-database mr-1 text-info"></i>
                    <span>${row.name}</span>
                    <div class="ds-list-commands">
                        <button class="btn btn-icon btn-flat btn-ds-edit"><i class="fa fa-pen"></i></button>
                        <button class="btn btn-icon btn-flat btn-ds-remove"><i class="fa fa-times"></i></button>
                    </div>
                </div>`)
            }
            me.dom.find('.ds-list').html(html.join(''));
        });
    },
    deleteDataSet(id) {
        let me = this;
        jmaa.msg.confirm({
            title: '确认'.t(),
            content: '确认删除?'.t(),
            submit() {
                me.save({dataset_ids: [[2, id]]}, function () {
                    jmaa.msg.show('操作成功'.t());
                    me.load();
                });
            }
        })
    },
    editDataSet(id) {
        let me = this;
        jmaa.showDialog({
            title: '添加数据源'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form>
                            <editor type="char" name="code" label="编码" readonly="1" placeholder="自动生成"></editor>
                            <editor type="char" name="name" label="名称" required="1"></editor>
                            <editor type="code-editor" rows="10" mode="sql" colspan="4" name="content" label="SQL" required="1"></editor>
                            <div class="toolbar grid-colspan-4">
                                <button type="button" t-click="execute" class="btn btn-flat btn-primary">${'执行'.t()}</button>
                            </div>
                            <div class="ds-result grid-colspan-4"></div>
                        </form>`,
                    ajax(form, callback) {
                        me.view.rpc(me.view.model, 'searchByField', {
                            relatedField: 'dataset_ids',
                            criteria: [['id', '=', id]],
                            limit: 1000,
                            fields: ['code', 'name', 'content'],
                        }, {
                            usePresent: true,
                        }).then(data => {
                            callback({data: data.values[0]});
                        });
                    },
                });
                id && dialog.form.load();
            },
            execute(e, dialog) {
                let code = dialog.form.getData().content;
                dialog.body.find('.ds-result').html('');
                me.executeSql(code, function (data) {
                    me.renderResult(dialog.body.find('.ds-result'), data);
                })
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                let values = id ? [[1, id, data]] : [[0, 0, data]];
                me.save({dataset_ids: values}, function () {
                    dialog.close();
                    jmaa.msg.show('操作成功'.t());
                    me.view.custom.load();
                });
            }
        })
    },
    save(values, callback) {
        let me = this;
        jmaa.rpc({
            model: me.view.model,
            module: 'report',
            method: 'update',
            args: {
                ids: [me.view.custom.dataId],
                values,
            },
            onsuccess() {
                callback();
            }
        });
    },
    executeSql(code, callback) {
        let me = this;
        if (!code || !code.trim()) {
            return callback({debug: 'SQL不能为空', error: 'SQL不能为空'});
        }
        let regex = /#\{([^[\}]+)/g;
        let keys = {};
        let params = [];
        let match;
        while ((match = regex.exec(code)) !== null) {
            let name = match[1].trim();
            if (!keys[name]) {
                params.push(`<editor name="${name}" type="char" label="${name}"></editor>`);
                keys[name] = !0;
            }
        }
        let execute = function (data) {
            jmaa.rpc({
                model: me.view.model,
                module: me.view.module,
                method: 'executeSql',
                args: {
                    code,
                    params: data,
                },
                onsuccess(r) {
                    callback(r.data);
                }
            });
        }
        if (params.length) {
            jmaa.showDialog({
                id: 'params-dialog',
                title: '参数'.t(),
                css: 'modal-sm',
                init(d) {
                    d.form = d.body.JForm({
                        arch: `<form cols="1">${params.join('')}</form>`
                    });
                },
                submit(d) {
                    let data = d.form.getSubmitData();
                    execute(data);
                    d.close();
                }
            })
        } else {
            execute();
        }
    },
    renderResult(dom, data) {
        let me = this;
        if (data.debug) {
            dom.html(`<span class="text-danger">${data.error}</span>`);
            console.error(data.debug);
        } else {
            let head = [];
            let body = [];
            for (let column of data.columns) {
                head.push(`<th>${column[0]}</th>`)
            }
            for (let row of data.values) {
                let h = [];
                for (let column of data.columns) {
                    h.push(`<td>${row[column[0]]}</td>`)
                }
                body.push(`<tr>${h.join('')}</tr>`)
            }
            dom.html(`<table class="table table-bordered dataTable">
                <thead><tr>${head.join('')}</tr></thead><tbody>${body.join('')}</tbody>
            </table>`);
        }
    },
});
