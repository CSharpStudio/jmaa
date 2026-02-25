//@ sourceURL=purchase_price.js
jmaa.view({
    computePrice(e) {
        let me = this;
        let data = e.owner.getRaw();
        if (data.price_tax && data.tax_rate) {
            data.price = Math.round(data.price_tax * 1000000 / (1 + data.tax_rate / 100)) / 1000000;
        }
    }
});
