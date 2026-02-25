//@ sourceURL=material_label.js
jmaa.view({
    onSnChange(target) {
        let me = this;
        let sn = target.getValue();
        let form = target.owner;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'getSnQty',
            args: {
                sn,
            },
            onerror(r) {
                if (r.code == 1000) {
                    form.editors.message.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
                form.editors.qty.setValue(null);
            },
            onsuccess: function (r) {
                form.editors.qty.setValue(r.data[0].qty);
            }
        });
    },
    printSplitLabel() {
        let me = this;
        let data = {print_old: true};
        let sel = me.grid.getSelected();
        if (sel.length == 1) {
            let d = me.grid.getSelectedData()[0];
            data.sn = d.sn;
            data.qty = d.qty;
        }
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${''}`,
            init(dialog) {
                let arch = `<form cols="4">
                                <editor name="sn" t-change="onSnChange" type="char" label="标签条码" required="1"></editor>
                                <editor name="qty" decimals="${data.accuracy}" type="float" label="标签数量" readonly="1"></editor>
                                <editor name="split_qty" gt="0" decimals="${data.accuracy}" type="float" label="拆分数量" required="1"></editor>
                                <editor name="print_old" type="boolean" label="打印原标签"></editor>
                                <editor name="message" rowspan="2" colspan="4" type="msg_editor" label="消息"></editor>
                            </form>`;
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    arch: arch,
                    view: me
                });
                dialog.splitForm.setData(data);
            },
            submit(dialog) {
                if (!dialog.splitForm.valid()) {
                    return;
                }
                dialog.busy(true);
                let form = dialog.splitForm;
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'printSplitLabel',
                    args: {
                        sn: form.editors.sn.getValue(),
                        splitQty: form.editors.split_qty.getValue(),
                        printOld: form.editors.print_old.getValue()
                    },
                    onerror(r) {
                        dialog.busy(false);
                        if (r.code == 1000) {
                            form.editors.message.setValue({error: true, msg: r.message});
                        } else {
                            jmaa.msg.error(r);
                        }
                    },
                    onsuccess: function (r) {
                        form.editors.split_qty.setValue(null);
                        form.editors.qty.setValue(null);
                        form.editors.message.setValue({msg: '拆分新标签:'.t() + r.data.newSn});
                        form.clearInvalid();
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                        });
                    }
                });
            },
            cancel() {
                me.load();
            }
        });
    },
    onMinPackagesChange(editor) {
        let me = this;
        let data = editor.owner.getData();
        if (data.min_packages <= 0) {
            editor.owner.setInvalid('min_packages', '标签数量不能少于0');
            return;
        }
        if (data.print_qty > 0) {
            data.label_count = Math.ceil(data.print_qty / data.min_packages);
        }
    },
    onPrintQtyChange(editor) {
        let me = this;
        let data = editor.owner.getData();
        if (data.print_qty <= 0) {
            editor.owner.setInvalid('print_qty', '打印数量必须大于0');
            return;
        }
        if (data.min_packages <= 0) {
            editor.owner.setInvalid('min_packages', '标签数量必须大于0');
            return;
        }
        data.label_count = Math.ceil(data.print_qty / data.min_packages);
    },
    reprintLabel() {
        let me = this;
        me.rpc(me.model, 'reprintLabel', {
            ids: me.getSelected()
        }).then(data => {
            jmaa.print(data);
        });
    },
    splitFilter() {
        let me = this;
        let id = me.getSelected()[0];
        return [['old_label_id', '=', id], ['new_label_id', '=', id]];
    }
});
