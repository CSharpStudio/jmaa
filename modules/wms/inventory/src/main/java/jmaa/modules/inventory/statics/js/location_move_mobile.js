//@ sourceURL=location_move_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
    },
    cleanForm() {
        let me = this;
        let form = me.locationMoveForm;
        form.editors.qty.setReadonly(false);
        form.setData({});
    },
    resetForm() {
        this.cleanForm();
        this.locationMoveForm.editors.msg.reset();
    },
    scanCode() {
        let me = this;
        let form = me.locationMoveForm;
        let data = form.getRawData();
        let code = data.code;
        if (!code) {
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'findMaterialByCode',
            args: {
                code,
            },
            onsuccess(r) {
                me.mediaScan.play();
                r.data.location_code_target && form.editors.location_code_target.setValue(r.data.location_code_target);
                r.data.qty && form.editors.qty.setValue(r.data.qty);
                form.editors.material_id.setValue(r.data.material_id);
                form.editors.material_name_spec.setValue(r.data.material_name_spec);
                form.editors.qty.setReadonly(r.data.code_type === "sn");
                form.editors.msg.setValue({msg: "条码[{0}]识别成功".t().formatArgs(code)});
            },
            onerror(r) {
                me.mediaError.play();
                if (r.code == 1000) {
                    form.editors.msg.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            }
        });
    },
    scanLocation() {
        this.mediaScan.play();
    },
    submitForm() {
        let me = this;
        let form = me.locationMoveForm;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRawData();
        if (!data.code) {
            return form.editors.msg.setValue({error: true, msg: "条码不能为空".t()});
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'move',
            args: {
                code: data.code,
                qty: data.qty,
                warehouseId: data.warehouse_id,
                locationCodeSource: data.location_code_source,
                locationCodeTarget: data.location_code_target,
            },
            onsuccess(r) {
                me.mediaSubmit.play();
                form.editors.msg.setValue({msg: r.data});
                form.editors.code.resetValue();
                form.editors.location_code_source.resetValue();
                form.editors.location_code_target.resetValue();
                form.editors.qty.resetValue();
            },
            onerror(r) {
                me.mediaError.play();
                if (r.code == 1000) {
                    form.editors.msg.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            }
        });
    }
});
