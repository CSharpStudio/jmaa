//@ sourceURL=material_receipt.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let labelButtons = form.editors.line_ids.dom.find('.label-group').parent();
        let editButtons = form.editors.line_ids.dom.find('.btn-edit-group').parent();
        if (['draft', 'reject'].includes(status)) {
            editButtons.show();
        } else {
            editButtons.hide();
        }
        if (status == 'approve') {
            labelButtons.show();
        } else {
            labelButtons.hide();
        }
        if (['done', 'close'].includes(status)) {
            form.setReadonly(true);
            form.dom.find('[name=btn_group_reprint]').show();
        } else if (!['draft', 'reject'].includes(status)) {
            for (let editor in form.editors) {
                if (!['details_ids', 'pallet_ids', 'line_ids'].includes(editor)) {
                    form.editors[editor].readonly(true);
                }
            }
        }
        me.form.editors.supplier_id.setReadonly(me.form.editors.line_ids.values.length > 0);
    },
    deleteAllLines() {
        let me = this;
        me.form.editors.line_ids.deleteAll();
    },
    showPoDialog() {
        let me = this
        let supplierId = me.form.editors.supplier_id.getRawValue();
        if (!supplierId) {
            return jmaa.msg.error('请先选择供应商'.t());
        }
        me.loadView('purchase.order_line', 'grid', 'purchase_order_line_dialog_key').then(v => {
            me.addReceiptDilaog = jmaa.showDialog({
                title: '按采购单选择'.t(),
                init: function (dialog) {
                    let tpl = `<div class="btn-row" style="display: flex;justify-content: space-between">
                                    <div class="po-item-form"></div>
                            </div>
                            <div class="toolbar" style="margin: 2px 4px 2px 8px;">
                                <button type="button" t-click="searchOrder" class="btn-info btn btn-flat">${'查询'.t()}</button>
                                <div class="float-right">
                                    <div class="po-item-pager"></div>
                                </div>
                            </div>
                            <div style='overflow: auto;width: 100%;min-height: 300px' class='po-item-grid o2m-grid'></div>`;
                    dialog.body.html(tpl);
                    dialog.body.addClass("o2m-grid m-3");

                    //初始化查询表单
                    me.initPoSearch(dialog);
                    //初始化分页控件
                    me.initPoPager(dialog, v);
                    //初始化表格控件
                    me.initPoGrid(dialog, v);
                },
                searchOrder(e, dialog) {
                    dialog.grid.load();
                },
                submit: function (dialog) {
                    me.poSubmit(dialog)
                }
            });
        })
    },
    //初始化查询表单
    initPoSearch(dialog) {
        dialog.form = dialog.body.find('.po-item-form').JForm({
            fields: {
                po_id: {name: 'po_id', type: 'char', label: '采购订单号'},
                material_id: {name: 'material_id', type: 'char', label: '物料编码'},
                name_spec: {name: 'name_spec', type: 'char', label: '名称规格'},
                order_data: {name: 'order_data', type: 'date', label: '订单日期'}
            },
            arch: `<form>
                      <field name="po_id"></field>
                      <field name="material_id"></field>
                      <field name="name_spec"></field>
                      <field name="order_data" editor="date-range"></field>
                   </form>`,
        });
        dialog.form.dom.find('.e-form').css('padding', '0');
        dialog.form.setData({})
    },
    //初始化采购订单分页
    initPoPager(dialog, res) {
        let me = this;
        let baseType = view.curView.editors.type.getRawValue()
        dialog.pager = new JPager({
            dom: dialog.body.find('.po-item-pager'),
            limit: 200,
            limitChange: function (e, pager) {
                if (dialog.grid) {
                    dialog.grid.limit = pager.limit;
                }
            },
            pageChange: function (e, pager) {
                pager.update(e)
                dialog.grid.load();
            },
            counting: function (e, pager) {
                let criteria = me.getPoCriteria(dialog);
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'countPoLine',
                    args: {
                        criteria
                    },
                    onsuccess: function (r) {
                        dialog.pager.update({
                            total: r.data
                        });
                    }
                });
            }
        });
    },
    //初始化采购订单列表
    initPoGrid(dialog, res) {
        let me = this;
        let fields = Object.keys(res.fields);
        fields.remove('request_qty');
        dialog.grid = dialog.body.find('.po-item-grid').JGrid({
            model: res.model,
            module: res.module,
            fields: res.fields,
            arch: res.views.grid.arch,
            view: me,
            ajax: function (grid, callback) {
                let order = grid.getSort()
                let criteria = me.getPoCriteria(dialog);
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'searchPoLine',
                    args: {
                        criteria: criteria,
                        fields: fields,
                        order: order,
                        nextTest: true,
                        offset: dialog.pager.getOffset(),
                        limit: dialog.pager.getLimit(),
                    },
                    context: {
                        usePresent: true
                    },
                    onsuccess: function (d) {
                        let asnMaterialDetailIds = view.form.editors.line_ids.data;
                        for (const asm of asnMaterialDetailIds) {
                            for (const f of d.data.values) {
                                if (asm.material_id[0] === f.material_id[0] && asm.po_id[0] === f.po_id[0] && asm.po_line_no === f.line_no) {
                                    if (!asm.id.startsWith('new')) {
                                        f.uncommit_qty += asm.request_qty
                                    }
                                }
                            }
                        }
                        // 过滤掉未建单数为0的
                        if (d.data.values.length > 0) {
                            callback({
                                data: d.data.values
                            });
                            dialog.pager.update(d.data);
                        } else {
                            callback({
                                data: []
                            });
                            dialog.pager.noData();
                        }
                    }
                });
            }
        });
        dialog.grid.dom.on('click', '.check-select', function (e) {
            let currentRow = $(this).closest("tr");
            let adavanceQty = currentRow.find('[column=commit_qty] input[type=number]').val();
            let unCommitQty = currentRow.find('[data-field=uncommit_qty]').text();
            if (adavanceQty === '') {
                adavanceQty = Number(unCommitQty);
                currentRow.find('[column=commit_qty] input[type=number]').val(adavanceQty)
            } else {
                if (!this.checked) {
                    currentRow.find('[column=commit_qty] input[type=number]').val("")
                }
            }
        }).on('click', '.all-check-select', function () {
            let selected = $('.all-check-select').is(":checked");
            let datas = dialog.grid.data;
            if (selected) {
                for (let e of datas) {
                    let unCommitQty = e.uncommit_qty;
                    let val = $("#" + e.id).find("[column=commit_qty] input[type=number]").val();
                    if (!val) {
                        $("#" + e.id).find("[column=commit_qty] input[type=number]").val(unCommitQty)
                    }
                }
            } else {
                for (let e of datas) {
                    $("#" + e.id).find("[column=commit_qty] input[type=number]").val("")
                }
            }
        }).on('input', '[column=commit_qty] input', function (e) {
            let val = e.currentTarget.value;
            let id = $(e.currentTarget).parent('div').attr('data-id');
            if (Number(val) > 0) {
                $("#" + id).find(".check-select").prop("checked", true);
                let sel = dialog.grid.getSelected();
                if (sel.indexOf(id) == -1) {
                    sel.push(id);
                }
            } else {
                $("#" + id).find(".check-select").prop("checked", false);
                let sel = dialog.grid.getSelected();
                sel.remove(id);
            }
        });
        dialog.grid.table.off('click', 'tbody tr');
    },
    getPoCriteria(dialog) {
        let me = this;
        let searchForm = dialog.form;
        let searchData = searchForm.getData();
        let criteria = [["po_id.supplier_id", "=", me.form.editors.supplier_id.getRawValue()], ["uncommit_qty", ">", 0]];
        if (searchData.po_id) {
            criteria.push(['po_id.code', 'like', searchData.po_id]);
        }
        if (searchData.material_id) {
            criteria.push(['material_id.code', 'like', searchData.material_id]);
        }
        if (searchData.name_spec) {
            criteria.push(['material_id.name_spec', 'like', searchData.name_spec]);
        }
        if (searchData.order_data) {
            let dataRange = searchData.order_data.split(" - ")
            if (dataRange[0]) {
                criteria.push(['po_id.order_date', '>=', dataRange[0]]);
            }
            if (dataRange[1]) {
                criteria.push(['po_id.order_date', '<=', dataRange[1]]);
            }
        }
        return criteria;
    },
    poSubmit: function (dialog) {
        let me = this;
        let material_details_ids = view.form.editors['line_ids'];
        let gridData = material_details_ids.getRawValue()
        let check = me.checkPoSubmit(gridData, dialog.grid.getSelectedData(), dialog);
        if (!dialog.grid || !check) {
            return;
        }
        let sel = dialog.grid.getSelectedData();
        if (sel.length === 0) {
            jmaa.msg.error('请勾选需要提交的物料'.t());
            return;
        }
        for (let d of sel) {
            let request_qty = dialog.grid.dom.find(`[column=commit_qty][data-id=${d.id}] input[type=number]`).val();
            request_qty = Number(request_qty);
            if (request_qty <= 0) {
                jmaa.msg.error('预收数量必须大于0'.t());
                return;
            }
            let data = {
                po_line_id: d.id,
                po_line_no: d.line_no,
                po_id: d.po_id,
                material_id: d.material_id,
                material_name_spec: d.material_name_spec,
                unit_id: d.unit_id,
                unit_accuracy: d.unit_accuracy,
                request_qty: request_qty,
                delivery_date: d.delivery_date,
                promised_delivery_date: d.promised_delivery_date,
                portal_status: 'new',
                status: 'new',
                gift_qty: 0,
                print_qty: 0,
                is_pallet: true,
                stock_rule: d.stock_rule,
                material_category: d.material_category,
            }
            let whInput = dialog.grid.dom.find('[data-field=warehouse_id] [data-id=' + d.id + ']');
            if (whInput.attr('data-value')) {
                data.warehouse_id = [whInput.attr('data-value'), whInput.data('text')];
            }
            for (const row of gridData) {
                if (row.material_id[0] === d.material_id[0] && (row.po_line_id === d.id || row.po_line_id[0] === d.id)) {
                    data = {id: row.id, request_qty}
                    break;
                }
            }
            material_details_ids.save(data);
        }
        dialog.close();
    },
    //检验选择的采购订单数据
    checkPoSubmit: function (gridDatas, selectedDatas, dialog) {
        let me = this;
        let companyId;
        for (let selectedData of selectedDatas) {

            let id = selectedData.id;
            let unCommitQty = selectedData.uncommit_qty;
            let advanceQty = dialog.grid.dom.find('[data-id="' + id + '"] input[type=number]').val();
            advanceQty = Number(advanceQty);
            if (advanceQty > unCommitQty) {
                jmaa.msg.error('订单[{0}]物料[{1}]预收数量不能大于可建单数'.t().formatArgs(selectedData.po_id[1], selectedData.material_id[1]));
                return false;
            }
        }
        return true;
    },
    addReceiptLine: function (e, target) {
        let me = this, data = me.form.getRawData();
        if (data.type === 'purchase.order') {
            me.showPoDialog();
        } else {
            jmaa.msg.confirm({
                title: '提示'.t(),
                content: '非采购收料不能添加物料'.t(),
            });
        }
    },
    checkCommitData: function (gridData, selectedData, dialog) {
        let me = this;
        for (let data of selectedData) {
            let id = data.id;
            let unCommitQty = data.uncommit_qty;
            let commitQty = dialog.grid.dom.find('[data-id="' + id + '"][data-field="commit_qty"]').val();
            commitQty = Number(commitQty);
            if (commitQty > unCommitQty) {
                jmaa.msg.error('订单[{0}]物料[{1}]预收数量不能大于可建单数'.t().formatArgs(data.po_id[1], data.material_id[1]));
                return false;
            }
            for (let row of gridData) {
                if (data.id === row.po_line_id && row.receive_qty > commitQty) {
                    jmaa.msg.error('订单[{0}]物料[{1}]预收数量不能少于实收数量'.t().formatArgs(data.po_id[1], data.material_id[1]));
                    return false;
                }
            }
            return true;
        }
    },
    //查询
    searchLine() {
        let me = this;
        me.form.editors.line_ids.load();
    },
    readReceiptMaterial(materialId, next, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'readReceiptMaterial',
            args: {
                ids: [me.form.dataId],
                materialId,
                next
            },
            onsuccess: function (r) {
                callback(r.data);
            }
        });
    },
    receipt() {
        let me = this;
        let editor = me.form.editors.line_ids;
        let selectedData = editor.grid.getSelectedData();
        let materialId;
        if (selectedData.length > 0) {
            let getId = function (val) {
                if (val && val[0]) {
                    return val[0];
                }
                return val;
            }
            materialId = getId(selectedData[0].material_id);
            for (let d of selectedData) {
                if (getId(d.material_id) != materialId) {
                    return jmaa.msg.error("不同物料不能一起接收".t());
                }
            }
        }
        jmaa.showDialog({
            title: `收料单：${me.curView.editors['code'].getValue()}`,
            init(dialog) {
                let tab = $(`<tabs>
                        <tab label="${'打印收货'.t()}"><div class="printReceipt"></div></tab>
                        <tab label="${'扫码收货'.t()}"><div class="scanReceipt"></div></tab>
                    </tabs>`).JTabs();
                dialog.body.html(tab.dom.html()).addClass('p-2').find('.tab-bar').css('width', '100%');
                me.loadView('wms.material_receipt_dialog', 'form').then(function (v) {
                    dialog.form = dialog.body.find('.printReceipt').JForm({
                        model: v.model,
                        module: v.module,
                        fields: v.fields,
                        arch: v.views.form.arch,
                        view: me
                    });
                    dialog.form.dom.on('click', '[role=print]', function () {
                        me.receiptPrint(dialog);
                    }).on('click', '[role=next]', function () {
                        me.readReceiptMaterial(dialog.form.editors.material_id.getRawValue(), true, function (data) {
                            dialog.form.setData(data);
                        });
                    }).on('click', '[role=reset]', function () {
                        let data = dialog.form.getData();
                        data.commit_qty = null;
                        data.gift_qty = 0;
                        data.label_count = 0;
                        data.product_date = null;
                        data.product_lot = null;
                        dialog.form.clearInvalid();
                    });
                    me.readReceiptMaterial(materialId, false, function (data) {
                        dialog.form.setData(data);
                    });
                });
                dialog.scanForm = dialog.body.find('.scanReceipt').JForm({
                    arch: `<form cols="3">
                        <editor name="unit_accuracy" visible="0" type="integer"></editor>
                        <editor label="标签条码" name='sn' colspan="2" placeholder="${'请扫描标签'.t()}" class="lg" type="char"></editor>
                        <editor label="扫描结果" name='message' readonly="1" rowspan="2" type="msg_editor"></editor>
                        <editor label="物料编码" name="material_id" readonly="1" type="many2one"></editor>
                        <editor label="收货仓库" name='warehouse_id' type="many2one" readonly="1"></editor>
                        <editor label="名称规格" name="material_name_spec" readonly="1" colspan="2" type="char"></editor>
                        <editor label="单位" name="unit_id" type="many2one" readonly="1"></editor>
                        <editor label="预收数量" name="request_qty" readonly="1" type="accuracy" accuracy="unit_accuracy"></editor>
                        <editor label="实收数量" name="receive_qty" readonly="1" type="accuracy" accuracy="unit_accuracy"></editor>
                        <editor label="赠品数量" name="gift_qty" readonly="1" type="accuracy" accuracy="unit_accuracy"></editor>
                        <editor label="待收数量" name="left_qty" readonly="1" type="accuracy" accuracy="unit_accuracy"></editor>
                        <editor label="收货数量(含赠品)" name='confirm_qty' type="accuracy" accuracy="unit_accuracy" required="1"></editor>
                        <editor label="赠品数量" name='confirm_gift_qty' type="accuracy" accuracy="unit_accuracy"></editor>
                        <div class="d-flex pt-2 form-group justify-content-between">
                            <div class="e-check" style="align-content: center;">
                                <input id="autoConfirm" type="checkbox" class="form-control"></input>
                                <label for="autoConfirm" title="自动确认">${'自动确认'.t()}</label>
                            </div>
                            <button type="button" role="confirmReceipt" class="btn btn-flat btn-lg btn-success">${'确认'.t()}</button>
                        </div>
                    </form>`,
                    view: me
                });
                dialog.scanForm.editors.sn.dom.on('keyup', 'input', function (e) {
                    if (e.keyCode == 13 && $(this).val()) {
                        me.submitCode(dialog.scanForm);
                    }
                });
                dialog.scanForm.dom.on('click', '[role=confirmReceipt]', function () {
                    if (!dialog.scanForm.valid()) {
                        return jmaa.msg.error(dialog.scanForm.getErrors());
                    }
                    let code = dialog.scanForm.editors.sn.getValue();
                    if (!code) {
                        return jmaa.msg.error('标签条码不能为空'.t());
                    }
                    me.submitCode(dialog.scanForm, true);
                });
            },
            cancel() {
                me.load();
            }
        });
    },
    submitCode(form, confirm) {
        let me = this;
        let code = form.editors.sn.getValue();
        let data = form.getRaw();
        let auto = form.dom.find('#autoConfirm').is(":checked");
        let action = confirm ? 'confirm' : auto ? 'auto' : '';
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'receiptByCode',
            args: {
                ids: [me.form.dataId],
                code,
                action,
                receiveQty: data.confirm_qty,
                giftQty: data.confirm_gift_qty,
            }, onerror(r) {
                if (r.code == 1000) {
                    data.message = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            }, onsuccess: function (r) {
                form.setData(r.data.data);
                form.editors.confirm_qty.readonly(r.data.data.lock_qty);
                data.message = {msg: r.data.message};
                if (!r.data.submit) {
                    data.sn = code;
                    form.editors.sn.dom.find('input').focus();
                }
            }
        });
    },
    reprint(e, target) {
        let me = this;
        let selected = target.getSelectedData();
        let materialId;
        for (let row of selected) {
            if (!materialId) {
                materialId = row.material_id[0];
            }
            if (materialId != row.material_id[0]) {
                return jmaa.msg.error('不同物料不能一起补打'.t());
            }
        }
        jmaa.rpc({
            model: target.model,
            module: me.module,
            method: 'reprint',
            args: {
                ids: target.getSelected(),
            },
            onsuccess: function (r) {
                let data = r.data;
                if (data.template) {
                    return jmaa.print(data);
                }
                jmaa.showDialog({
                    title: '打印'.t(),
                    css: 'modal-sm',
                    init(dialog) {
                        dialog.form = dialog.body.JForm({
                            arch: `<form cols="1">
                                <editor type="float" name="qty" label="总数量" readonly="1"></editor>
                                <editor type="float" name="min_packages" label="标签数量" t-change="onQtyChange" required="1"></editor>
                                <editor type="float" name="print_qty" label="打印数量" t-change="onQtyChange" required="1"></editor>
                                <editor type="float" name="label_count" label="标签张数" readonly="1"></editor>
                            </form>`,
                            view: dialog,
                        });
                        data.print_qty = data.qty;
                        data.label_count = Math.ceil(data.print_qty / data.min_packages);
                        dialog.form.setData(r.data);
                    },
                    onQtyChange(e) {
                        let data = e.owner.getData();
                        if (data.min_packages && data.print_qty) {
                            data.label_count = Math.ceil(data.print_qty / data.min_packages);
                        }
                    },
                    submit(dialog) {
                        if (!dialog.form.valid()) {
                            return jmaa.msg.error(dialog.form.getErrors());
                        }
                        let data = dialog.form.getData();
                        if (data.print_qty > data.qty) {
                            dialog.form.setInvalid('print_qty', '打印数量不能超过总数量');
                            return jmaa.msg.error('打印数量不能超过总数量');
                        }
                        jmaa.rpc({
                            model: target.model,
                            module: me.module,
                            method: 'reprint',
                            args: {
                                ids: target.getSelected(),
                                minPackages: data.min_packages,
                                printQty: data.print_qty,
                            },
                            onsuccess: function (r) {
                                jmaa.print(r.data, function () {
                                    dialog.close();
                                });
                            }
                        });
                    }
                })
            },
        });
    },
    receiptPrint(dialog) {
        let me = this
        let data = dialog.form.getData();
        if (!dialog.form.valid()) {
            return
        }
        if (!me.checkPrint(dialog.form)) {
            return
        }
        let dom = dialog.dom;
        dom.find('[role=print]').attr('disabled', 'disabled');
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'receipt',
            args: {
                ids: [me.form.dataId],
                materialId: data.material_id[0],
                receiveQty: data.commit_qty,
                giftQty: data.gift_qty,
                productDate: data.product_date,
                productLot: data.product_lot,
                lotAttr: data.lot_attr,
                printTplId: data.print_tpl_id[0],
                minPackages: data.min_packages,
                lpn: data.lpn,
                warehouseId: data.warehouse_id[0]
            },
            onerror: function (e) {
                me.form.editors['line_ids'].grid.load();
                dom.find('[role=print]').removeAttr('disabled');
                jmaa.msg.error(e);
            },
            onsuccess: function (r) {
                dom.find('[role=print]').removeAttr('disabled');
                data.deficit_qty = r.data.data.left_qty;
                dialog.form.editors.commit_qty.resetValue();
                jmaa.print(r.data.printData);
                me.form.load();
            },
        });
    },
    checkPrint(form) {
        let me = this;
        let data = form.getData();
        if (data.commit_qty > data.deficit_qty) {
            jmaa.msg.error('实收数量不能大于待收数量');
            form.setInvalid('commit_qty', '实收数量不能大于待收数量');
            return false
        }
        return true;
    },
    onMinPackagesChange(target) {
        let me = this;
        let editors = target.owner.editors;
        let minPackages = editors.min_packages.getValue();
        let commitQty = editors.commit_qty.getValue();
        let giftQty = editors.gift_qty.getValue();
        if (minPackages <= 0) {
            target.owner.setInvalid('min_packages', '必须大于0');
            return;
        }
        if (commitQty > 0) {
            let count = Math.ceil((commitQty + giftQty) / minPackages);
            editors.label_count.setValue(count);
        }
    },
    onCommitQtyChange(target) {
        let me = this;
        let editors = target.owner.editors;
        let minPackages = editors.min_packages.getValue();
        let commitQty = editors.commit_qty.getValue();
        let giftQty = editors.gift_qty.getValue();
        if (commitQty <= 0) {
            target.owner.setInvalid('commit_qty', '必须大于0');
            return;
        }
        if (minPackages <= 0) {
            target.owner.setInvalid('min_packages', '必须大于0');
            return;
        }
        let count = Math.ceil((commitQty + giftQty) / minPackages);
        editors.label_count.setValue(count);
    },
    onToolbarInit(e, bar) {
        let me = this;
        bar.dom.find('#showUndone').on('change', function () {
            me.searchLine();
        });
    },
    createStockIn: function (e, target) {
        let me = this;
        jmaa.showDialog({
            title: '报检'.t(),
            css: 'default',
            init(dialog) {
                dialog.form = me.createCommentForm(dialog);
            },
            submit(dialog) {
                let comment = dialog.form.getData().comment;
                let sel = me.form.editors.line_ids.grid.getSelectedData();
                let materialIds = [];
                for (let row of sel) {
                    materialIds.push(row.material_id[0]);
                }
                jmaa.rpc({
                    model: target.model,
                    module: me.module,
                    method: 'createStockIn',
                    args: {
                        ids: target.getSelected(),
                        materialIds,
                        comment,
                    },
                    onsuccess: function (r) {
                        dialog.close();
                        me.load();
                        if (r.data.exempted) {
                            return jmaa.msg.show(r.data.message, {delay: 8000});
                        }
                        jmaa.msg.show(r.data.message);
                    }
                });
            }
        });
    },
    refresh: function (e, target) {
        let me = this;
        target.owner.load();
    },
    lineFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchInput').val();
        if (keyword) {
            criteria.push(['material_id.code', 'like', keyword])
        }
        let showUndone = target.toolbar.dom.find('#showUndone').is(':checked');
        if (showUndone) {
            criteria.push(['status', 'in', ['new', 'receiving']]);
        }
        return criteria;
    },
    detailsFilter(_, target) {
        let keyword = target.toolbar.dom.find('#searchDetailsInput').val();
        let criteria;
        if (keyword) {
            criteria = ['&', '|', ['material_id.code', 'like', keyword], ['sn', 'like', keyword], [target.field.inverseName, '=', target.owner.dataId]];
        } else {
            criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        }
        return criteria;
    },
    poLineOnLoad(e, grid) {
        let me = this;
        grid.dom.find('div[data-field=warehouse_id]').each(function () {
            let cfg = {
                field: {
                    name: 'warehouse_id',
                },
                model: me.model,
                module: me.module,
                owner: grid,
                view: me.view,
                dom: $(this),
            };
            let ctl = jmaa.editors['many2one'];
            let edt = new ctl(cfg);
        });
        grid.dom.find('div[column=commit_qty]').each(function () {
            let cfg = {
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
            let ctl = jmaa.editors['float'];
            let edt = new ctl(cfg);
        });
    },
    createPallet(e, target) {
        let me = this;
        jmaa.showDialog({
            title: '生成码盘',
            css: 'modal-xs',
            init(dialog) {
                me.rpc("wms.material_receipt_line", "getPalletPacking", {
                    ids: target.getSelected()
                }).then(data => {
                    me.loadView('wms.pallet_print_dialog', 'form').then(v => {
                        dialog.form = dialog.body.JForm({
                            arch: v.views.form.arch,
                            fields: v.fields,
                            module: v.module,
                            model: v.model,
                            view: me,
                        });
                        data.id = target.getSelected()[0];
                        dialog.form.setData(data);
                    });
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    jmaa.msg.error(dialog.form.getErrors());
                    return;
                }
                let data = dialog.form.getRaw();
                jmaa.rpc({
                    model: 'wms.material_receipt_line',
                    module: me.module,
                    method: 'createPallet',
                    args: {
                        ids: [dialog.form.dataId],
                        materialId: data.material_id,
                        packingId: data.packing_level_id,
                        printTplId: data.template_id,
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    },
                    onsuccess(r) {
                        dialog.close();
                        jmaa.print(r.data, function () {
                            me.load();
                        });
                    }
                });
            }
        });
    },
    printPallet(e, target) {
        let me = this;
        jmaa.rpc({
            model: target.model,
            module: me.module,
            method: 'print',
            args: {
                ids: target.getSelected()
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess(r) {
                jmaa.print(r.data);
            }
        });
    }
});
jmaa.column('commit_qty_column', {
    render: function () {
        return function (data, type, row) {
            return `<div data-id="${row.id}" style="min-width: 120px" column="commit_qty" data-decimal="${row.unit_accuracy}"></div>`;
        }
    }
});
jmaa.column('warehouse_column', {
    render: function () {
        return function (data, type, row) {
            return `<div data-id="${row.id}" data-field="warehouse_id"></div>`;
        }
    }
});

jmaa.editor('packing-editor', {
    extends: 'editors.many2one',
    searchRelated(callback, fields) {
        let me = this;
        jmaa.rpc({
            model: "wms.pallet_print_dialog",
            module: me.module,
            method: "searchPalletPacking",
            args: {
                materialId: me.owner.getRaw().material_id,
                fields,
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
});
