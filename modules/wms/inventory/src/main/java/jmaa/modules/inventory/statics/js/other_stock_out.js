//@ sourceURL=other_stock_out.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = !['draft', 'commit', 'reject'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        if (readonly) {
            form.setReadonly(true);
        }
        if (status !== 'done') {
            form.editors.details_ids.dom.find('.delete-class').parent().show();
        }
    },
    onToolbarInit(e, bar) {
        let me = this;
        bar.dom.find('#showUndone').on('change', function () {
            me.searchLine();
        });
    },
    searchLine() {
        let me = this;
        me.form.editors.line_ids.load();
    },
    lineFilter(_, target) {
        let criteria = [[target.field.inverseName, '=', target.owner.dataId]];
        let keyword = target.toolbar.dom.find('#searchInput').val();
        if (keyword) {
            criteria.push(['material_id.code', 'like', keyword])
        }
        let showUndone = target.toolbar.dom.find('#showUndone').is(':checked');
        if (showUndone) {
            criteria.push(['status', 'in', ['new', 'delivering']]);
        }
        return criteria;
    },
    // 发货
    delivery() {
        let me = this;
        me.form.offset = 0;

        jmaa.showDialog({
            title: '发料单:'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'wms.other_stock_out_dialog',
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
                        dialog.dom.find('.buttons-right').html(`<button type="button" class="btn btn-default prev-material">${'上一个'.t()}</button>
                                                                    <button type="button" class="btn btn-default next-material">${'下一个'.t()}</button>`);
                        dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode == 13 && $(this).val()) {
                                me.submitCode(dialog.form);
                            }
                        }).find('input').focus();
                        dialog.form.dom.on('click', '.delivery-material', function () {
                            me.submitMaterial(dialog.form);
                        }).find('button').each(function () {
                            $(this).html($(this).html().t());
                        });
                        dialog.dom.on('click', '.next-material', function () {
                            me.form.offset++;
                            me.loadStockOutMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                                dialog.form.setData(data);
                                dialog.form.editors.result.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                            });
                        }).on('click', '.prev-material', function () {
                            me.form.offset--;
                            me.loadStockOutMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                                dialog.form.setData(data);
                                dialog.form.editors.result.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                            });
                        });
                        me.loadStockOutMaterial(me.form.editors.warehouse_id.getRawValue(), function (data) {
                            dialog.form.setData(data);
                        });
                    }
                });
            },
            cancel(dialog) {
                me.form.load();
            }
        });
    },
    loadStockOutMaterial(warehouseId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadStockOutMaterial',
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
    // 扫码返回物料信息,序列号直接提交
    submitCode(form) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'delivery',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
            },
            onerror(r) {
                form.editors.sn.setValue();
                if (r.code == 1000) {
                    form.editors.result.setValue({error: true, msg: r.message});
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.editors.result.setValue({msg: r.data.message});
                form.setData(r.data.data);
                if (r.data.action == 'split') {
                    me.splitLabel(form, r.data.split);
                }
                if ('sn' === form.editors.stock_rule.getRawValue()) {
                    form.editors.sn.setValue();
                }
                form.editors.scan_qty.setReadonly(r.data.data.stock_rule === 'lot' && r.data.data.lot_out_qty)
            }
        });
    },
    // 批次数量确认提交
    submitMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockOutMaterial',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                materialId: data.material_id,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
                qty: data.scan_qty,
                printFlag: data.print_flag,
                templateId: data.template_id,
            },
            onerror(r) {
                if (r.code == 1000) {
                    data.result = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            },
            onsuccess: function (r) {
                form.setData(r.data.data);
                data.result = {msg: r.data.message};
                let printMap = r.data.printMap
                if (printMap && printMap.data) {
                    jmaa.print(printMap);
                }
            }
        });
    },
    splitLabel(form, splitData) {
        let me = this;
        jmaa.showDialog({
            id: 'label-split',
            css: 'modal-lg',
            title: `拆分标签：${form.editors.sn.getValue()}`,
            init(dialog) {
                dialog.splitForm = dialog.body.JForm({
                    model: me.model,
                    module: me.module,
                    arch: `<form cols="3">
                                <editor name="sn" type="char" label="标签条码" readonly="1"></editor>
                                <editor name="qty" type="float" label="标签数量" readonly="1" decimals="${splitData.accuracy}"></editor>
                                <editor name="to_delivery_qty" type="float" label="待发数量" readonly="1" decimals="${splitData.accuracy}"></editor>
                                <editor name="split_qty" type="float" label="拆分数量" decimals="${splitData.accuracy}"></editor>
                                <editor name="print_old" type="boolean" label="打印原标签"></editor>
                            </form>`,
                    view: me
                });
                dialog.splitForm.setData(splitData);
            },
            submit(dialog) {
                let data = dialog.splitForm.getRaw();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'splitLabel',
                    dialog,
                    args: {
                        sn: data.sn,
                        splitQty: data.split_qty,
                        printOld: data.print_old
                    },
                    onsuccess: function (r) {
                        form.editors.sn.setValue(r.data.newSn);
                        form.editors.result.setValue({msg: '拆分新标签:'.t() + r.data.newSn});
                        me.submitCode(form);
                        jmaa.print(r.data, () => {
                            dialog.busy(false);
                            dialog.close();
                        });
                    }
                });
            }
        });
    },
    deleteDetails(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "wms.other_stock_out_details",
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
});
