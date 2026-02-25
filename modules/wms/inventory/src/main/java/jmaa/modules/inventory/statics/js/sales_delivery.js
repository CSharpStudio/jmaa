//@ sourceURL=sales_delivery.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = !['draft', 'reject'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        if (readonly) {
            form.setReadonly(true);
        }
        if (status !== 'done') {
            // 删除放开
            form.editors.details_ids.dom.find('.delete-class').parent().show();
        }
    },
    stockOut() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOut',
            args: {
                ids: [me.form.dataId]
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.form.load();
            }
        });
    },
    //发货按钮
    divery() {
        let me = this;
        if (me.form.editors.line_ids.dirty) {
            jmaa.msg.error('请先保存发料单!'.t());
            return false
        }
        me.loadIssueLine(null, function (data) {
            jmaa.showDialog({
                title: `发料单：${me.curView.editors['code'].getValue()}`,
                init(dialog) {
                    jmaa.rpc({
                        model: 'ir.ui.view',
                        method: 'loadView',
                        args: {
                            model: 'mfg.material_issue_dialog',
                            type: 'form'
                        },
                        onsuccess: function (r) {
                            let v = r.data;
                            dialog.form = dialog.body.JForm({
                                cols: 4,
                                model: v.model,
                                module: v.module,
                                fields: v.fields,
                                arch: v.views.form.arch,
                                view: me
                            });
                            dialog.form.setData({});
                            dialog.form.editors.sn.dom.find('input').focus();
                            dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                                if (e.keyCode == 13 && $(this).val()) {
                                    me.submitCode(dialog.form);
                                }
                            });
                            dialog.form.dom.find('button').each(function () {
                                $(this).html($(this).html().t());
                            });
                            dialog.form.dom.on('click', '.btn-issue', function () {
                                me.issueMaterial(dialog.form);
                            });
                            dialog.form.dom.on('click', '.btn-next', function () {
                                let matId = dialog.form.editors.material_id.getRawValue();
                                me.loadIssueLine(matId, function (data) {
                                    dialog.form.setData(data);
                                    dialog.form.editors.message.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                                });
                            });
                        }
                    });
                },
                cancel(dialog) {
                    me.form.load();
                }
            });
        });
    },
    loadIssueLine(materialId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'readIssueMaterial',
            args: {
                ids: [me.form.dataId],
                materialId
            },
            onsuccess: function (r) {
                callback(r.data);
            }
        });
    },
    issueMaterial(form) {
        let me = this;
        let qty = form.editors.deficit_qty.getValue();
        if (qty <= 0) {
            jmaa.msg.error("发料数量必须大于0".t());
            return form.setInvalid('deficit_qty', '必须大于0');
        }
        let toQty = form.editors.commit_qty.getValue();
        if (qty > toQty) {
            jmaa.msg.error("发料数量不能大于待发数量".t());
            return form.setInvalid('deficit_qty', '不能大于待发数量');
        }
        let onhandQty = form.editors.onhand_qty.getValue();
        if (qty > onhandQty) {
            jmaa.msg.error("发料数量不能大于库存数量".t());
            return form.setInvalid('deficit_qty', '不能大于库存数量');
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'issue',
            args: {
                ids: [me.form.dataId],
                materialId: form.editors.material_id.getRawValue(),
                warehouseId: form.editors.warehouse_id.getRawValue(),
                qty
            },
            onerror(r) {
                if (r.code == 1000) {
                    form.editors.message.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                form.onLoad();
                form.editors.message.setValue({msg: r.data.message});
            }
        });
    },
    submitCode(form) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'submitCode',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
            },
            onerror(r) {
                if (r.code == 1000) {
                    form.editors.message.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                if (r.data.action == 'material' || r.data.action == 'split') {
                    form.setData(r.data.data);
                    form.onLoad();
                    form.editors.message.setValue({msg: r.data.message});
                }
                if (r.data.action == 'split') {
                    me.splitLabel(form, r.data.split);
                }
                form.editors.sn.setValue();
            }
        });
    },
    splitLabel(form, data) {
        let me = this;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${form.editors.sn.getValue()}`,
            init(dialog) {
                let fields = {
                    sn: {name: 'sn', type: 'char', label: '标签条码', readonly: true},
                    qty: {name: 'qty', type: 'float', label: '标签数量', readonly: true},
                    deficit_qty: {name: 'deficit_qty', type: 'float', label: '待发数量', readonly: true},
                    split_qty: {name: 'split_qty', type: 'float', label: '拆分数量'},
                    print_old: {name: 'print_old', type: 'boolean', label: '打印原标签', defaultValue: true},
                };
                let arch = `<form cols="3">
                                <field name="sn"></field>
                                <field name="qty" decimals="${data.unit_accuracy}"></field>
                                <field name="deficit_qty" decimals="${data.unit_accuracy}"></field>
                                <field name="split_qty" decimals="${data.unit_accuracy}"></field>
                                <field name="print_old"></field>
                            </form>`;
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    fields: fields,
                    arch: arch,
                    view: me
                });
                dialog.splitForm.setData(data);
            },
            submit(dialog) {
                dialog.busy(true);
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    args: {
                        sn: dialog.splitForm.editors.sn.getValue(),
                        splitQty: dialog.splitForm.editors.split_qty.getValue(),
                        printOld: dialog.splitForm.editors.print_old.getValue()
                    },
                    onsuccess: function (r) {
                        form.editors.sn.setValue(r.data.newSn);
                        form.editors.message.setValue({msg: '拆分新标签:'.t() + r.data.newSn});
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    addLine: function (e, target) {
        let me = this;
        let data = me.form.getRawData();
        if (!data.related_code) {
            jmaa.msg.error('未关联销售订单'.t());
            return;
        }
        me.addLineDilaog = jmaa.showDialog({
            title: '按销售订单选择'.t(),
            init: function (dialog) {
                me.initLineDialog(dialog, data);
            },
            submit: function (dialog) {
                let sel = dialog.grid.getSelectedData();
                if (sel.length == 0) {
                    jmaa.msg.error('请勾选需要提交的物料'.t());
                    return;
                }
                let check = me.checkFormData(target.data, sel, dialog);
                let line_ids = view.form.editors['line_ids'];
                if (!dialog.grid || !check) {
                    return;
                }
                let datas = dialog.grid.data;
                for (let d of sel) {
                    let commit_qty = dialog.grid.dom.find('[data-field=commit_qty][data-id=' + d.id + ']').children('input').val();
                    commit_qty = Number(commit_qty);
                    if (commit_qty <= 0) {
                        jmaa.msg.error('发货数量必须大于0'.t());
                        return;
                    }
                    // 判断是否优先选择了行号靠前的物料
                    for (const element of datas) {
                        if (d.material_id[0] === element.material_id[0]) {
                            if (element.line_no < d.line_no) {
                                line_qty = dialog.grid.dom.find('[data-id=' + element.id + ']').children('input').val()
                                if (line_qty < element.un_commit_qty) {
                                    jmaa.msg.error('存在相同的物料,请优先选择行号为' + element.line_no + '的物料'.t());
                                    return;
                                }
                            }
                        }
                    }
                    let data = {
                        so_line_id: d.id,
                        line_no: d.line_no,
                        material_id: d.material_id,
                        request_qty: commit_qty,
                        material_name_spec: d.material_name_spec,
                        material_category: d.material_category,
                        unit_id: d.unit_id,
                        unit_accuracy: d.unit_accuracy,
                        delivered_qty: 0,
                        return_qty: 0
                    };
                    for (const row of line_ids.getRawValue()) {
                        if (row.material_id[0] === d.material_id[0] && (row.po_line_id === d.id || row.po_line_id[0] === d.id)) {
                            data.id = row.id;
                            delete data.delivered_qty;
                            delete data.return_qty;
                            break;
                        }
                    }
                    line_ids.save(data);
                }
                dialog.close();
            }
        });
    },
    checkFormData: function (gridDatas, selectedDatas, dialog) {
        let me = this;
        for (let selectedData of selectedDatas) {
            let id = selectedData.id;
            let unCommitQty = selectedData.uncommit_qty;
            let commitQty = dialog.grid.dom.find('[data-id="' + id + '"][data-field="commit_qty"]').val();
            commitQty = Number(commitQty);
            if (commitQty > unCommitQty) {
                jmaa.msg.error('订单[{0}]物料[{1}]发货数量不能大于可建单数'.t().formatArgs(selectedData.so_id[1], selectedData.material_id[1]));
                return false;
            }
            for (let row of gridDatas) {
                if (selectedData.id === row.so_line_id) {
                    if (row.delivered_qty > commitQty) {
                        jmaa.msg.error('订单[{0}]物料[{1}]发货数量不能少于已发数量'.t().formatArgs(selectedData.so_id[1], selectedData.material_id[1]));
                        return false;
                    }
                }
            }
            return true;
        }
    },
    initLineDialog(dialog, data) {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadView',
            args: {
                model: 'sales.order_line',
                type: 'grid'
            },
            onsuccess: function (r) {
                let fields = Object.keys(r.data.fields);
                r.data.fields.commit_qty = {type: 'float', label: '预发数量'};
                dialog.body.css("overflow", "auto");
                dialog.grid = dialog.body.JGrid({
                    model: r.data.model,
                    module: r.data.module,
                    fields: r.data.fields,
                    arch: r.data.views.grid.arch,
                    view: me,
                    ajax: function (grid, callback) {
                        jmaa.rpc({
                            model: me.model,
                            module: me.module,
                            method: 'querySoLine',
                            args: {
                                poCode: data.related_code,
                                fields: fields,
                                order: grid.getSort()
                            },
                            context: {
                                usePresent: true
                            },
                            onsuccess: function (d) {
                                for (const row of view.form.editors.line_ids.data) {
                                    for (const f of d.data) {
                                        if (row.material_id[0] === f.material_id[0] && (row.so_line_id === f.id || row.so_line_id[0] === f.id)) {
                                            if (!row.id.startsWith('new')) {
                                                f.uncommit_qty += row.delivery_qty
                                            }
                                        }
                                    }
                                }
                                callback({
                                    data: d.data
                                });
                            }
                        });
                    }
                });
                dialog.grid.dom.find('.dataTables_wrapper').css({
                    'margin-bottom': '100px',
                    "overflow": "unset"
                });
                dialog.grid.dom.on('click', '.all-check-select', function () {
                    let selected = $('.all-check-select').is(":checked");
                    let datas = dialog.grid.data;
                    if (selected) {
                        for (let e of datas) {
                            let unCommitQty = e.uncommit_qty;
                            $("#" + e.id).find("[data-field='commit_qty']").children('input').val(unCommitQty)
                        }
                    } else {
                        for (let e of datas) {
                            $("#" + e.id).find("[data-field='commit_qty']").children('input').val("")
                        }
                    }
                });
                dialog.grid.dom.on('click', '.check-select', function () {
                    let id = $(this).parents('tr').attr('id');
                    let currentRow = $(this).closest("tr");
                    let commitQty = currentRow.find('[data-field=commit_qty]').children('input').val()
                    if (commitQty === '') {
                        commitQty = Number(currentRow.find("[data-field=uncommit_qty]").text());
                        currentRow.find('[data-field=commit_qty]').children('input').val(commitQty)

                    } else {
                        if (!this.checked) {
                            currentRow.find('[data-field=commit_qty]').children('input').val("")
                        }
                    }
                });
                dialog.grid.table.off('click', 'tbody tr');
                dialog.grid.dom.on('input', '[data-field=commit_qty] input', function (e) {
                    let val = e.currentTarget.value;
                    let id = $(e.currentTarget).parent('div').attr('data-id');
                    if (Number(val) > 0) {
                        $("#" + id).find(".check-select").prop("checked", true);
                        let sel = me.addLineDilaog.grid.getSelected();
                        if (sel.indexOf(id) == -1) {
                            sel.push(id);
                        }
                    } else {
                        $("#" + id).find(".check-select").prop("checked", false);
                        let sel = me.addLineDilaog.grid.getSelected();
                        sel.remove(id);
                    }
                });
            }
        });
    },
    searchLine() {
        let me = this;
        me.form.editors.line_ids.load();
    },
    onToolbarInit(e, bar) {
        let me = this;
        bar.dom.find('#showUndone').on('change', function () {
            me.searchLine();
        });
    },
    lineFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchInput').val();
        if (keyword) {
            criteria.push(['material_id.code', 'like', keyword])
        }
        let showUndone = target.toolbar.dom.find('#showUndone').is(':checked');
        if (showUndone) {
            criteria.push(['status', 'in', ['saved', 'issuing']]);
        }
        return criteria;
    },
    detailsFilter(_, target) {
        let criteria;
        let keyword = target.toolbar.dom.find('#searchDetailsInput').val();
        if (keyword) {
            criteria = ['&', '|', ['material_id.code', 'like', keyword], ['sn', 'like', keyword], [target.field.inverseName, '=', target.owner.dataId]];
        } else {
            criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        }
        return criteria;
    },
    warehouseFilter(criteria, target) {
        let me = this;
        let values = me.form.editors.warehouse_ids.getRawValue();
        criteria.push(['id', 'in', values])
        return criteria;
    },
    soLineOnLoad(e, grid) {
        let me = this;
        grid.dom.find('div[data-field=commit_qty]').each(function () {
            var cfg = {
                field: {
                    name: 'qty',
                    min: 0,
                },
                model: me.model,
                module: me.module,
                owner: grid,
                view: me.view,
                decimals: $(this).attr('data-decimal'),
                dom: $(this),
            };
            var ctl = jmaa.editors['float'];
            var edt = new ctl(cfg);
        });
    },
    deliver() {
        let me = this;
        me.form.offset = 0;
        let warehouseId = me.form.editors.warehouse_id.getRawValue();
        jmaa.showDialog({
            title: '销售发货单:'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'wms.sales_delivery_dialog',
                        type: 'form'
                    },
                    onsuccess: function (r) {
                        let v = r.data;
                        dialog.form = dialog.body.JForm({
                            cols: 4,
                            model: v.model,
                            module: v.module,
                            fields: v.fields,
                            arch: v.views.form.arch,
                            view: me
                        });
                        dialog.dom.find('.buttons-right')
                            .html(`<button type="button" t-click="prevMaterial" class="btn btn-default btn-flat">${'上一个'.t()}</button>
                                    <button type="button" t-click="nextMaterial" class="btn btn-default btn-flat">${'下一个'.t()}</button>`);

                        dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode == 13 && $(this).val()) {
                                me.scanCode(dialog.form);
                            }
                        }).find('input').focus();
                        dialog.form.dom.find('t').each(function () {
                            let el = $(this);
                            el.replaceWith(el.text().t());
                        });
                        me.loadDeliveryMaterial(warehouseId, function (data) {
                            dialog.form.setData(data);
                        });
                    }
                });
            },
            deliveryMaterial(e, dialog) {
                me.deliveryMaterial(dialog.form);
            },
            prevMaterial(e, dialog) {
                me.form.offset--;
                me.loadDeliveryMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                    dialog.form.setData(data);
                    dialog.form.editors.message.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                });
            },
            nextMaterial(e, dialog) {
                me.form.offset++;
                me.loadDeliveryMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                    dialog.form.setData(data);
                    dialog.form.editors.message.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                });
            },
            cancel(dialog) {
                me.form.load();
            }
        });
    },
    loadDeliveryMaterial(warehouseId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadDeliveryMaterial',
            args: {
                ids: [me.form.dataId],
                warehouseId,
                offset: me.form.offset,
            },
            onsuccess: function (r) {
                callback(r.data);
            }
        });
    },
    scanCode(form) {
        let me = this;
        let warehouseId = form.editors.warehouse_id.getRawValue()
        let autoConfirm = form.editors.auto_confirm.getRawValue();
        let scanCode = form.editors.sn.getValue();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
                warehouseId,
                autoConfirm,
            },
            onerror(r) {
                form.editors.sn.setValue();
                if (r.code === 1000) {
                    form.editors.message.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                form.editors.message.setValue({msg: r.data.message});
                if (autoConfirm) {
                    form.editors.auto_confirm.setValue(autoConfirm);
                } else {
                    form.editors.sn.setValue(scanCode);
                }
                if (r.data.action === 'split') {
                    // 这里有数据,就拆分
                    me.splitLabel(form, r.data.split);
                }
            }
        });
    },
    splitLabel(form, data) {
        let me = this;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${form.editors.sn.getValue()}`,
            init(dialog) {
                let fields = {
                    sn: {name: 'sn', type: 'char', label: '标签条码', readonly: true},
                    qty: {name: 'qty', type: 'float', label: '标签数量', readonly: true},
                    deficit_qty: {name: 'deficit_qty', type: 'float', label: '待发数量', readonly: true},
                    split_qty: {name: 'split_qty', type: 'float', label: '拆分数量'},
                    print_old: {name: 'print_old', type: 'boolean', label: '打印原标签', defaultValue: true},
                };
                let arch = `<form cols="3">
                                <field name="sn"></field>
                                <field name="qty" decimals="${data.accuracy}"></field>
                                <field name="deficit_qty" decimals="${data.accuracy}"></field>
                                <field name="split_qty" decimals="${data.accuracy}"></field>
                                <field name="print_old"></field>
                            </form>`;
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    fields: fields,
                    arch: arch,
                    view: me
                });
                dialog.splitForm.setData(data);
            },
            submit(dialog) {
                dialog.busy(true);
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    args: {
                        sn: dialog.splitForm.editors.sn.getValue(),
                        splitQty: dialog.splitForm.editors.split_qty.getValue(),
                        printOld: dialog.splitForm.editors.print_old.getValue()
                    },
                    onsuccess: function (r) {
                        form.editors.sn.setValue(r.data.newSn);
                        form.editors.commit_qty.setValue(r.data.data[0].qty);
                        form.editors.message.setValue({msg: '拆分新标签:'.t() + r.data.newSn});
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    deliveryMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        if (!data.sn) {
            return jmaa.msg.error('请扫描标签'.t());
        }
        let autoConfirm = form.editors.auto_confirm.getRawValue();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'deliveryMaterial',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data)
                form.editors.sn.setValue('')
                form.editors.message.setValue({msg: r.data.message});
                form.editors.auto_confirm.setValue(autoConfirm)
            }
        });
    },
    deleteDetails(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "wms.sales_delivery_details",
            module: me.module,
            method: 'deleteDetails',
            args: {
                ids: grid.selected,
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                me.load()
            }
        });
    },
});
jmaa.column('commit_qty_column', {
    render: function () {
        return function (data, type, row) {
            return `<div data-id="${row.id}" style="min-width: 120px" data-field="commit_qty" data-decimal="${row.unit_accuracy}"></div>`;
        }
    }
});
jmaa.editor('related_code_editor', {
    extends: "editors.many2one",
    searchRelated(callback, fields) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "searchRelatedCode",
            args: {
                relatedField: me.field.name,
                limit: me.limit,
                offset: me.offset,
                criteria: me.getFilter(),
                fields,
                order: me.order
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    },
    loadPresent(id, el, link) {
        let me = this;
        //el.val(id).attr('data-value', id).attr('data-text', id).trigger('change');
        me.setValue([id, id]);
    },
});
