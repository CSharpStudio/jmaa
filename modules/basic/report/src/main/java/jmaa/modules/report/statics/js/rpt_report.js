//@ sourceURL=rpt_report.js
jmaa.view({
    onFormLoad(e, form) {
        form.editors.content.codeEditor.refresh();
    },
    onDataSetLoad(e, form) {
        let editor = form.editors.content.codeEditor;
        editor.refresh();
        editor.setOption('mode', 'sql');
    },
    updateStatus(e, target) {
        let me = this;
        let name = $(e.target).attr('name');
        me.rpc(me.model, "updateStatus", {
            ids: target.getSelected(),
            status: name == 'publish' ? "1" : "0"
        }).then(r => {
            top.app && top.app.loadMenu();
            target.load();
            jmaa.msg.show('操作成功'.t());
        });
    },
    create() {
        let me = this;
        jmaa.showDialog({
            title: '创建'.t(),
            css: 'modal-sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1"><editor name="name" label="名称" type="char" required="1"></editor></form>`
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let values = dialog.form.getSubmitData();
                me.rpc(me.model, "create", values).then(r => {
                    dialog.close();
                    me.urlHash.id = r;
                    me.changeView('custom');
                });
            }
        })
    },
    edit(e, target) {
        let me = this;
        me.urlHash.id = target.getSelected()[0];
        me.changeView('custom');
    },
    renderCustom() {
        const me = this;
        me.custom = new JCustom({
            dom: me.dom.find('.custom-panel'),
            model: me.model,
            module: me.module,
            arch: me.views.custom.arch,
            fields: me.fields,
            view: me,
            load() {
                me.custom.dataId = me.urlHash.id;
                me.dataSet.load();
                me.designer.load();
            }
        });
        me.dom.find('t').each(function () {
            let tag = $(this);
            let text = tag.html().t();
            tag.replaceWith(text);
        });
        if (!me.backView) {
            me.backView = me.urlHash.back;
        }
        me.dataSet = jmaa.create('report.dataset', {dom: me.custom.dom.find('.left-aside'), view: me});
        me.designer = jmaa.create('report.designer', {dom: me.custom.dom.find('.r-body'), view: me});
        me.memo = jmaa.create("CommandMemo", {
            change() {
                me.custom.dom.find('.btn-redo').attr('disabled', !me.memo.canRedo());
                me.custom.dom.find('.btn-undo').attr('disabled', !me.memo.canUndo());
            }
        });
        me.custom.dom.on('click', '.btn-back', function () {
            if (me.dirty) {
                jmaa.msg.confirm({
                    title: '提示'.t(),
                    content: '数据未保存，是否继续？'.t(),
                    submit() {
                        delete me.urlHash.readonly;
                        me.changeView(me.backView);
                    }
                })
            } else {
                delete me.urlHash.readonly;
                me.changeView(me.backView);
            }
        }).on('click', '.btn-source', function () {
            me.editSource();
        }).on('click', '.btn-redo', function () {
            me.memo.redo();
        }).on('click', '.btn-undo', function () {
            me.memo.undo();
        }).on('click', '.btn-reload', function () {
            me.custom.load();
        }).on('click', '.r-title', function () {
            jmaa.showDialog({
                title: '编辑'.t(),
                css: 'modal-sm',
                init(dialog) {
                    dialog.form = dialog.body.JForm({
                        arch: `<form cols="1">
                            <editor type="char" name="name" label="名称" required="1"></editor>
                        </form>`
                    });
                    dialog.form.setData({name: me.dom.find('.r-title .rpt-name').html()});
                },
                submit(dialog) {
                    if (!dialog.form.valid()) {
                        return jmaa.msg.error(dialog.form.getErrors());
                    }
                    let name = dialog.form.getData().name;
                    me.saveReport({name}, function () {
                        dialog.close();
                        jmaa.msg.show('操作成功'.t());
                        me.dom.find('.r-title .rpt-name').html(name);
                    });
                }
            });
        });
    },
    editSource() {
        let me = this;
        jmaa.showDialog({
            title: '编辑源码'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form>
                        <editor type="code-editor" rows="20" mode="xml" colspan="4" name="content" nolabel="1" required="1"></editor>
                    </form>`
                });
                dialog.form.setData({content: me.designer.arch});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                me.saveReport(data, function () {
                    dialog.close();
                    jmaa.msg.show('操作成功'.t());
                    me.custom.load();
                });
            }
        })
    },
    saveReport(values, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: 'report',
            method: 'update',
            args: {
                ids: [me.custom.dataId],
                values: values,
            },
            onsuccess() {
                callback();
            }
        });
    },
});
