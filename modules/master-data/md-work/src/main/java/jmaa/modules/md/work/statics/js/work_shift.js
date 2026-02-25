//@ sourceURL=work_shift.js
jmaa.view({
    saveShiftDay(e, grid, dirty) {
        let me = this;
        dirty.shift_id = me.form.dataId;
        if (dirty.id.startsWith('new')) {
            jmaa.rpc({
                model: 'md.work_shift_day',
                module: me.module,
                method: 'create',
                args: dirty,
                onsuccess(r) {
                    me.load();
                    jmaa.msg.show('保存成功'.t());
                },
            });
        } else {
            let id = dirty.id;
            delete dirty.id;
            jmaa.rpc({
                model: 'md.work_shift_day',
                module: me.module,
                method: 'update',
                args: {
                    ids: [id],
                    values: dirty,
                },
                onsuccess(r) {
                    me.load();
                    jmaa.msg.show('保存成功'.t());
                },
            });
        }
    }
});
