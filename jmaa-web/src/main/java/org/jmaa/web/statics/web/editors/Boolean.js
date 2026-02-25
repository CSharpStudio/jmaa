jmaa.editor('boolean', {
    css: 'e-boolean',
    getTpl() {
        let me = this;
        let id = me.getId();
        if (me.allowNull) {
            return `<select class="form-control" focusable id="${id}">
                        <option value="null"></option>
                        <option value="true">${'是'.t()}</option>
                        <option value="false">${'否'.t()}</option>
                    </select>`;
        }
        return `<div class="custom-switch">
                    <input type="checkbox" focusable class="custom-control-input" id="${id}"/>
                    <label for="${id}" class="custom-control-label mt-1"></label>
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.allowNull = me.nvl(eval(dom.attr('allow-null')), me.allowNull);
        dom.html(me.getTpl());
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'select,input', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        let selector = me.allowNull ? 'select' : 'input';
        if (readonly) {
            me.dom.find(selector).attr('disabled', true);
        } else {
            me.dom.find(selector).removeAttr('disabled');
        }
    },
    getValue() {
        let me = this;
        if (me.allowNull) {
            let value = me.dom.find('select').val();
            if (value === 'true') {
                return true;
            } else if (value === 'false') {
                return false;
            }
            return null;
        }
        return me.dom.find('input').is(":checked");
    },
    setValue(value) {
        let me = this;
        if (me.allowNull) {
            if (value === undefined || value === '') {
                value = null;
            } else if (value !== null && typeof value !== "boolean") {
                value = Boolean(eval(value));
            }
            if (me.getValue() !== value) {
                return me.dom.find('select').val(String(value)).trigger('change');
            }
        } else {
            if (typeof value !== "boolean") {
                value = Boolean(eval(value));
            }
            if (me.getValue() !== value) {
                me.dom.find('input').prop("checked", value).trigger('change');
            }
        }
    }
});

jmaa.searchEditor('boolean', {
    extends: "editors.boolean",
    allowNull: true,
    getCriteria() {
        let value = this.getValue();
        if (value != null) {
            return [[this.name, '=', value]];
        }
        return [];
    },
    getText() {
        let me = this;
        let value = me.getValue();
        if (value === true) {
            return '是'.t();
        }
        if (value === false) {
            return '否'.t();
        }
        return "空值".t();
    },
});
