//@ sourceURL=material_label_status.js
jmaa.column('sn_column', {
    render: function () {
        let me = this;
        return function (data, type, row) {
            if (data != undefined && me.field.name === 'sn') {
                return `<span style="color: #0000FF;">${data}</span>`;
            }
        }
    }
});

jmaa.column('quality_status_column', {
    render: function () {
        let me = this;
        return function (data, type, row) {
            let value = me.field.options[data];
            let colors = {
                pass: 'green',
                fail: 'red',
                red: 'red',
                'over-due': '#ffb600'
            }
            return value ? `<span style="color: ${colors[data]};padding: 5px 10px;">${value}</span>` : data;
        }
    }
});

jmaa.column('related_column', {
    render: function () {
        let me = this;
        me.owner.dom.on('click', '.link-code', function () {
            let rowId = $(this).attr('data-id');
            let row = me.owner.data.find(r => r.id == rowId);
            let model = row.operation.split(":")[0];
            let op = me.owner.fields.operation.options[row.operation];
            let code = $(this).html();
            jmaa.showDialog({
                title: op + ":" + code,
                init(dialog) {
                    dialog.body.html(`<iframe style="width: 100%; border: 0px; height: ${$(document.body).height() - 300}px;" src="${jmaa.web.getTenantPath()}/view#model=${model}&id=${row.related_id}&views=form&top=1&readonly=1" height="500px"/>`)
                }
            });
        });
        return function (data, type, row) {
            return `<a style="cursor: pointer" class="link-code" data-id="${row.id}">${data}</a>`
        }
    }
});
