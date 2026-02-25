//@ sourceURL=code_coding.js
jmaa.view({
    onCreatePart(values, form) {
        let seq = 1;
        if (form.owner.data) {
            for (let row of form.owner.data) {
                if (Number(row.seq) >= seq) {
                    seq = Number(row.seq) + 1;
                }
            }
        }
        return {seq}
    },
    moveUp(e, target) {
        target.moveUp("seq");
    },
    moveDown(e, target) {
        target.moveDown("seq");
    },
    //设置默认
    setDefault(e, target) {
        let dataId = target.owner.owner.dataId
        if (dataId === undefined || dataId === '') {
            return jmaa.msg.show("请保存编码规则".t());
        }
        let selectIds = target.getSelected();
        let me = target.owner;
        let selectId = selectIds[0]
        this.updateDefault(me, selectId, true);
    },
    //取消默认
    cancelDefault(e, target) {
        let dataId = target.owner.owner.dataId
        if (dataId === undefined || dataId === '') {
            jmaa.msg.show("请保存编码规则".t())
            return;
        }
        let selectIds = target.getSelected();
        let me = target.owner;
        let selectId = selectIds[0]
        this.updateDefault(me, selectId, false)
    },
    updateDefault(me, selectId, status) {
        let templateList = me.data
        for (let i = 0; i < templateList.length; i++) {
            let template = templateList[i]
            let id = template.id
            if (id === selectId) {
                template.is_default = status;
            } else if (status) {
                template.is_default = !status;
            }
            if (id.startsWith('new')) {
                me.removeDataById(me.create, id);
                me.create.push(template);
                me.dom.triggerHandler("valueChange", [me]);
            } else {
                me.removeDataById(me.update, id);
                me.update.push(template);
                me.dom.triggerHandler("valueChange", [me]);
            }
        }
        //重新渲染grid表格
        me.updateGrid();
    }
})

jmaa.editor("part-code", {
    extends: 'editors.selection',
    init() {
        let me = this;
        me.options = {};
        me.callSuper();
        me.dom.on('openChange', function () {
            if (me.open) {
                me.lookup();
            }
        })
    },
    getRawValue(){
        return this.getValue();
    },
    lookup() {
        let me = this;
        let rule = me.view.form.editors.rule.getRawValue();
        if (!rule) {
            return jmaa.msg.error('请选择编码规则类型'.t());
        }
        me.dom.find('.dropdown-select ul').html(`<li class="m-2 text-center">${'加载中'.t()}</li>`);
        jmaa.rpc({
            model: "code.coding",
            module: me.view.module,
            method: "getCodeParts",
            args: {
                rule
            },
            onsuccess(r) {
                if (r.data) {
                    me.options = r.data;
                    let selected = me.dom.attr('data-value');
                    let options = [];
                    for (const key in me.options) {
                        options.push(`<li class="options${selected == key ? ' selected' : ''}" value="${key}">${me.options[key]}</li>`);
                    }
                    me.dom.find('.dropdown-select ul').html(options.join(''));
                } else {
                    me.dom.find('.dropdown-select ul').html(`<li class="m-2 text-center">${'没有数据'.t()}</li>`);
                }
            }
        });
    },
});

jmaa.column('part-code', {
    render() {
        return function (data) {
            if (data && data[0]) {
                return `<span data-value="${data[0]}">${data[1]}</span>`;
            }
            return '';
        }
    }
});

/**
 * 编码规则
 */
jmaa.editor("part-content", {
    extends: 'editors.char',
    getTpl() {
        return `<div class="input-group">
                    <input type="text" class="form-control">
                    <div class="input-suffix">
                        <button type="button" class="btn btn-default">
                            <i class="fas fa-edit"></i>
                        </button>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).on('click', 'button', function (e) {
            if (me.readonly()) {
                return;
            }
            me.edit();
        }).on('keydown', 'input', function (e) {
            if (e.key != 'Tab') {
                e.preventDefault();//禁止输入，使用readonly或者disabled样式冲突
            }
        });
    },
    edit() {
        let me = this;
        let part_code = me.owner.editors.part_code.getValue();
        let content = me.owner.editors.content.getValue();
        if (!part_code) {
            jmaa.msg.error("请先选择编码段算法".t());
            return;
        }
        jmaa.showDialog({
            title: '编码组成规则',
            css: 'modal-lg',
            init(dialog) {
                jmaa.rpc({
                    model: "ir.ui.view",
                    module: "base",
                    method: "loadView",
                    args: {
                        model: part_code[0],
                        type: 'form'
                    },
                    onsuccess(r) {
                        dialog.form = dialog.body.JForm({
                            model: r.data.model,
                            module: r.data.module,
                            fields: r.data.fields,
                            arch: r.data.views.form.arch,
                        });
                        if (content) {
                            let data = $.parseJSON(content);
                            dialog.form.setData(data);
                        }else{
                            dialog.form.create();
                        }
                        dialog.form.load();
                    }
                });
            },
            submit(dialog) {
                dialog.busy(true);
                if (dialog.form.valid()) {
                    let data = $.extend({}, dialog.form.getRaw());
                    delete data.id;
                    me.dom.find('input').val(JSON.stringify(data)).trigger('change');
                    dialog.close();
                }
            }
        });
    }
});
