//@ sourceURL=work_station_on_duty.js
jmaa.view({
    onDuty() {
        let me = this;
        jmaa.showDialog({
            title: '上岗'.t(),
            init(dialog) {
                me.loadView("mfg.work_station_on_duty_dialog", "form").then(r => {
                    dialog.form = dialog.body.JForm({
                        module: me.module,
                        model: me.model,
                        fields: r.fields,
                        arch: r.views.form.arch,
                        view: me,
                    });
                    dialog.form.dom.on('click', '.btn-on-duty', function () {
                        if (!dialog.form.valid()) {
                            return jmaa.msg.error(dialog.form.getErrors());
                        }
                        let data = dialog.form.getRaw();
                        me.saveOnDuty(data.station_id, data.staff_id, function () {
                            dialog.form.editors.on_duty_ids.load();
                            jmaa.msg.show("操作成功".t());
                        });
                    }).on('click', '.btn-off-duty', function () {
                        let btn = $(this);
                        let dutyId = btn.attr('data-id');
                        me.saveOffDuty(dutyId, function () {
                            dialog.form.editors.on_duty_ids.load();
                            jmaa.msg.show("操作成功".t());
                        });
                    });
                    dialog.form.editors.on_duty_ids.setValue([]);
                    me.rpc(me.model, "currentUserStaff").then(d => {
                        if (d.length == 1) {
                            dialog.form.setData({staff_id: [d[0].id, d[0].name]});
                        }
                    });
                    me.onDutyFormInit(dialog.form);
                });
            },
            cancel(dialog) {
                let value = dialog.form.editors.station_id.getValue();
                me.setOnDutyStation(value);
            }
        })
    },
    onDutyFormInit(form) {
    },
    setOnDutyStation(station) {
    },
    saveOnDuty(stationId, staffId, callback) {
        let me = this;
        jmaa.rpc({
            module: me.module,
            model: me.model,
            method: 'onDuty',
            args: {
                stationId,
                staffId,
            },
            onsuccess() {
                callback();
            },
        });
    },
    saveOffDuty(dutyId, callback) {
        let me = this;
        jmaa.rpc({
            module: me.module,
            model: me.model,
            method: 'offDuty',
            args: {
                dutyId
            },
            onsuccess() {
                callback();
            },
        });
    },
    filterOnDutyStation(criteria, station) {
        let me = this;
        let data = station.owner.getRaw();
        if (data.resource_id) {
            criteria.push(['resource_id', '=', data.resource_id]);
        }
        return criteria;
    },
    filterStationOnDuty(criteria, onDuty) {
        let me = this;
        let data = onDuty.owner.getRaw();
        return [['station_id', '=', data.station_id]];
    },
});
