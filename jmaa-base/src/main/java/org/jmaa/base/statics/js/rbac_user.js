//@ sourceURL=rbac_user.js
jmaa.view({
    setPassword: function () {
        let me = this;
        jmaa.showDialog({
            title: '设置密码'.t(),
            css: '',
            init: function () {
                let dialog = this;
                dialog.form = dialog.body.JForm({
                    arch: '<form cols="1"><editor name="pwd" type="password" label="密码" required="1"></editor></form>'
                });
            },
            submit: function (dialog) {
                let form = dialog.form;
                if (form.valid()) {
                    let data = form.getData();
                    me.rpc('rbac.user', 'setPassword', {
                        ids: me.curView.getSelected(),
                        password: window.btoa(unescape(encodeURIComponent(data.pwd)))
                    }).then(() => {
                        dialog.close();
                        jmaa.msg.show('操作成功'.t());
                    })
                }
            }
        });
    }
});
