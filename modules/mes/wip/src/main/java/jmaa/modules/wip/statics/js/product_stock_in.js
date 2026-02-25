//@ sourceURL=product_storage_notice.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        //let readonly = 'draft' === status;
        //me.toolbar.dom.find('button[name=save]').attr('disabled', !readonly);
        if (['stocking', 'done'].includes(status)) {
            form.setReadonly(true);
        }
        if ('done' == status) {
            form.editors.details_ids.toolbar.dom.find('button[name=delete]').parent().hide();
            me.toolbar.dom.find('button[name=save]').attr('disabled', true);
        } else{
            form.editors.remark.setReadonly(false);
            me.toolbar.dom.find('button[name=save]').attr('disabled', false);
            form.editors.details_ids.toolbar.dom.find('button[name=delete]').parent().show();
        }
    },
    createStockIn(e, target) {
        let me = this;
        jmaa.showDialog({
            title: '生成入库单'.t(),
            css: 'default',
            init(dialog) {
                dialog.form = me.createCommentForm(dialog);
            },
            submit(dialog) {
                let values = target.dataId ? target.getSubmitData() : null;
                let comment = dialog.form.getData().comment;
                jmaa.rpc({
                    model: target.model,
                    module: me.module,
                    method: 'commit',
                    args: {
                        ids: target.getSelected(),
                        values,
                        comment,
                    },
                    onsuccess: function (r) {
                        dialog.close();
                        me.load();
                        jmaa.msg.show('操作成功'.t())
                    }
                });
            }
        });
    },
    scanCode() {
        let me = this;
        me.form.offset = 0;
        jmaa.showDialog({
            title: '成品入库通知单:'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'mfg.product_storage_notice_dialog',
                        type: 'form'
                    },
                    onsuccess: function (r) {
                        let v = r.data;
                        dialog.form = dialog.body.JForm({
                            cols: 4,
                            model: v.model,
                            module: v.module,
                            fields: v.fields,
                            arch: v.views.form.arch,
                            view: me
                        });
                        dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode === 13 && $(this).val()) {
                                me.submitCode(dialog.form);
                            }
                        }).find('input').focus();
                        dialog.form.dom.on('click', '.stock-material', function () {
                            me.stockMaterial(dialog.form);
                        }).find('button').each(function () {
                            $(this).html($(this).html().t());
                        });
                    }
                });
            },
            cancel(dialog) {
                me.form.load();
            }
        });
    },
    stockMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        if (!data.sn) {
            return jmaa.msg.error('请扫描标签'.t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                submit: true
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.sn.setValue('')
                data.result = {msg: r.data.message};
            }
        });
    },
    submitCode(form) {
        let me = this;
        let autoConfirm = form.editors.auto_confirm.getRawValue();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
                submit: autoConfirm
            },
            onerror(r) {
                form.editors.sn.setValue();
                if (r.code === 1000) {
                    form.editors.result.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                if (r.data.data){
                    form.setData(r.data.data);
                } else {
                    form.editors.qty.setValue(r.data.scan_qty)
                }
                if (autoConfirm) {
                    form.editors.sn.setValue('')
                    form.editors.auto_confirm.setValue(true)
                }
                form.editors.result.setValue({msg: r.data.message});
            }
        });
    },
    deleteDetails(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "mfg.product_storage_notice_details",
            module: me.module,
            method: 'deleteDetails',
            args: {
                ids: grid.selected,
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                me.load()
            }
        });
    },
})
