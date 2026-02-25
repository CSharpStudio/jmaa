//@ sourceURL=batch_code.js
jmaa.view({
    reprint(e, target) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'reprint',
            args: {
                ids: target.getSelected(),
            },
            onsuccess(r) {
                jmaa.print(r.data);
            }
        });
    },
    print() {
        let me = this;
        jmaa.showDialog({
            title: '打印'.t(),
            init(dialog) {
                me.loadView("wip.batch_code_print_dialog", "form").then(v => {
                    dialog.form = dialog.body.JForm({
                        model: v.model,
                        module: v.module,
                        arch: v.views.form.arch,
                        fields: v.fields,
                        view: me
                    });
                    dialog.form.create();
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'print',
                    dialog,
                    args: {
                        workOrderId: data.work_order_id,
                        printQty: data.print_qty,
                        templateId: data.template_id,
                        codingId: data.coding_id,
                        batchQty: data.batch_qty,
                        supplierId: data.supplier_id,
                        productDate: data.product_date,
                    },
                    onsuccess(r) {
                        dialog.close();
                        let printData = r.data.printData;
                        jmaa.print(printData);
                        me.load();
                    }
                });
            }
        })
    },
});
