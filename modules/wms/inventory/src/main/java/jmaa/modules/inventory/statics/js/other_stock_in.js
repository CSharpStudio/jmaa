//@ sourceURL=other_stock_in.js
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
            form.editors.details_ids.dom.find('.delete-class').parent().show();
        }
    },
    stockIn() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockIn',
            args: {
                ids: [me.form.dataId]
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.form.load();
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
            criteria.push(['status', 'in', ['saved', 'stocking']]);
        }
        return criteria;
    },
    soFilter(criteria, target) {
        let me = this;
        let value = me.form.editors.customer_id.getRawValue();
        criteria.push(['customer_id', '=', value])
        return criteria;
    },
    materialFilter(criteria, target) {
        let me = this;
        let value = target.owner.editors.so_id.getRawValue();
        criteria.push(['so_id', '=', value])
        return criteria;
    },
    receive() {
        let me = this;
        me.form.offset = 0;
        jmaa.showDialog({
            title: '其它入库单:'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'wms.other_stock_in_dialog',
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
                        dialog.form.editors.warehouse_id.setValue(me.form.editors.warehouse_id.getRawValue());
                        dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode === 13 && $(this).val()) {
                                me.scanCode(dialog.form);
                            }
                        }).find('input').focus();
                        dialog.form.dom.on('click', '.stock-material', function () {
                            me.stockMaterial(dialog.form);
                        }).find('button').each(function () {
                            $(this).html($(this).html().t());
                        });
                    }
                });
            },
            cancel(dialog) {
                me.form.load();
            }
        });
    },
    scanCode(form) {
        let me = this;
        let autoConfirm = form.editors.auto_confirm.getRawValue();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
                autoConfirm: form.editors.auto_confirm.getRawValue(),
                warehouseId: form.editors.warehouse_id.getRawValue(),
                locationId: form.editors.location_id.getRawValue(),
            },
            onerror(r) {
                form.editors.sn.setValue();
                if (r.code === 1000) {
                    form.editors.result.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                if (autoConfirm) {
                    form.editors.sn.setValue('')
                    form.editors.qty.setValue('')
                    form.editors.auto_confirm.setValue(true)
                }
                form.editors.result.setValue({msg: r.data.message});
                form.editors.qty.setReadonly(r.data.data.stock_rule === 'sn' || (r.data.data.stock_rule === 'lot' && r.data.data.lot_in_qty))
            }
        });

    },
    stockMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        if (!data.sn) {
            return jmaa.msg.error('请扫描标签'.t());
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'receive',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                materialId: data.material_id,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
                qty: data.qty
            },
            onerror(r) {
                if (r.code === 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                form.editors.sn.setValue('')
                form.editors.qty.setValue('')
                data.result = {msg: r.data.message};
            }
        });
    },
    deleteDetails(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "wms.other_stock_in_details",
            module: me.module,
            method: 'delete',
            args: {
                ids: grid.selected,
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                me.load()
            }
        });
    },
});
