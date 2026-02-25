//@ sourceURL=order_status.js
jmaa.view({
    rejectOrder(e, target) {
        this.changeStatus(target.getSelected(), '驳回', 'reject');
    },
    approveOrder(e, target) {
        this.changeStatus(target.getSelected(), '审核', 'approve');
    },
    closeOrder(e, target) {
        this.changeStatus(target.getSelected(), '关闭', 'close', true);
    },
    reopenOrder(e, target) {
        this.changeStatus(target.getSelected(), '重新修改', 'reopen', true);
    },
    commitOrder(e, target) {
        this.saveAndChangeStatus(target, "提交", 'commit');
    },
    saveAndChangeStatus(target, title, method, required) {
        let me = this;
        let ids = target.getSelected();
        if (ids.length == 0) {
            return;
        }
        if (target.valid && !target.valid()) {
            return jmaa.msg.error(target.getErrors());
        }
        jmaa.showDialog({
            title: title.t(),
            css: 'default',
            init(dialog) {
                dialog.form = me.createCommentForm(dialog, required);
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let values = target.dataId ? target.getSubmitData() : null;
                jmaa.rpc({
                    model: me.model,
                    method,
                    dialog,
                    args: {
                        ids: ids,
                        comment: dialog.form.getData().comment,
                        values
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                        me.load();
                    }
                });
            }
        });
    },
    createCommentForm(dialog, required) {
        return dialog.body.JForm({
            cols: 1,
            fields: {
                comment: {name: 'comment', type: 'text', label: '备注', required}
            },
            arch: `<form><field name="comment"></field></form>`
        });
    },
    changeStatus(ids, title, method, required) {
        let me = this;
        jmaa.showDialog({
            title: title.t(),
            css: 'default',
            init(dialog) {
                dialog.form = me.createCommentForm(dialog, required);
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getData();
                jmaa.rpc({
                    model: me.model,
                    method: method,
                    dialog,
                    args: {
                        ids: ids,
                        comment: data.comment,
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                        me.load();
                    }
                });
            }
        });
    },
});

jmaa.column('status_column', {
    render: function () {
        let me = this;
        let css = {
            'commit': 'success',//待审核
            'approve': 'info',//已审核
            'draft': 'warning',//草稿
            'reject': 'danger',//驳回
            'close': 'secondary',//关闭
            'done': 'secondary',//完成
        };
        return function (data, type, row) {
            let text = me.field.options[data];
            if (text) {
                return `<span data-value="${data}" class="tags tag-${css[data]}"">${text}</span>`;
            }
            return data;
        }
    }
});
