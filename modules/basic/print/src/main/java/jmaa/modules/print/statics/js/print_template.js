//@ sourceURL=print_template.js
jmaa.view({
    designTemplate(e, target) {
        let me = this;
        me.busy(true);
        let tpl = target.getSelectedData ? target.getSelectedData()[0] : target.getRaw();
        if (tpl.adapter === 'print.adapter.btw' || tpl.adapter == 'print.adapter.client') {
            jmaa.rpc({
                model: "print.template",
                method: "designate",
                args: {
                    ids: [tpl.id],
                },
                onsuccess: function (r) {
                    let params = r.data
                    let data = {
                        "type": params.type,
                        "title": params.title,
                        "data": params.data,
                        "design": true,
                    }
                    if (params.hash) {
                        data.hash = params.hash;
                        data.template = `${window.location.origin}${params.template}`
                    }
                    $.ajax({
                        type: "post",
                        url: 'http://localhost:8085/api/design',
                        data,
                        dataType: "json",
                        success: function (responseData) {
                            me.busy(false);
                        },
                        error: function (e) {
                            me.busy(false);
                            jmaa.msg.error('打印服务调用失败'.t());
                        }
                    })
                }
            });
        } else if (tpl.adapter === 'print.adapter.stimulsoft') {
            jmaa.rpc({
                model: "print.template",
                method: "designate",
                args: {
                    ids: [tpl.id],
                },
                onsuccess: function (r) {
                    let params = r.data;
                    let json = JSON.stringify({
                        data: params.data,
                        template: params.template ? `${window.location.origin}${params.template}` : null,
                        title: tpl.name,
                        id: tpl.id,
                        tenant: jmaa.web.getTenantPath(),
                    });
                    let base64 = encodeURIComponent(json);
                    window.open(`/web/jmaa/modules/print/statics/report/designer.html?d=${base64}`, '_blank');
                    me.busy(false)
                }
            });
        }
    },
    uploadTemplate() {
        let me = this;
        let upload = me.dom.find('#upload');
        if (!upload.length) {
            upload = $('<input type="file" name="upload" id="upload" style="display: none;"/>');
            upload.on('change', function (e) {
                me.fileChange(e);
            });
            me.dom.append(upload);
        }
        upload.trigger('click');
    },
    downloadTemplate(e, target) {
        let me = this;
        let id = target.getSelected()[0];
        jmaa.rpc({
            model: "print.template",
            method: "read",
            args: {
                ids: [id],
                fields: ['file']
            },
            onsuccess: function (r) {
                let get = (obj, path) => path.reduce((current, key) => current == null ? undefined : current[key], obj);
                let file = get(r, ['data', 0, 'file', 0]);
                if (file) {
                    let data = file.data;
                    let decodedBinary = atob(data);
                    let binaryLength = decodedBinary.length;
                    let uint8Array = new Uint8Array(binaryLength);
                    for (let i = 0; i < binaryLength; i++) {
                        uint8Array[i] = decodedBinary.charCodeAt(i);
                    }
                    let blob = new Blob([uint8Array], {type: "application/stream"});
                    let url = URL.createObjectURL(blob);
                    let a = document.createElement("a");
                    a.href = url;
                    a.download = file.name;
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                    URL.revokeObjectURL(url);
                }
            }
        });
    },
    fileChange(e) {
        let me = this;
        let formData = new FormData()
        formData.set('file', e.target.files[0])
        formData.set("id", me.curView.getSelected()[0]);
        $.ajax({
            url: jmaa.web.getTenantPath() + "/print/upload",
            type: "POST",
            data: formData,
            contentType: false,
            processData: false,
            success: function (data) {
                jmaa.msg.show('操作成功'.t())
                $('#upload').val('');
                me.curView.load();
            },
            error: function (data) {
                $(document).Toasts('create', $.extend({
                    class: 'msg bg-danger',
                    title: '网络错误'.t(),
                    icon: 'iconfont icon-exclamation-circle',
                    autohide: true,
                    delay: 3000,
                }));
            }
        });
    },
});
