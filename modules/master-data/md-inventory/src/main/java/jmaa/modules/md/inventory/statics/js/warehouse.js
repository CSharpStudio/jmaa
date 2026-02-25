//@ sourceURL=warehouse.js
jmaa.view({
    searchLocation: function (e, target) {
        let me = this;
        me.form.editors.location_ids.load();
    },
    filterArea(criteria){
        let me = this;
        criteria.push(['warehouse_id', '=', me.form.dataId]);
        return criteria;
    },
    filterLocation(criteria){
        let me = this;
        let val = me.form.dom.find('.search-location').val();
        criteria.push(['warehouse_id', '=', me.form.dataId]);
        criteria.push('|');
        criteria.push('|');
        criteria.push(["code", "like", val]);
        criteria.push(["shelf_code", "like", val]);
        criteria.push(["area_id.code", "like", val]);
        return criteria;
    },
});
