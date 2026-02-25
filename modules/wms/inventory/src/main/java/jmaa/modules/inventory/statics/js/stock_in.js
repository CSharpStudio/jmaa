//@ sourceURL=stock_in.js
jmaa.view({
    onToolbarInit(e, tbar) {
        let me = this;
        tbar.dom.on('change', '#statusSelect', function () {
            let editor = me.form.editors.details_ids;
            editor.pager.reset();
            editor.load();
        });
    },
    stockIn: function (e, target) {
        let me = this;
        // 改为弹框,扫码,
        me.form.offset = 0;
        jmaa.showDialog({
            title: '入库单:'.t() + me.form.getData().code,
            init(dialog) {
                jmaa.rpc({
                    model: 'ir.ui.view',
                    method: 'loadView',
                    args: {
                        model: 'stock.stock_in_dialog',
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
                        dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                            if (e.keyCode === 13 && $(this).val()) {
                                me.scanCode(dialog.form);
                            }
                        }).find('input').focus();
                        dialog.form.dom.on('click', '.stock-material', function () {
                            if (!dialog.form.valid()) {
                                return jmaa.msg.error(dialog.form.getErrors());
                            }
                            me.scanCode(dialog.form, true);
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
    scanCode(form, confirm) {
        let me = this;
        let code = form.editors.sn.getValue();
        if (!code) {
            return jmaa.msg.error("请先扫描标签".t());
        }
        let raw = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'stockIn',
            args: {
                ids: [me.form.dataId],
                code,
                submit: confirm || raw.auto_confirm,
                location: raw.location_id,
                materialId: raw.material_id,
                confirmQty: !confirm && raw.auto_confirm ? null : raw.confirm_qty,
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
                let data = r.data.data;
                data.auto_confirm = form.editors.auto_confirm.getRawValue();
                let oldWarehouse = form.editors.warehouse_id.getValue();
                if (data.warehouse_id && oldWarehouse && data.warehouse_id[0] === oldWarehouse[0]) {
                    data.location_id = form.editors.location_id.getValue();
                }
                form.setData(data);
                form.setEditorRequired("location_id", data.location_manage);
                if (r.data.submit) {
                    form.editors.sn.setValue('');
                    form.editors.confirm_qty.resetValue();
                } else {
                    form.editors.sn.setValue(code)
                }
                form.editors.result.setValue({msg: r.data.message});
            }
        });
    },
    refresh: function (e, target) {
        let me = this;
        target.owner.load();
    },
    detailsFilter(criteria, target) {
        let me = this;
        criteria.push(["stock_in_id", "=", me.form.dataId]);
        const status = target.toolbar.dom.find('#statusSelect').val();
        if (status && status !== "all") {
            criteria.push(["status", "=", status]);
        }
        let keyword = target.toolbar.dom.find('#searchDetailsInput').val();
        if (keyword) {
            criteria.push("|");
            criteria.push("|");
            criteria.push(['material_id.code', 'like', keyword]);
            criteria.push(['lot_num', 'like', keyword]);
            criteria.push(['label_id.sn', 'like', keyword]);
        }
        return criteria;
    }
});

// 库位编辑器
jmaa.editor('locationEditor', {
    extends: "editors.many2one",
    filterCriteria() {
        let me = this;
        let criteria = me.callSuper();
        let wh = me.dom.attr('data-wh');
        criteria.push(['warehouse_id', '=', wh])
        return criteria;
    },
});
// 收料明细编辑器
jmaa.editor('stock_in_details_editor', {
    extends: "editors.one2many",
    filterCriteria() {
        let me = this;
        const criteria = [["stock_in_id", "=", view.form.dataId]];
        const status = $('#statusSelect').val();
        if (status && status !== "all") {
            criteria.push(["status", "=", status]);
        }
        return criteria;
    }
});
