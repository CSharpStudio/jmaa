//@ sourceURL=warehouseSearchEditor.js
jmaa.searchEditor('warehouseSearchEditor', {
    extends: "searchEditors.many2many-tags",
    lookup: function () {
        let me = this, body = me.dom.find('.lookup-body');
        let companyIds = me.owner.editors['company_id'].getRawValue();
        body.html('<div class="m-2">' + '加载中'.t() + '</div>');
        let criteria = [["present", "like", me.keyword]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "searchWarehouse",
            args: {
                criteria: criteria,
                limit: me.limit,
                offset: me.offset,
                companyIds: companyIds,
            },
            onsuccess: function (r) {
                me.renderItems(r.data.values);
                let nextBtn = me.dom.find('[data-btn=next]');
                if (r.data.hasNext) {
                    nextBtn.attr('disabled', false);
                } else {
                    nextBtn.attr('disabled', true);
                }
            }
        });
    }
});
