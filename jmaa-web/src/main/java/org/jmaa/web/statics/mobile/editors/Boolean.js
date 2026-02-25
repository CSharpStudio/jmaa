jmaa.editor('boolean', {
    css: 'e-boolean',
    getTpl() {
        let me = this;
        let id = me.getId();
        let name = "bool-" + jmaa.nextId();
        return `<fieldset data-role="controlgroup" data-type="horizontal" data-mini="true" id="${id}">
                    ${me.allowNull ? `<input type="radio" name="${name}" id="${name + "-null"}" value="null" checked="checked">
                    <label for="${name + "-null"}">${'空'.t()}</label>` : ''}
                    <input type="radio" name="${name}" id="${name + "-true"}" value="true">
                    <label for="${name + "-true"}">${'是'.t()}</label>
                    <input type="radio" name="${name}" id="${name + "-false"}" value="false">
                    <label for="${name + "-false"}">${'否'.t()}</label>
                </fieldset>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.allowNull = me.nvl(eval(dom.attr('allow-null')), me.allowNull);
        dom.html(me.getTpl());
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'input', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        if (readonly) {
            me.dom.find('input').attr('disabled', true);
        } else {
            me.dom.find('input').removeAttr('disabled');
        }
    },
    getValue() {
        let me = this;
        let value = me.dom.find('input:checked').val();
        if (value == 'true') {
            return true;
        } else if (value == 'false') {
            return false;
        }
        return null;
    },
    setValue(value) {
        let me = this;
        if (value === undefined || value === '') {
            value = null;
        } else if (value !== null && typeof value !== "boolean") {
            value = Boolean(eval(value));
        }
        let el = me.dom.find('input[value=' + value + ']').prop('checked', true);
        me.dom.find('.ui-btn-active').removeClass('ui-btn-active');
        me.dom.find(`[for=${el.attr('id')}]`).addClass('ui-btn-active');
        if (me.getValue() !== value) {
            el.trigger('change');
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
