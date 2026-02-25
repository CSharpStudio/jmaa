//@ sourceURL=inventory_check_first_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.dom.find('.search-input').val('');
        me.orderList.load()
        me.dom.find('[name=detailScope]').on('change', function () {
            me.detailsList.load();
        });
        me.dom.find('[name=materialScope]').on('change', function () {
            me.lineList.load();
        });
    },
    searchOrder() {
        let me = this;
        me.dom.find('.code-input').val('');
        me.scanMaterialForm.setData({})
        me.scanMaterialForm.editors.message.reset()
        me.orderList.load();
    },
    loadOrderList(list, callback) {
        let me = this;
        let criteria = [["status", "in", ["first_running", "approve"]]];
        let keyword = me.dom.find('.search-input').val();
        if (keyword) {
            criteria.push(['code', 'like', keyword]);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'search',
            args: {
                criteria,
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
    openDetails() {
        let me = this;
        me.changePage("details");
        if ('first_done' === me.orderList.getSelectedData()[0].status[0] || 'second_done' === me.orderList.getSelectedData()[0].status[0]) {
            //me.dom.find('.first-check-final .scan-confirm').hide();
            me.dom.find('.first-check-final').hide();
            me.dom.find('.scan-confirm').hide();
        } else {
            //me.dom.find('.first-check-final .scan-confirm').show();
            me.dom.find('.first-check-final').show();
            me.dom.find('.scan-confirm').show();
        }
        let data = me.orderList.getSelectedData()[0]
        me.scanMaterialForm.editors.warehouse_id.setValue(data.warehouse_id[0]);
    },
    loadLineList(list, callback) {
        let me = this;
        let criteria = [['inventory_check_id', '=', me.orderList.getSelected()[0]]];
        let keyword = me.dom.find('.code-line-input').val();
        if (keyword) {
            criteria.push(['code', '=', keyword]);
        }
        let scope = me.dom.find('[name=materialScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', '=', 'create']);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                relatedField: 'line_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['inventory_check_id', '=', me.orderList.getSelected()[0]]];
        let keyword = me.dom.find('.code-details-input').val();
        if (keyword) {
            criteria.push(['code', '=', keyword]);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                relatedField: 'details_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    },
    searchDetails() {
        let me = this;
        me.detailsList.load()
    },
    searchLine() {
        let me = this;
        me.lineList.load()
    },
    scanDetailsCode(e) {
        // 扫码定位标签明细
        let me = this;
        me.detailsList.load()
    },
    scanLineCode(list, callback) {
        let me = this;
        me.lineList.load()
    },
    openLabelDetail() {
        // 查看物料对应的所有标签
        let me = this;
        me.changePage("line-label-main");
    },
    loadLabelList(list, callback) {
        let me = this;
        let inventoryCheckId = me.orderList.getSelected()[0]
        let criteria = [["inventory_check_id", "=", inventoryCheckId]];
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                relatedField: 'details_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
            }
        });
    },
    scanCode() {
        let me = this;
        let input = me.dom.find('.code-input');
        let code = input.val();
        let dataId = me.orderList.getSelected()[0]
        let data = me.scanMaterialForm.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanLabelCode',
            args: {
                ids: [dataId],
                code,
                warehouseId: data.warehouse_id,
                locationCode: data.location_id
            },
            onerror(r) {
                if (r.code == 1000) {
                    data.message = {error: true, msg: r.message};
                    input.val('')
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                data.message = {msg: r.data.message};
                //me.scanMaterialForm.setData(r.data.data);
                // 其他字段不动
                me.scanMaterialForm.editors.material_id.setValue(r.data.data.material_id);
                me.scanMaterialForm.editors.qty.setValue(r.data.data.qty);
                me.scanMaterialForm.editors.blind_qty.setValue(r.data.data.blind_qty);
                me.scanMaterialForm.editors.first_qty.setValue(r.data.data.first_qty);
                me.scanMaterialForm.editors.material_name_spec.setValue(r.data.data.material_name_spec);
            }
        });
    },
    scanConfirm: function () {
        let me = this;
        let input = me.dom.find('.code-input');
        let form = me.scanMaterialForm;
        if (!me.scanMaterialForm.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let code = input.val();
        if (!code) {
            return jmaa.msg.error("请扫标签码".t());
        }
        let firstQty = form.editors.first_qty.getRawValue();
        if (!firstQty || firstQty < 0) {
            return jmaa.msg.error("数量需大于0".t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanConfirm',
            args: {
                ids: [me.orderList.getSelected()[0]],
                code,
                warehouseId: form.editors.warehouse_id.getRawValue(),
                locationCode: form.editors.location_id.getRawValue(),
                qty: form.editors.qty.getRawValue(),
                materialId: form.editors.material_id.getRawValue(),
                firstQty
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                form.getRaw().message = {msg: r.data.message};
                input.val('')
                me.scanMaterialForm.editors.material_id.setValue('');
                me.scanMaterialForm.editors.qty.setValue('');
                me.scanMaterialForm.editors.blind_qty.setValue('');
                me.scanMaterialForm.editors.first_qty.setValue('');
                me.scanMaterialForm.editors.material_name_spec.setValue('');
            }
        });
    },
    deleteDetails(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.msg.confirm({
            title: '确认删除'.t(),
            content: '确认删除?'.t(),
            submit() {
                jmaa.rpc({
                    model: "wms.inventory_check_details",
                    module: me.module,
                    method: 'delete',
                    args: {
                        ids: [id],
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        me.detailsList.load();
                    }
                });
            },
            cancel() {
            },
        });
    },
    firstCheckFinal(e) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "firstCheckFinal",
            args: {
                ids: [me.orderList.getSelected()[0]]
            },
            onsuccess(r) {
                jmaa.msg.show("操作成功".t());
                //me.dom.find('.first-check-final .scan-confirm').hide();
                me.dom.find('.first-check-final').hide();
                me.dom.find('.scan-confirm').hide();
            }
        });
    },
    areaFilter(criteria) {
        let me = this;
        let storeAreaIds = me.orderList.getSelectedData()[0].store_area_ids
        if (storeAreaIds) {
            let values = [];
            for (let storeAreaId of storeAreaIds) {
                let areaId = storeAreaId[0];
                values.push(areaId);
            }
            criteria.push(['id', 'in', values])
        }
        return criteria;
    },
    locationFilter(criteria) {
        let me = this;
        let storeLocationIds = me.orderList.getSelectedData()[0].store_location_ids
        let values = [];
        if (storeLocationIds && storeLocationIds.length > 0) {
            for (let storeLocationId of storeLocationIds) {
                let areaId = storeLocationId[0];
                values.push(areaId);
            }
            criteria.push(['id', 'in', values])
        } else {
            let area = me.scanMaterialForm.editors.area_id.getRawValue()
            criteria.push(['area_id', '=', area])
        }
        return criteria;
    },
    checkLocation() {
        let me = this;
        let locationId = me.scanMaterialForm.editors.location_id.getRawValue();
        if (locationId) {
            // 有值,
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'checkLocation',
                args: {
                    ids: [me.orderList.getSelected()[0]],
                    locationCode:locationId,
                },
                onerror(r) {
                    me.scanMaterialForm.editors.location_id.setValue(null)
                    jmaa.msg.error(r);
                },
                onsuccess: function (r) {
// 校验成功
                }
            });
        }
    },
})
