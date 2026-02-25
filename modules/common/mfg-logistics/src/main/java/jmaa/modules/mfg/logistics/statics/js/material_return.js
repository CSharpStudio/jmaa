//@ sourceURL=material_return.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = ['returning', 'done'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        form.setReadonly(readonly);
        if (status !== 'done') {
            // 删除放开
            form.editors.details_ids.dom.find('.delete-class').parent().show();
        }
    },
    scanCode() {
        let me = this;
        me.form.offset = 0;
        jmaa.showDialog({
            title: '生产退料单:'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'mfg.material_return_dialog',
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
                        dialog.form.dom.on('click', '.return-material', function () {
                            me.returnMaterial(dialog.form);
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
    returnMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let printFlag = data.print_flag
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'returnMaterial',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                materialId: data.material_id,
                returnedQty: data.return_qty,
                printFlag: printFlag,
                templateId: data.template_id,
                warehouseId: data.warehouse_id,
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.dialogData);
                data.result = {msg: r.data.message};
                if (printFlag) {
                    jmaa.print(r.data.data);
                }
            }
        });
    },
    submitCode(form) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
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
                form.editors.result.setValue({msg: r.data.message});
                form.setData(r.data.data);
            }
        });
    },
    deleteDetails(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "mfg.material_return_details",
            module: me.module,
            method: 'delete',
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
    createStockInOrder(e, target) {
        this.saveAndChangeStatus(target, "生成入库单", 'commit');
    },
    saveAndChangeStatus(target, title, method, required) {
        let me = this;
        let ids = target.getSelected();
        if (ids.length == 0) {
            return;
        }
        if (target.valid && !target.valid()) {
            return jmaa.msg.error(target.getErrors());
        }
        jmaa.showDialog({
            title: title.t(),
            css: 'default',
            init: function (dialog) {
                dialog.form = me.createCommentForm(dialog, required);
            },
            submit: function (dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let values = target.dataId ? target.getSubmitData() : null;
                jmaa.rpc({
                    model: me.model,
                    method,
                    dialog,
                    args: {
                        ids: ids,
                        comment: dialog.form.getData().comment,
                        values
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                        me.load();
                    }
                });
            }
        });
    },
    createCommentForm(dialog, required) {
        return dialog.body.JForm({
            cols: 1,
            fields: {
                comment: {name: 'comment', type: 'text', label: '备注', required}
            },
            arch: `<form><field name="comment"></field></form>`
        });
    },
});
