//@ sourceURL=material_issue_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.orderList.load();
        me.dom.find('[name=detailScope]').on('change', function () {
            me.detailsList.load();
        });
        me.dom.find('[name=materialScope]').on('change', function () {
            me.materialList.load();
        });
    },
    searchOrder() {
        let me = this;
        me.dom.find('.issue-input').val('');
        me.orderList.load();
    },
    openIssue() {
        let me = this;
        me.changePage("details");
        let data = me.orderList.getSelectedData()[0];
        me.issueId = data.id;
        me.issueForm.orderData = {
            code: data.code,
            warehouse_ids: data.warehouse_ids,
            related_code: data.related_code,
            workshop_id: data.workshop_id,
        }
        me.issueForm.offest = 0;
        me.issueForm.editors.result.reset();
        me.loadIssueMaterial();
        me.tabs.open('issueTab');
    },
    nextMaterial() {
        let me = this;
        me.issueForm.offest++;
        me.loadIssueMaterial(true);
    },
    prevMaterial() {
        let me = this;
        me.issueForm.offest--;
        me.loadIssueMaterial(true);
    },
    loadIssueMaterial(showResult) {
        let me = this;
        if (!me.issueId) {
            return jmaa.msg.error('请选择发料单'.t());
        }
        let form = me.issueForm;
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadIssueMaterial',
            args: {
                ids: [me.issueId],
                offset: form.offest,
                warehouseId: data.warehouse_id,
            },
            onsuccess: function (r) {
                $.extend(r.data, form.orderData);
                form.setData(r.data);
                if (showResult) {
                    data.result = {msg: `${'读取物料'.t()}[${r.data.material_id[1]}]`};
                }
            }
        });
    },
    filterWarehouse(criteria) {
        let me = this;
        let data = me.issueForm.getRaw();
        criteria.push(['id', 'in', data.warehouse_ids]);
        return criteria;
    },
    loadOrderList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-order-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchIssueOrder',
            args: {
                keyword,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                order: 'code asc',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data});
            }
        });
    },
    showMaterialList() {
        let me = this;
        me.materialList.load();
    },
    loadMaterialList(list, callback) {
        let me = this;
        let criteria = [['issue_id', '=', me.issueId]];
        let scope = me.dom.find('[name=materialScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', 'not in', ['issued', 'done']]);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                relatedField: 'line_ids',
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
    showDetailsList() {
        let me = this;
        me.detailsList.load();
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['issue_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=detailScope]:checked').val();
        if (scope == 'undone') {
            criteria.push(['status', '!=', 'done']);
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
    scanIssue() {
        let me = this;
        let input = me.dom.find('.issue-input');
        let code = input.val();
        if (!code) {
            return;
        }
        let form = me.issueForm;
        if (!me.issueId) {
            return jmaa.msg.error('请选择发料单'.t());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'issue',
            args: {
                ids: [me.issueId],
                code,
            },
            onerror(r) {
                me.mediaError.play();
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                data.result = {msg: r.data.message};
                $.extend(r.data.data, me.issueForm.orderData);
                me.issueForm.setData(r.data.data);
                if (r.data.split) {
                    me.mediaScan.play();
                    me.splitLabel(r.data.split);
                } else if (r.data.confirm) {
                    me.mediaScan.play();
                    me.confirmIssue(r.data.confirm);
                } else {
                    me.mediaSubmit.play();
                }
            }
        });
    },
    confirmIssue(confirmData) {
        let me = this;
        let form = me.issueForm;
        jmaa.showDialog({
            title: '确认发料：'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    model: form.model,
                    fields: form.fields,
                    module: me.module,
                    arch: `<form cols="2">
                                <div class="card-view">
                                    <field name="material_id" editor="card-item" colspan="2"></field>
                                    <editor name="material_name_spec" label="名称规格" type="card-item" colspan="2"></editor>
                                    <editor name="code" type="char" visible="0"></editor>
                                    <editor name="label_qty" type="card-item" label="标签数量"></editor>
                                    <editor name="to_issue_qty" type="card-item" label="待发数量"></editor>
                                    <field name="warehouse_id" editor="card-item" colspan="2"></field>
                                </div>
                                <editor name="location" label="库位" type="scanner"></editor>
                                <editor name="issue_qty" type="float" label="发料数量" required="1" min="0" decimals="${confirmData.unit_accuracy}"></editor>
                                <editor name="print_label" label="打印新标签" type="boolean"></editor>
                                <field name="template_id" lookup="[['category','=','material_label']]" t-visible="print_label"></field>
                            </form>`,
                    view: me
                });
                dialog.form.dom.enhanceWithin();
                dialog.form.setData(confirmData);
                dialog.form.editors.issue_qty.setAttr('max', confirmData.lock_qty ? confirmData.issue_qty : null);
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'issueMaterial',
                    args: {
                        ids: [me.issueId],
                        code: data.code,
                        materialId: data.material_id,
                        warehouseId: data.warehouse_id,
                        locationCode: data.location,
                        qty: data.issue_qty,
                        printLabel: data.print_label,
                        templateId: data.template_id,
                    },
                    onerror(r) {
                        if (r.code == 1000) {
                            form.getData().result = {error: true, msg: r.message};
                        }
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        r.data.data.result = {msg: r.data.message};
                        form.setData(r.data.data);
                        let printData = r.data.printData
                        if (printData) {
                            jmaa.print(printData);
                        }
                        dialog.close();
                    }
                });
            }
        });
    },
    splitLabel(splitData) {
        let me = this;
        jmaa.showDialog({
            title: '拆分标签'.t(),
            init(dialog) {
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    fields: me.fields,
                    arch: `<form>
                                <div class="card-view">
                                    <field name="material_id" editor="card-item" colspan="2"></field>
                                    <editor name="material_name_spec" label="名称规格" type="card-item" colspan="2"></editor>
                                    <field name="warehouse_id" editor="card-item" colspan="2"></field>
                                    <field name="location_id" editor="card-item" colspan="2"></field>
                                    <editor name="sn" type="card-item" label="标签序列号" colspan="2"></editor>
                                    <editor name="qty" type="card-item" label="标签数量"></editor>
                                    <editor name="to_issue_qty" type="card-item" label="待发数量"></editor>
                                </div>
                                <editor name="split_qty" type="float" label="拆分数量" decimals="${splitData.unit_accuracy}"></editor>
                                <editor name="print_old" type="boolean" label="打印原标签"></editor>
                            </form>`,
                    view: me
                });
                dialog.splitForm.dom.enhanceWithin();
                dialog.splitForm.setData(splitData);
            },
            submit(dialog) {
                let data = dialog.splitForm.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    args: {
                        ids: [me.issueId],
                        sn: data.sn,
                        splitQty: data.split_qty,
                        printOld: data.print_old
                    },
                    dialog,
                    onsuccess: function (r) {
                        r.data.data.result = {msg: r.data.message};
                        me.issueForm.setData(r.data.data);
                        jmaa.print(r.data.printData, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    createStockOut() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOut',
            args: {
                ids: [me.issueId]
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.detailsList.load();
            }
        });
    },
})
