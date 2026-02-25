//@ sourceURL=print.js
jmaa.define('print.btw', {
    print(printData, onPrinted) {
        printData.template = window.location.origin + printData.template;
        $.ajax({
            type: "post",
            url: 'http://localhost:8085/api/print',
            data: printData,
            dataType: "json",
            success: function (responseData) {
                onPrinted && onPrinted();
                jmaa.msg.show('操作成功'.t());
            },
            error: function (e) {
                onPrinted && onPrinted();
                jmaa.msg.error('打印服务调用失败'.t());
            }
        })
    }
});
jmaa.define('print.lpt', {
    extends: 'print.btw'
});
jmaa.define('print.sti', {
    print(printData, onPrinted) {
        window.reportData = {
            data: printData.data,
            template: printData.template
        };
        window.printDialog = jmaa.showDialog({
            title: '打印中'.t(),
            id: 'print-dialog',
            css: 'default',
            init(dialog) {
                dialog.body.html(`<div class="p-5">${'正在打印中，请稍等'.t()}</div>`);
            }
        })
        $('body div.printer').remove();
        $('body').append(`<div class="printer d-none"><iframe src="/web/jmaa/modules/print/statics/report/print.html"/></div>`);
        onPrinted && onPrinted();
    }
});
jmaa.print = function (data, onPrinted) {
    jmaa.create('print.' + data.type).print(data, onPrinted);
}
