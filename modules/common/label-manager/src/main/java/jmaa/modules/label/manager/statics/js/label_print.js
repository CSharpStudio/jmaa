//@ sourceURL=label_print.js
jmaa.view({
    reprintLabel() {
        let me = this;
        me.rpc(me.model, 'reprintLabel', {
            ids: me.getSelected()
        }).then(data => {
            jmaa.print(data);
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
    /**
     * 生成&打印标签
     * @param data 表单数据
     */
    printLabel() {
        let me = this;
        jmaa.showDialog({
            title: '打印标签'.t(),
            init(dialog) {
                me.loadView('lbl.material_label_print_dialog', 'form').then(v => {
                    dialog.form = dialog.body.JForm({
                        arch: v.views.form.arch,
                        fields: v.fields,
                        module: v.module,
                        model: v.model,
                        view: me,
                    });
                    dialog.form.create({type: 'yw'});
                });
                dialog.dom.find('.buttons-right').html(`<button type="button" class="btn btn-flat btn-default" t-click="resetPrint">${'重置'.t()}</button>
                    <button type="button" t-click="printLabel" class="btn btn-flat btn-blue">${'打印标签'.t()}</button>`);
            },
            resetPrint(e, dialog) {
                dialog.form.create({type: 'yw'});
            },
            printLabel(e, dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                dialog.busy(true);
                let data = dialog.form.getSubmitData();
                me.rpc(me.model, 'printLabel', {
                    materialId: data.material_id,
                    printQty: data.print_qty,
                    minPackages: data.min_packages,
                    productDate: data.product_date,
                    productLot: data.product_lot,
                    lotAttr: data.lot_attr,
                    printTplId: data.template_id,
                    lpn: data.lpn,
                    data: {
                        supplier_id: data.supplier_id,
                        customer_id: data.customer_id
                    }
                }).then(data => {
                    dialog.form.getData().print_qty = null;
                    me.load();
                    jmaa.print(data, () => {
                        dialog.busy(false);
                    });
                }).finally(r => dialog.busy(false));
            },
        });
    },
});
