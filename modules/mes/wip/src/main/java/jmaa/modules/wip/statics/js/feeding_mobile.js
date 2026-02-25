//@ sourceURL=feeding_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.dom.find('.code-input').focus();
    },
    onStationChange(station) {
        let me = this;
        let value = station.getValue();
        me.dom.find('.code-input').attr('placeholder', value ? '请扫描物料标签'.t() : '请扫描工位'.t());
        if (me.stockList) {
            me.stockList.load();
        }
    },
    loadStockList(list, callback) {
        let me = this;
        let data = me.stationForm.getRaw();
        let code = me.dom.find('.stock-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchStock',
            args: {
                stationId: data.station_id,
                code,
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
    searchStock() {
        let me = this;
        me.stockList.load();
    },
    submitCode() {
        let me = this;
        let code = me.dom.find('.code-input').val();
        if (!code) {
            return;
        }
        let data = me.stationForm.getRaw();
        if (!data.station_id) {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'loadStation',
                args: {
                    code,
                },
                onsuccess(r) {
                    me.mediaScan.play();
                    me.dom.find('.code-input').val('');
                    me.stationForm.setData(r.data);
                    me.stationForm.triggerChange("station_id");
                    me.stockList.load();
                },
                onerror(r) {
                    me.mediaError.play();
                    jmaa.msg.error(r);
                }
            });
        } else {
            me.submitFeeding(me.stationForm)
        }
    },
    submitFeeding(form, qty) {
        let me = this;
        if (!form.valid()) {
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let code = me.dom.find('.code-input').val();
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
                me.mediaScan.play();
                if (r.data.message) {
                    data.msg = {msg: r.data.message};
                    me.dom.find('.code-input').val('');
                } else if (r.data.material_id) {
                    me.confirmFeeding(form, r.data);
                }
            },
            onerror(r) {
                me.mediaError.play();
                me.dom.find('.code-input').val('');
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
                dialog.form.dom.enhanceWithin();
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
    filterFeedingStation(criteria, station) {
        let me = this;
        let data = station.owner.getRaw();
        if (data.resource_id) {
            criteria.push(['resource_id', '=', data.resource_id]);
        }
        return criteria;
    },
    unload(e) {
        let me = this;
        me.changePage("unload");
        let id = $(e.target).closest("[data-id]").attr('data-id');
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'loadWorkStationMaterial',
            args: {
                dataId: id,
                fields: me.unloadForm.getFields(),
            },
            onsuccess(r) {
                r.data.print_label = Boolean(me.unloadForm.getData().print_label);
                me.unloadForm.setData(r.data);
            }
        });
    },
    submitUnloadCode() {
        let me = this;
        let code = me.dom.find('.unload-code').val();
        if (!code) {
            return;
        }
        let form = me.unloadForm;
        let data = form.getRaw();
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
                me.mediaScan.play();
                for (let f of form.getFields()) {
                    form.editors[f].setValue(r.data[f]);
                }
                form.dataId = r.data.id;
                data.msg = {msg: "条码[{0}]识别成功".t().formatArgs(code)};
            },
            onerror(r) {
                me.mediaError.play();
                if (r.code == 1000) {
                    data.msg = {error: true, msg: r.message};
                } else {
                    jmaa.msg.error(r);
                }
            }
        });
    },
    submitUnload() {
        let me = this;
        let form = me.unloadForm;
        if(!form.valid()){
            return jmaa.msg.error(form.getErrors());
        }
        let data = form.getRaw();
        let code = me.dom.find('.unload-code').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'unload',
            args: {
                ids: [form.dataId],
                code,
                qty: data.unload_qty,
                print: data.print_label,
            },
            onsuccess(r) {
                me.dom.find('.unload-code').val('');
                form.editors.unload_qty.resetValue();
                form.editors.qty.setValue(r.data.qty);
                data.msg = {msg: r.data.message};
                if (data.print_label) {
                    jmaa.print(r.data.printData);
                }
            },
            onerror(r) {
                me.mediaError.play();
                if (r.code == 1000) {
                    data.msg = {error: true, msg: r.data.message};
                } else {
                    jmaa.msg.error(r);
                }
            }
        });
    },
});
