//@ sourceURL=qc_iqc_sheet.js
jmaa.view({
    commitOrder(e, target) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            method: 'commit',
            args: {
                ids: target.getSelected(),
                values: target.getSubmitData()
            },
            onerror: function (e) {
                jmaa.msg.error(e);
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t());
                me.load();
            }
        });
    },
    onFormLoad(e, form) {
        let me = this;
        me.form.dom.find('.inspect_mark').remove();
        let data = form.getData();
        if (data.status == 'exempted') {
            me.showMark("exempted", '免检'.t());
        } else if (data.status == 'inspected' || data.status == 'done') {
            let result = data.result;
            me.showMark(result[0], result[1].t());
        }
    },
    showMark(mark, text) {
        let me = this;
        me.form.dom.find('.form-card').append(`
                <div class="inspect_mark ${mark}">
                    <img class="img" src="/web/jmaa/modules/wms/qc/statics/img/${mark}.svg">
                    <div class="text">
                      <span>${text}</span>
                    </div>
                </div>`);
    }
});
