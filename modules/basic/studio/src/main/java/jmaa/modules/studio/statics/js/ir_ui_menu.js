//@ sourceURL=ir_ui_menu_studio.js
jmaa.view({
    onFormInit(e, form) {
        let me = this;
        jmaa.rpc({
            method: 'canDesign',
            model: me.model,
            module: me.module,
            onsuccess(r) {
                if (!r.data) {
                    form.toolbar.dom.find('[t-click=design],[t-click=addModel]').hide();
                }
            }
        });
    },
    onFormLoad(e, form) {
        form.toolbar.dom.find('[t-click=design]').attr('disabled', !form.getRaw().model);
    },
    design(e, target) {
        let name = target.getRaw().model;
        window.open(jmaa.web.getTenantPath() + '/view#model=dev.studio&views=custom&view=custom&name=' + name, "_blank");
    },
    createModel() {
        let me = this;
        jmaa.showDialog({
            title: '添加新功能'.t(),
            css: 'modal-sm',
            submitText: '下一步'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor name="name" type="char" label="标题" required="1"></editor>
                            <editor name="model" type="char" label="模型名称" help="推荐小写字母+下划线" required="1"></editor>
                            <editor name="parent_menu" type="many2one" label="上级菜单" readonly="1"></editor>
                        </form>`
                });
                let formData = me.form.getData();
                if (me.form.dataId && !formData.model) {
                    dialog.form.create({parent_menu: [me.form.dataId, formData.name]});
                }
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                dialog.dom.hide();
                jmaa.showDialog({
                    title: '功能选择'.t(),
                    cancelText: '上一步'.t(),
                    id: 'add-feature-dialog',
                    init(d) {
                        me.loadView('ir.model', 'custom', 'feature').then(v => {
                            let arch = jmaa.utils.parseXML(v.views.custom.arch);
                            d.body.html(arch.children('custom').html());
                        });
                    },
                    submit(d) {
                        let feature = [];
                        d.body.find('[type=checkbox]:checked').each(function () {
                            let f = $(this);
                            feature.push(f.attr('feature'));
                        });
                        let data = dialog.form.getRaw();
                        jmaa.rpc({
                            model: me.model,
                            module: me.module,
                            method: 'createModel',
                            args: {
                                name: data.name,
                                model: data.model,
                                parentMenu: data.parent_menu,
                                feature
                            },
                            onsuccess(r) {
                                d.close();
                                dialog.close();
                                me.load();
                                window.open(jmaa.web.getTenantPath() + '/view#model=dev.studio&views=custom&view=custom&name=' + data.model, "_blank");
                            }
                        });
                    },
                    cancel() {
                        dialog.dom.show();
                    }
                });
            }
        })
    }
});
