//@ sourceURL=craft_route_product.js
jmaa.view({
    filterNode() {
        let me = this;
        let routeId = me.form.getRaw().craft_route_id;
        return [['route_id', '=', routeId], ['is_end', "=", false]];
    }
});
