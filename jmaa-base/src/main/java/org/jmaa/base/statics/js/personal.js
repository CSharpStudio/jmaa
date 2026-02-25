//@ sourceURL=personal.js
jmaa.view({
    onFormInit(e, form) {
        let me = this;
        form.ajax = function (form, callback) {
            me.rpc('rbac.user', 'getPersonal', {
                fields: form.getFields()
            }, {
                usePresent: form.getUsePresent()
            }).then(data => {
                callback({data})
            });
        }
    },
    async saveData() {
        let me = this, form = me.form;
        if (form.valid()) {
            let data = form.getSubmitData();
            await me.rpc('rbac.user', 'updatePersonal', {
                values: data,
            });
            jmaa.msg.show('操作成功'.t());
            form.load();
        }
    },
    updatePwd: function () {
        let me = this;
        jmaa.showDialog({
            title: '修改密码'.t(),
            css: '',
            init: function () {
                let dialog = this;
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                                <editor name="old_pwd" type="password" label="原密码" required="1"></editor>
                                <editor name="new_pwd" type="password" label="新密码" required="1"></editor>
                                <editor name="cfm_pwd" type="password" label="确认密码" required="1"></editor>
                           </form>`
                });
            },
            submit: function () {
                let dialog = this;
                me.postUpdatePwd(dialog);
            }
        });
    },
    postUpdatePwd: function (dialog) {
        let me = this, form = dialog.form;
        if (form.valid()) {
            let d = form.getData();
            if (d.old_pwd === d.new_pwd) {
                form.setInvalid('new_pwd', '新密码不能跟原密码相同'.t());
                return;
            }
            if (d.cfm_pwd != d.new_pwd) {
                form.setInvalid('cfm_pwd', '确认密码与新密码不一致'.t());
                return;
            }
            me.rpc('rbac.user', 'changePassword', {
                oldPassword: window.btoa(unescape(encodeURIComponent(d.old_pwd))),
                newPassword: window.btoa(unescape(encodeURIComponent(d.new_pwd)))
            }).then(() => {
                jmaa.msg.show('操作成功'.t());
                dialog.close();
            });
        }
    }
});
jmaa.editor('user-company', {
    extends: 'editors.selection',
    filterData(keyword) {
        let me = this;
        if (keyword) {
            me.callSuper(keyword);
        } else {
            me.dom.find('.dropdown-select ul').html(`<li>${'加载中'.t()}</li>`);
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: "getUserCompanies",
                args: {},
                onsuccess(r) {
                    if (r.data && r.data.length) {
                        for (let data of r.data) {
                            me.options[data.id] = data.present;
                        }
                        let selected = me.dom.attr('data-value');
                        let options = [];
                        for (const key in me.options) {
                            options.push(`<li class="options${selected == key ? ' selected' : ''}" value="${key}">${me.options[key]}</li>`);
                        }
                        me.dom.find('.dropdown-select ul').html(options.join(''));
                    } else {
                        me.dom.find('.dropdown-select ul').html(`<li>${'没有数据'.t()}</li>`);
                    }
                }
            });
        }
    },
});
