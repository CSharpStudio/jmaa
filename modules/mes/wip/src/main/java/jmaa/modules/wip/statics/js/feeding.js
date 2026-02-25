//@ sourceURL=feeding.js
jmaa.view({
    feeding() {
        let me = this;
        jmaa.showDialog({
            title: '上料',
            init(dialog) {
                me.loadView("mfg.feeding_dialog", "form").then(r => {
                    dialog.form = dialog.body.JForm({
                        model: r.model,
                        module: r.module,
                        view: me,
                        fields: r.fields,
                        arch: r.views.form.arch
                    });
                    dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                        if (e.keyCode == 13 && $(this).val()) {
                            me.submitFeeding(dialog.form);
                        }
                    }).find('input').focus();
                    me.initFeedingForm(dialog.form);
                });
            },
            cancel() {
                me.reloadFeeding();
            }
        })
    },
    reloadFeeding() {
    },
    submitFeeding(form, qty) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let code = data.sn;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'feeding',
            args: {
                code,
                stationId: data.station_id,
                workOrderId: data.work_order_id,
                qty,
            },
            onsuccess(r) {
                if (r.data.message) {
                    data.msg = {msg: r.data.message};
                    data.sn = '';
                } else if (r.data.material_id) {
                    me.confirmFeeding(form, r.data);
                }
            },
            onerror(r) {
                data.sn = '';
                if (r.code == 1000) {
                    data.msg = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            }
        });
    },
    confirmFeeding(form, data) {
        let me = this;
        jmaa.showDialog({
            title: '上料确认'.t(),
            css: '',
            id: 'confirm-feeding',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor label="物料" name="material_id" type="many2one" readonly="1"></editor>
                            <editor label="批次号" name="lot_num" type="char" readonly="1"></editor>
                            <editor label="数量" name="qty" type="float" requierd="1"></editor>
                        </form>`
                });
                dialog.form.setData(data);
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                me.submitFeeding(form, dialog.form.editors.qty.getValue());
                dialog.close();
            },
        })
    },
    initFeedingForm(form) {
    },
    filterFeedingStation(criteria, station) {
        let me = this;
        let data = station.owner.getRaw();
        if (data.resource_id) {
            criteria.push(['resource_id', '=', data.resource_id]);
        }
        return criteria;
    },
    unload(e, target) {
        let me = this;
        jmaa.showDialog({
            title: '下料',
            init(dialog) {
                me.loadView("mfg.unload_dialog", "form").then(r => {
                    dialog.form = dialog.body.JForm({
                        model: r.model,
                        module: r.module,
                        view: me,
                        fields: r.fields,
                        arch: r.views.form.arch
                    });
                    let data = target.getSelectedData()[0];
                    data.print_label = true;
                    dialog.form.setData(data);
                    dialog.dom.find('[name=submitUnload]').html('下料'.t()).on('click', function () {
                        me.submitUnload(dialog.form);
                    })
                });
            },
            cancel() {
                if (target.reload) {
                    target.reload();
                } else {
                    target.load();
                }
            }
        })
    },
    loadMaterialByCode(e) {
        let me = this;
        let form = e.owner;
        let data = form.getRaw();
        let code = data.code;
        if (!code) {
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'findStationMaterial',
            args: {
                stationId: data.station_id,
                code,
                fields: form.getFields(),
            },
            onsuccess(r) {
                for (let f of form.getFields()) {
                    form.editors[f].setValue(r.data[f]);
                }
                form.dataId = r.data.id;
                data.msg = {msg: "条码[{0}]识别成功".t().formatArgs(code)};
            },
            onerror(r) {
                if (r.code == 1000) {
                    data.msg = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            }
        });
    },
    submitUnload(form) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'unload',
            args: {
                ids: [form.dataId],
                code: data.code,
                qty: data.unload_qty,
                print: data.print_label,
            },
            onsuccess(r) {
                form.editors.code.resetValue();
                form.editors.unload_qty.resetValue();
                form.editors.qty.setValue(r.data.qty);
                data.msg = {msg: r.data.message};
                jmaa.print(r.data.printData);
            }
        });
    },
});
