//@ sourceURL=initial_inventory_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.initialForm.editors.is_store.setValue(true);
        me.initialForm.editors.auto_print.setValue(true);
    },
    createLabelCode(){
        let me = this;
        let form = me.initialForm
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRawData()
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'createLabelCode',
            args: {
                materialId:data.material_id,
                qty:data.qty,
                productDate:data.product_date,
                productLot:data.product_lot,
                templateId:data.template_id,
                supplierId:data.supplier_id,
                customerId:data.customer_id,
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                let resultData = r.data.data[0];
                form.editors.lot_num.setValue( resultData.lot_num);
                form.editors.sn.setValue(resultData.sn);
            }
        });
    },
    scanMaterial(){
        let me = this;
        let form = me.initialForm;
        let data = form.getRawData();
        let materialCode = data.material_code;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'findMaterialByCode',
            args: {
                materialCode
            },
            onsuccess(r) {
                me.mediaScan.play();
                form.editors.material_id.setValue(r.data.material_id)
                form.editors.material_code.setValue(r.data.material_code)
                form.editors.template_id.setValue(r.data.template_id)
                form.editors.stock_rule.setValue(r.data.stock_rule)
                form.editors.unit_id.setValue(r.data.unit_id)
                form.editors.material_name_spec.setValue(r.data.material_name_spec)
            },
            onerror(r) {
                me.mediaError.play();
                jmaa.msg.error(r);
            }
        });
    },
    scanLocation(){
        let me = this;
        let form = me.initialForm;
        let data = form.getRawData();
        let locationCode = data.location_code;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'findLocationByCode',
            args: {
                warehouseId:data.warehouse_id,
                locationCode,
            },
            onsuccess(r) {
                me.mediaScan.play();
                form.editors.location_id.setValue(r.data.location_id)
            },
            onerror(r) {
                me.mediaError.play();
                jmaa.msg.error(r);
                form.editors.location_code.setValue('')
                form.editors.location_id.setValue('')
            }
        });
    },
    initial(){
        let me = this;
        let form = me.initialForm
        let data = form.getRawData()
        if (!data.sn && !data.lot_num){
            return jmaa.msg.error("请先生成条码".t())
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'initial',
            args: {
                data:data
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                form.editors.lot_num.setValue('');
                form.editors.sn.setValue('');
                form.editors.qty.setValue('');
                if (r.data.data) {
                    jmaa.print(r.data);
                }
            }
        });
    },
    loadDetailsList(list, callback){
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'search',
            args: {
                criteria:[],
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
    showDetailsList(){
        let me = this;
        me.detailsList.load()
    },
    deleteDetail(e){
        let me = this;
        let btn = $(e.target);
        let id = btn.closest(".ui-list-item[data-id]").attr('data-id');
        jmaa.showDialog({
            title: '确认删除'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    arch: `<form cols="1">
                            <span>确认删除?</span>
                        </form>`,
                });
            },
            submit(dialog) {
                jmaa.rpc({
                    module: me.module,
                    model: me.model,
                    method: 'delete',
                    args: {
                        ids: [id]
                    },
                    onsuccess() {
                        me.detailsList.load();
                        jmaa.msg.show("操作成功".t());
                        dialog.close();
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    }
                });
            }
        });
    },
    resetForm() {
        let me = this;
        let form = me.initialForm;
        let is_store = form.getData().is_store;
        let auto_print = form.getData().auto_print;
        form.setData({});
        form.editors.is_store.setValue(is_store);
        form.editors.auto_print.setValue(auto_print);
    },
})
