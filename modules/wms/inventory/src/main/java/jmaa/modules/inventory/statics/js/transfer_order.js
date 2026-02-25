//@ sourceURL=transfer_order.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        let readonly = !['draft', 'reject'].includes(status);
        me.toolbar.dom.find('button[name=save]').attr('disabled', readonly);
        form.setReadonly(readonly);
        if (readonly) {
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
    //调拨按钮
    transfer() {
        let me = this;
        me.form.offset = 0;
        me.loadTransferMaterial(null, function (data) {
            let setData = function (dialog, data) {
                dialog.form.setData(data);
                for (let e of ['transfer_qty', 'to_transfer_qty', 'onhand_qty']) {
                    dialog.form.editors[e].setAttr('data-decimals', data.accuracy);
                }
            }
            jmaa.showDialog({
                title: '调拨单:'.t() + me.form.getData().code,
                init(dialog) {
                    jmaa.rpc({
                        model: 'ir.ui.view',
                        method: 'loadView',
                        args: {
                            model: 'wms.transfer_order_dialog',
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
                            setData(dialog, data);
                            dialog.form.editors.transfer_qty.setReadonly(data.stock_rule === 'lot' && data.lot_out_qty)
                            dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                                if (e.keyCode == 13 && $(this).val()) {
                                    me.submitCode(dialog.form);
                                }
                            }).find('input').focus();
                            dialog.form.dom.find('t').each(function () {
                                let el = $(this);
                                el.replaceWith(el.text().t());
                            });
                        }
                    });
                },
                transferMaterial(e, dialog) {
                    me.transferMaterial(dialog.form);
                },
                prevMaterial(e, dialog) {
                    me.form.offset--;
                    me.loadTransferMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                        setData(dialog, data);
                        dialog.form.editors.result.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                    });
                },
                nextMaterial(e, dialog) {
                    me.form.offset++;
                    me.loadTransferMaterial(dialog.form.getRaw().warehouse_id, function (data) {
                        setData(dialog, data);
                        dialog.form.editors.result.setValue({msg: '加载物料:'.t() + data.material_id[1]});
                    });
                },
                cancel(dialog) {
                    me.form.load();
                }
            });
        });
    },
    loadTransferMaterial(warehouseId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadTransferMaterial',
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
    transferMaterial(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'transferMaterial',
            args: {
                ids: [me.form.dataId],
                code: data.sn,
                materialId: data.material_id,
                warehouseId: data.warehouse_id,
                locationId: data.location_id,
                qty: data.transfer_qty,
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
    submitCode(form) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'transfer',
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
                form.editors.transfer_qty.setReadonly(r.data.data.stock_rule === 'lot' && r.data.data.lot_out_qty)
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
                                <editor name="to_transfer_qty" type="float" label="待调拨数量" readonly="1" decimals="${splitData.accuracy}"></editor>
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
            criteria.push(['status', 'in', ['new', 'transfering']]);
        }
        return criteria;
    },
    filterWarehouse(criteria) {
        let me = this;
        let values = me.form.editors.source_warehouse_ids.getRawValue();
        criteria.push(['id', 'in', values])
        return criteria;
    },
    printTransferOrder() {
        const me = this;
        me.busy(true);
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "printTransferOrder",
            args: {
                ids: me.curView.getSelected()
            },
            onsuccess: function (r) {
                jmaa.print(r.data, () => {
                    me.busy(false);
                });
            },
            onerror: function (r) {
                me.busy(false)
                jmaa.msg.error(r);
            }
        });
    },
    deleteDetails(e, grid) {
        let me = this;
        jmaa.rpc({
            model: "wms.transfer_order_details",
            module: me.module,
            method: 'deleteDetails',
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
    }
})
