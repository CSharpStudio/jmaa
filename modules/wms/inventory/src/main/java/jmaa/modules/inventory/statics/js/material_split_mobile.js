//@ sourceURL=material_split_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.changePage("details");
        me.materialLabelForm.setData({})
        me.packageLabelForm.setData({});
        me.materialLabelForm.editors.print_old.setValue(false);

    },
    scanMaterialCode() {
        let me = this;
        let form = me.materialLabelForm;
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanMaterialCode',
            args: {
                code: form.editors.code.getValue()
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.qty.setValue(r.data.qty)
                data.message = {msg: r.data.message};
            }
        });
    },
    materialSplitConfirm() {
        let me = this;
        let form = me.materialLabelForm;
        let data = form.getRaw();

        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'materialSplitConfirm',
            args: {
                code: form.editors.code.getValue(),
                splitQty: form.editors.split_qty.getValue(),
                printOld:form.editors.print_old.getValue(),
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.code.setValue(null)
                data.message = {msg: r.data.message};
                jmaa.print(r.data, () => {
                });
            }
        });

    },
    scanPackageCode() {
        let me = this;
        let form = me.packageLabelForm;
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanPackageCode',
            args: {
                code: form.editors.code.getValue(),
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.qty.setValue(r.data.qty)
                data.message = {msg: r.data.message};
            }
        });
    },
    scanPackageSn() {
        let me = this;
        let form = me.packageLabelForm;
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanPackageSn',
            args: {
                sn: form.editors.sn.getValue(),
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.label_qty.setValue(r.data.label_qty)
                data.message = {msg: r.data.message};

            }
        });
    },
    packageSplit() {
        let me = this;
        let form = me.packageLabelForm;
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'packageSplit',
            args: {
                code: form.editors.code.getValue(),
                sn: form.editors.sn.getValue(),
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.qty.setValue(form.editors.qty.getValue() - r.data.qty)
                data.message = {msg: r.data.message};
                form.editors.sn.setValue('');
            }
        });
    },
    packageMerge() {
        let me = this;
        let form = me.packageLabelForm;
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'packageMerge',
            args: {
                code: form.editors.code.getValue(),
                sn: form.editors.sn.getValue(),
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.qty.setValue(form.editors.qty.getValue() + r.data.qty)
                data.message = {msg: r.data.message};
                form.editors.sn.setValue('');
            }
        });
    },
    resetMaterialForm() {
        let me = this;
        let form = me.materialLabelForm;
        form.setData({});
        form.editors.message.reset();
        form.editors.print_old.setValue(false);
    },
    resetPackageForm(){
        let me = this;
        let form = me.packageLabelForm;
        form.setData({});
        form.editors.message.reset();
    }
})
