//@ sourceURL=initial_inventory.js
jmaa.view({
    createLabelCode(){
        let me = this;
        let form = me.form
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
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
    initial(){
        let me = this;
        let form = me.form
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
                form.editors.qty.resetValue();
            }
        });
    },
    reprintLabel(e, target) {
        let me = this;
        me.rpc(me.model, 'reprintLabel', {
            ids: target.getSelected(),
        }).then(data => {
            jmaa.print(data);
        })
    }
})
