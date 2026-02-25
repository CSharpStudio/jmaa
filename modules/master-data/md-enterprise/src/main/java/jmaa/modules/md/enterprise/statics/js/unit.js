//@ sourceURL=unit.js
jmaa.editor('unit-conversion', {
    css: 'e-unit',
    getTpl() {
        let me = this;
        return `<div class="input-group" id="${me.getId()}">
                    <input type="text" class="form-control"/>
                    <div class="input-suffix">
                        <button type="button" class="btn btn-edit">
                            <i class="fa fa-pencil-alt"></i>
                        </button>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).on('keydown', 'input', function (e) {
            if (e.key == 'Enter') {
                me.showEditDialog();
            }
            if (e.key != 'Tab') {
                e.preventDefault();//禁止输入，使用readonly或者disabled样式冲突
            }
        }).on('click', '.btn-edit', function () {
            me.showEditDialog();
        });
    },
    showEditDialog() {
        let me = this;
        jmaa.showDialog({
            title: '编辑'.t(),
            css: 'modal-xs',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    model: 'md.unit_conversion',
                    module: me.module,
                    arch: `<form cols="2">
                                <editor name="radio" type="float" label="比例" help="比例=1衍生单位/1主单位" gt="0" decimals="6" required="1"></editor>
                                <editor name="base_id" type="many2one" label="主单位" comodel="md.unit" lookup="[['is_base','=',true]]" required="1"></editor>
                            </form>`
                });
                let value = me.getValue();
                if (value) {
                    dialog.form.setData({radio: value[0], base_id: [value[1], value[2]]});
                } else {
                    dialog.form.setData({radio: null, base_id: null});
                }
            },
            submit(dialog) {
                if (dialog.form.valid()) {
                    let data = dialog.form.getData();
                    me.setValue([data.radio, ...data.base_id]);
                    dialog.close();
                }
            }
        });
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        me.dom.find('input,button').attr('disabled', readonly);
    },
    getValue() {
        let me = this;
        let ratio = parseFloat(me.dom.attr("data-ratio"));
        if (isNaN(ratio)) {
            return null;
        }
        return [ratio, me.dom.attr("data-base-id"), me.dom.attr("data-base-unit")];
    },
    setValue(value) {
        let me = this;
        if (value != me.getValue()) {
            if (value && value.length) {
                me.dom.attr("data-ratio", value[0]).attr("data-base-id", value[1]).attr("data-base-unit", value[2]);
                me.dom.find('input').val(value[0] + " " + value[2]);
            } else {
                me.dom.removeAttr('data-ratio data-base-id data-base-unit');
                me.dom.find('input').val('');
            }
            me.dom.trigger('change');
        }
    },
});
jmaa.column('unit-conversion', {
    render: function () {
        return function (data) {
            if (data && data.length) {
                return data[0] + " " + data[2];
            }
            return "";
        }
    }
});
