//@ sourceURL=board_designer.js
jmaa.view({
    copy(e, target) {
        let me = this;
        let id = target.getSelected()[0];
        jmaa.showDialog({
            title: '创建看板',
            css: 'modal-sm',
            init(dialog) {
                me.rpc(me.model, "read", {ids: [id], fields: ['title', 'content']}).then(r => {
                    dialog.form = dialog.body.JForm({
                        arch: `<form cols="1">
                            <editor name="name" type="char" label="名称" required="1" length="16"></editor>
                        </form>`
                    });
                    dialog.form.setData({name: r[0].title + " - 复制".t()});
                    dialog.content = r[0].content;
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let name = dialog.form.getData().name;
                let content = JSON.parse(dialog.content || {});
                if (!content.editCanvasConfig) {
                    content = {"editCanvasConfig": {"projectName": name}};
                } else {
                    content.editCanvasConfig.projectName = dialog.form.getData().name;
                }
                localStorage.setItem("GO_LOGIN_TOKEN_STORE", jmaa.web.cookie('ctx_token'));
                sessionStorage.removeItem('GO_CHART_STORAGE_CANVAS_ID');
                sessionStorage.setItem('GO_CHART_STORAGE_LIST', JSON.stringify([content]));
                window.open(jmaa.web.getTenantPath() + "/board/#/chart/home/");
                dialog.close();
            }
        });
    },
    create() {
        jmaa.showDialog({
            title: '创建看板',
            css: 'modal-sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor name="name" type="char" label="名称" required="1" length="16"></editor>
                        </form>`
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let name = dialog.form.getData().name;
                localStorage.setItem("GO_LOGIN_TOKEN_STORE", jmaa.web.cookie('ctx_token'));
                sessionStorage.removeItem('GO_CHART_STORAGE_CANVAS_ID');
                sessionStorage.setItem('GO_CHART_STORAGE_LIST', JSON.stringify([{"editCanvasConfig": {"projectName": name}}]));
                window.open(jmaa.web.getTenantPath() + "/board/#/chart/home/");
                dialog.close();
            }
        });
    },
    createFromTemplate(e, target) {
        let me = this;
        jmaa.showDialog({
            title: '从模板创建'.t(),
            init(dialog) {
                me.initTemplateDialog(dialog);
            },
            submit(dialog) {
                let selected = dialog.card.getSelected();
                if (!selected.length) {
                    return jmaa.msg.error('请选择模板'.t());
                }
                let id = selected[0];
                me.rpc(me.model, "readTemplate", {
                    templateId: id,
                    fields: ['content'],
                }).then(r => {
                    localStorage.setItem("GO_LOGIN_TOKEN_STORE", jmaa.web.cookie('ctx_token'));
                    sessionStorage.removeItem('GO_CHART_STORAGE_CANVAS_ID');
                    sessionStorage.setItem('GO_CHART_STORAGE_LIST', "[" + r[0].content + "]");
                    window.open(jmaa.web.getTenantPath() + "/board/#/chart/home/" + id);
                    dialog.close();
                });
            }
        })
    },
    initTemplateDialog(dialog) {
        let me = this;
        me.loadView('board.template', 'card').then(v => {
            dialog.body.html(`<div class="border-bottom btn-row m-0 w-100 p-2">
                        <div class="toolbar">
                            <div class="input-group">
                                <input type="text" id="keyword" class="form-control"/>
                                <div class="input-suffix">
                                    <button type="button" class="btn btn-icon search-template">
                                        <i class="fa fa-search"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                        <div class="template-pager"></div>
                    </div>
                    <div class="template-card">
                    </div>`);
            dialog.pager = new JPager({
                dom: dialog.body.find('.template-pager'),
                pageChange: function (e, pager) {
                    dialog.card.load();
                },
                counting: function (e, pager) {
                    me.rpc(me.model, "countTemplate", {
                        keyword: dialog.body.find('#keyword').val(),
                    }).then(r => {
                        pager.update({
                            total: r
                        });
                    });
                }
            });
            dialog.card = dialog.body.find('.template-card').JCard({
                model: v.model,
                arch: v.views.card.arch,
                fields: v.fields,
                ajax(card, callback) {
                    me.rpc(me.model, "searchTemplate", {
                        fields: card.getFields(),
                        limit: dialog.pager.getLimit(),
                        offset: dialog.pager.getOffset(),
                        keyword: dialog.body.find('#keyword').val(),
                    }, {
                        usePresent: true,
                    }).then(r => {
                        callback({data: r.values});
                        if (r.values.length > 0) {
                            dialog.pager.update(r);
                        } else {
                            dialog.pager.noData();
                        }
                    });
                },
            });
            dialog.body.on('click', '.search-template', function () {
                dialog.card.load();
            }).on('click', '.template-preview', function () {
                let id = $(this).closest('.card').attr('data');
                window.open(jmaa.web.getTenantPath() + "/board/#/chart/preview/$" + id);
            }).on('click', '.template-delete', function () {
                let id = $(this).closest('.card').attr('data');
                jmaa.msg.confirm({
                    title: '确认删除'.t(),
                    content: '确认删除模板?'.t(),
                    submit() {
                        me.rpc(me.model, "deleteTemplate", {
                            templateId: id,
                        }).then(r => {
                            dialog.card.load();
                            jmaa.msg.show('操作成功'.t());
                        });
                    }
                })
            });
        });
    },
    edit(e, target) {
        let me = this;
        let id = target.getSelected()[0];
        localStorage.setItem("GO_LOGIN_TOKEN_STORE", jmaa.web.cookie('ctx_token'));
        sessionStorage.setItem('GO_CHART_STORAGE_CANVAS_ID', id);
        me.rpc(me.model, "read", {ids: [id], fields: ['content']}).then(r => {
            sessionStorage.setItem('GO_CHART_STORAGE_LIST', "[" + r[0].content + "]");
            window.open(jmaa.web.getTenantPath() + "/board/#/chart/home/" + id);
        });
    },
    updateState(e, target) {
        let me = this;
        let name = $(e.target).attr('name');
        me.rpc(me.model, "updateState", {
            ids: target.getSelected(),
            state: name == 'publish' ? "1" : "0"
        }).then(r => {
            top.app && top.app.loadMenu();
            target.load();
            jmaa.msg.show('操作成功'.t());
        });
    },
    preview(e, target) {
        let me = this;
        let id = target.getSelected()[0];
        me.rpc(me.model, "read", {ids: [id], fields: ['content']}).then(r => {
            window.open(jmaa.web.getTenantPath() + "/board/#/chart/preview/" + id);
        });
    },
    saveAsTemplate(e, target) {
        let me = this;
        jmaa.showDialog({
            title: '创建看板',
            css: 'modal-m',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor name="name" type="char" label="模板名称" required="1" length="16"></editor>
                            <editor name="remark" type="char" label="模板说明" required="1"></editor>
                        </form>`
                });
                dialog.form.setData({name: target.getSelectedData()[0].title});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getData();
                me.rpc(me.model, "saveAsTemplate", {
                    ids: target.getSelected(),
                    name: data.name,
                    remark: data.remark
                }).then(r => {
                    dialog.close();
                    jmaa.msg.show('操作成功'.t());
                });
            }
        });
    }
});
