//@ sourceURL=return_supplier.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = !['draft', 'reject'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        if (readonly) {
            form.setReadonly(true);
        }
    },
    stockOut() {
        let me = this;
        jmaa.showDialog({
            title: '生成出库单'.t(),
            css: 'default',
            init(dialog) {
                dialog.form = me.createCommentForm(dialog);
            },
            submit(dialog) {
                let comment = dialog.form.getData().comment;
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'stockOut',
                    args: {
                        ids: [me.form.dataId],
                        comment,
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t())
                        me.form.load();
                        dialog.close();
                    }
                });
            }
        });
    },
    //退货按钮
    returns() {
        let me = this;
        if (me.form.editors.line_ids.dirty) {
            jmaa.msg.error('请先保存退货单!'.t());
            return false
        }
        jmaa.showDialog({
            title: `退货单：${me.curView.editors['code'].getValue()}`,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'wms.return_supplier_dialog',
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
                        dialog.form.editors.sn.dom.find('input').focus();
                        dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode == 13 && $(this).val()) {
                                me.scanCode(dialog.form);
                            }
                        });
                        dialog.form.dom.find('button').each(function () {
                            $(this).html($(this).html().t());
                        });
                        dialog.form.dom.on('click', '.btn-return', function () {
                            me.returnMaterial(dialog.form);
                        });
                    }
                });
            },
            cancel(dialog) {
                me.form.load();
            }
        });
    },
    loadReturnLine(materialId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'readReturnMaterial',
            args: {
                ids: [me.form.dataId],
                materialId
            },
            onsuccess: function (r) {
                callback(r.data);
            }
        });
    },
    scanCode(form) {
        let me = this;
        let code = form.editors.sn.getValue()
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.form.dataId],
                code,
            },
            onerror(r) {
                if (r.code == 1000) {
                    form.editors.message.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                if (r.data.action == 'material') {
                    form.setData(r.data.data);
                    form.editors.commit_qty.setReadonly(r.data.data.stock_rule === 'sn' || (r.data.data.stock_rule === 'lot' && r.data.data.lot_out_qty))
                    form.editors.sn.setValue(code);
                    form.editors.message.setValue({msg: r.data.message});
                }
                if (r.data.action == 'split') {
                    form.setData(r.data.data);
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
                    deficit_qty: {name: 'deficit_qty', type: 'float', label: '待退数量', readonly: true},
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
                        form.editors.sn.setValue(r.data.data[0].code);
                        form.editors.qty.setValue(r.data.newQty);

                        let deficitQty = form.editors.deficit_qty.getValue();
                        if (r.data.newQty > deficitQty) {
                            form.editors.commit_qty.setValue(deficitQty);
                        } else {
                            form.editors.commit_qty.setValue(r.data.newQty);
                        }
                        if (form.editors.stock_rule.getRawValue() === 'sn') {
                            form.editors.commit_qty.setReadonly(true);
                        }
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
    returnMaterial(form) {
        let me = this;
        let qty = form.editors.commit_qty.getValue();
        if (qty <= 0) {
            jmaa.msg.error("退货数量必须大于0".t());
            return form.setInvalid('commit_qty', '必须大于0');
        }
        let toQty = form.editors.deficit_qty.getValue();
        if (qty > toQty) {
            jmaa.msg.error("退货数量不能大于待退数量".t());
            return  form.setInvalid('commit_qty', '不能大于待退数量');
        }
        let raw = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'submitScanCode',
            args: {
                ids: [me.form.dataId],
                materialId: raw.material_id,
                warehouseId: raw.warehouse_id,
                locationId: raw.location_id,
                sn: raw.sn,
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
                form.setData({});
                form.onLoad();
                form.editors.message.setValue({msg: r.data.message});
                jmaa.msg.show('操作成功'.t())
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
            criteria.push(['material_id.code', 'like', keyword]);
        }
        let showUndone = target.toolbar.dom.find('#showUndone').is(':checked');
        if (showUndone) {
            criteria.push(['status', 'in', ['saved', 'returning']]);
        }
        return criteria;
    },
    poFilter(criteria, target) {
        let me = this;
        let value = me.form.editors.supplier_id.getRawValue();
        criteria.push(['supplier_id', '=', value])
        return criteria;
    },
    materialFilter(criteria, target) {
        let me = this;
        let value = target.owner.editors.po_id.getRawValue();
        criteria.push(['po_id', '=', value])
        return criteria;
    },
    deleteDetail(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "wms.return_supplier_details",
            module: me.module,
            method: 'deleteDetails',
            args: {
                ids: grid.selected
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.form.load();
            }
        });
    }
});
