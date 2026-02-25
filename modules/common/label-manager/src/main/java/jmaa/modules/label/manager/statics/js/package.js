//@ sourceURL=package.js
jmaa.view({
    print() {
        let me = this;
        me.rpc(me.model, 'print', {
            ids: me.getSelected()
        }).then(data => {
            jmaa.print(data);
        });
    },
    splitLabel() {
        let me = this;
        let data = {};
        let sel = me.grid.getSelected();
        if (sel.length > 1) {
            return jmaa.msg.error("请选择一条数据操作".t());
        }
        let d = me.grid.getSelectedData()[0];
        if (d.stock_rule !== 'sn') {
            return jmaa.msg.error("序列号管控规则标签才支持拆箱".t());
        }
        if (d.category !== 'finished') {
            return jmaa.msg.error("成品物料才支持拆箱".t());
        }
        data.code = d.code;
        data.qty = d.qty;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${''}`,
            init(dialog) {
                let arch = `<form cols="4">
                                 <editor name="code" type="char" label="包装条码" readonly="1"></editor>
                                <editor name="qty" decimals="${data.accuracy}" type="float" label="包装数量" readonly="1"></editor>
                                <editor name="sn" type="char" label="标签条码" required="1"></editor>
                                <editor name="message" rowspan="2" colspan="4" type="msg_editor" label="消息"></editor>
                            </form>`;
                dialog.form = dialog.body.JForm({
                    model: me.model,
                    arch: arch,
                    view: me
                });
                dialog.form.setData(data);
                dialog.dom.find('.buttons-right').html(`<button type="button" class="btn btn-blue btn-confirm">${'确认'.t()}</button>
                    <button type="button" class="btn btn-blue btn-confirm-close">${'确认并关闭'.t()}</button>`);
                dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                    if (e.keyCode == 13 && $(this).val()) {
                        if (!dialog.form.valid()) {
                            jmaa.msg.error(dialog.form.getErrors());
                            return;
                        }
                        let form = dialog.form;
                        jmaa.rpc({
                            model: me.model,
                            module: me.module,
                            method: 'splitLabel',
                            args: {
                                code: form.editors.code.getValue(),
                                sn: form.editors.sn.getValue(),
                            },
                            onerror(r) {
                                if (r.code == 1000) {
                                    form.editors.message.setValue({error: true, msg: r.message});
                                } else {
                                    jmaa.msg.error(r);
                                }
                            },
                            onsuccess: function (r) {
                                form.editors.sn.setValue(null);
                                form.editors.qty.setValue(form.editors.qty.getValue() - r.data.qty);
                                form.editors.message.setValue({msg: '拆分标签:'.t() + r.data.message});
                                jmaa.msg.show('操作成功'.t());
                            }
                        });
                    }
                }).find('input').focus();
                dialog.dom.on('click', '.btn-confirm', function () {
                    if (!dialog.form.valid()) {
                        jmaa.msg.error(dialog.form.getErrors());
                        return;
                    }
                    let form = dialog.form;
                    jmaa.rpc({
                        model: me.model,
                        module: me.module,
                        method: 'splitLabel',
                        args: {
                            code: form.editors.code.getValue(),
                            sn: form.editors.sn.getValue(),
                        },
                        onerror(r) {
                            if (r.code == 1000) {
                                form.editors.message.setValue({error: true, msg: r.message});
                            } else {
                                jmaa.msg.error(r);
                            }
                        },
                        onsuccess: function (r) {
                            form.editors.sn.setValue(null);
                            form.editors.qty.setValue(form.editors.qty.getValue() - r.data.qty);
                            form.editors.message.setValue({msg: '拆分标签:'.t() + r.data.message});
                            jmaa.msg.show('操作成功'.t());
                        }
                    });
                }).on('click', '.btn-confirm-close', function () {
                    if (!dialog.form.valid()) {
                        jmaa.msg.error(dialog.form.getErrors());
                        return;
                    }
                    let form = dialog.form;
                    jmaa.rpc({
                        model: me.model,
                        module: me.module,
                        method: 'splitLabel',
                        args: {
                            code: form.editors.code.getValue(),
                            sn: form.editors.sn.getValue(),
                        },
                        onerror(r) {
                            dialog.busy(false);
                            jmaa.msg.error(r);
                        },
                        onsuccess: function (r) {
                            form.editors.sn.setValue(null);
                            jmaa.msg.show('操作成功'.t());
                            dialog.close();
                            me.load()
                        }
                    });
                });
            },
            cancel() {
                me.load();
            }
        });
    },
    mergeLabel() {
        let me = this;
        let data = {};
        let sel = me.grid.getSelected();
        if (sel.length > 1) {
            return jmaa.msg.error("请选择一条数据操作".t());
        }
        let d = me.grid.getSelectedData()[0];
        if (d.stock_rule !== 'sn') {
            return jmaa.msg.error("序列号管控规则标签才支持合箱".t());
        }
        if (d.category !== 'finished') {
            return jmaa.msg.error("成品物料才支持合箱".t());
        }
        data.code = d.code;
        data.qty = d.qty;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `合并标签：${''}`,
            init(dialog) {
                let arch = `<form cols="4">
                                 <editor name="code" type="char" label="包装条码" readonly="1"></editor>
                                <editor name="qty" decimals="${data.accuracy}" type="float" label="包装数量" readonly="1"></editor>
                                <editor name="sn" type="char" label="标签条码" required="1"></editor>
                                <editor name="message" rowspan="2" colspan="4" type="msg_editor" label="消息"></editor>
                            </form>`;
                dialog.form = dialog.body.JForm({
                    model: me.model,
                    arch: arch,
                    view: me
                });
                dialog.form.setData(data);
                dialog.dom.find('.buttons-right').html(`<button type="button" class="btn btn-blue btn-confirm">${'确认'.t()}</button>
                    <button type="button" class="btn btn-blue btn-confirm-close">${'确认并关闭'.t()}</button>`);
                dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                    if (e.keyCode == 13 && $(this).val()) {
                        if (!dialog.form.valid()) {
                            jmaa.msg.error(dialog.form.getErrors());
                            return;
                        }
                        let form = dialog.form;
                        jmaa.rpc({
                            model: me.model,
                            module: me.module,
                            method: 'mergeLabel',
                            args: {
                                code: form.editors.code.getValue(),
                                sn: form.editors.sn.getValue(),
                            },
                            onerror(r) {
                                if (r.code == 1000) {
                                    form.editors.message.setValue({error: true, msg: r.message});
                                } else {
                                    jmaa.msg.error(r);
                                }
                            },
                            onsuccess: function (r) {
                                form.editors.sn.setValue(null);
                                form.editors.qty.setValue(form.editors.qty.getValue() + r.data.qty);
                                form.editors.message.setValue({msg: '合并标签:'.t() + r.data.message});
                                jmaa.msg.show('操作成功'.t());
                            }
                        });
                    }
                }).find('input').focus();
                dialog.dom.on('click', '.btn-confirm', function () {
                    if (!dialog.form.valid()) {
                        jmaa.msg.error(dialog.form.getErrors());
                        return;
                    }
                    let form = dialog.form;
                    jmaa.rpc({
                        model: me.model,
                        module: me.module,
                        method: 'mergeLabel',
                        args: {
                            code: form.editors.code.getValue(),
                            sn: form.editors.sn.getValue(),
                        },
                        onerror(r) {
                            if (r.code == 1000) {
                                form.editors.message.setValue({error: true, msg: r.message});
                            } else {
                                jmaa.msg.error(r);
                            }
                        },
                        onsuccess: function (r) {
                            form.editors.sn.setValue(null);
                            form.editors.qty.setValue(form.editors.qty.getValue() + r.data.qty);
                            form.editors.message.setValue({msg: '合并标签:'.t() + r.data.message});
                            jmaa.msg.show('操作成功'.t());
                        }
                    });
                }).on('click', '.btn-confirm-close', function () {
                    if (!dialog.form.valid()) {
                        jmaa.msg.error(dialog.form.getErrors());
                        return;
                    }
                    let form = dialog.form;
                    jmaa.rpc({
                        model: me.model,
                        module: me.module,
                        method: 'mergeLabel',
                        args: {
                            code: form.editors.code.getValue(),
                            sn: form.editors.sn.getValue(),
                        },
                        onerror(r) {
                            jmaa.msg.error(r);
                        },
                        onsuccess: function (r) {
                            form.editors.sn.setValue(null);
                            jmaa.msg.show('操作成功'.t());
                            dialog.close();
                            me.load()

                        }
                    });
                });
            },
            cancel() {
                me.load();
            }
        });
    }
});
