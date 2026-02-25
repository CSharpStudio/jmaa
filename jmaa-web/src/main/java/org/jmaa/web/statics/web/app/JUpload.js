jmaa.component("JUpload", {
    init() {
        let me = this;
        me.dom.on('click', () => me.open());
        me.load();
    },
    open() {
        app.ws.openTab("导入记录".t(), jmaa.web.getTenantPath() + "/view#model=res.upload&views=grid&view=grid&app=base");
    },
    add() {
        let me = this;
        me.dom.show();
        let flyer = $('<i style="color:red;z-index: 10000" class="fa fa-futbol"/>');
        flyer.fly({
            start: {
                left: window.innerWidth - 120,
                top: 600,
            },
            end: {
                left: me.dom.offset().left + 10,
                top: me.dom.offset().top,
            },
            onEnd() {
                me.dom.addClass("uploading");
                me.load();
                flyer.remove();
            }
        });
    },
    load() {
        let me = this;
        $.ajax({
            url: jmaa.web.getTenantPath() + '/upload/polling',
            type: 'GET',
            contentType: 'json',
            success(rs) {
                console.log(rs)
                if (rs == -1) {
                    me.dom.hide();
                    return;
                }
                if (rs.running > 0) {
                    me.dom.find('.badge').html(rs.running);
                    me.dom.addClass("uploading");
                    me.dom.find('i').toggleClass('toggle');
                    setTimeout(() => me.load(), 5000);
                } else {
                    me.dom.removeClass("uploading");
                    me.dom.find('.badge').html('');
                    me.dom.hide();
                }
                if (rs.stop.length > 0) {
                    let msg = '', error = '';
                    for (let r of rs.stop) {
                        if (r.success) {
                            msg += r.title + "->" + r.message + "\r\n";
                        } else {
                            error += r.title + "->" + r.message + "\r\n";
                        }
                    }
                    if (msg) {
                        jmaa.msg.show(msg, {position: 'topRight', delay: 30000, title: '操作成功'.t()});
                    }
                    if (error) {
                        jmaa.msg.error(error, {delay: 30000, title: '导入失败'.t()});
                    }
                }
            }
        });
    },
});
