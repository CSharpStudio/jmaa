//@ sourceURL=account_purchase_order.js
jmaa.view({
    readPrice(e) {
        let me = this;
        let order = me.form.getRaw();
        let data = e.owner.getRaw();
        if (data.material_id && data.purchase_qty) {
            if (!data.price_tax) {
                me.rpc(e.model, "readPrice", {
                    supplierId: order.supplier_id,
                    type: order.type,
                    materialId: data.material_id,
                    qty: data.purchase_qty,
                }).then(r => {
                    if (r.price) {
                        data.price = r.price;
                        data.tax_rate = r.tax_rate;
                        data.price_tax = r.price_tax;
                        data.total_price = r.total_price;
                        r.accuracy && e.owner.editors.total_price.setAttr("data-decimals", r.accuracy);
                        r.price_accuracy && e.owner.editors.price_tax.setAttr("data-decimals", r.price_accuracy);
                    }
                });
            } else {
                data.total_price = Math.round(data.purchase_qty * data.price_tax);
            }
        }
    },
    computeTotalPrice(e) {
        let me = this;
        let data = e.owner.getRaw();
        if (data.price_tax) {
            if (data.tax_rate) {
                data.price = Math.round(data.price_tax * 1000000 / (1 + data.tax_rate / 100)) / 1000000;
            }
            if (data.purchase_qty) {
                data.total_price = Math.round(data.purchase_qty * 100 * data.price_tax) / 100;
            }
        }
    }
});
