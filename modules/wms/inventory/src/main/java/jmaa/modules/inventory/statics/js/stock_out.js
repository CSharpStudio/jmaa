//@ sourceURL=stock_out.js
jmaa.view({
    detailsFilter(criteria, target) {
        let me = this;
        criteria.push(["stock_out_id", "=", me.form.dataId]);
        let keyword = target.toolbar.dom.find('#searchDetailsInput').val();
        if (keyword) {
            criteria.push("|");
            criteria.push("|");
            criteria.push(['material_id.code', 'like', keyword]);
            criteria.push(['lot_num', 'like', keyword]);
            criteria.push(['label_id.sn', 'like', keyword]);
        }
        return criteria;
    },
    refresh: function (e, target) {
        let me = this;
        target.owner.load();
    },
})
