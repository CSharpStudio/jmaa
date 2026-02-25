jmaa.editors = {};
jmaa.searchEditors = {};
jmaa.component("editors.Editor", {
    css: '',
    getId() {
        let me = this;
        if (!me.id) {
            if (me.owner && me.owner.model) {
                me.id = me.owner.model.replaceAll('\.', "_");
            }
            if (me.name) {
                me.id = me.id + "-" + me.name;
            }
            me.id += "-" + jmaa.nextId();
        }
        return me.id;
    },
    onInit() {
        let me = this;
        me.dom.addClass(me.css);
        let init = me.dom.attr('on-init');
        if (init) {
            let v = me.view || window.view;
            const fn = new Function('return this.' + init).call(v, me);
            if (fn instanceof Function) {
                fn.call(v, me);
            }
        }
        let valueChange = me.dom.attr('on-value-change');
        if (valueChange) {
            let v = me.view || window.view;
            me.onValueChange(function (e, editor) {
                if (me.owner && me.owner.loading) return;
                const fn = new Function('return this.' + valueChange).call(v, e, editor);
                if (fn instanceof Function) {
                    fn.call(v, e, editor);
                }
            });
        }
    },
    readonly(readonly) {
        let me = this;
        if (readonly === undefined) {
            return me.dom.hasClass('readonly');
        }
        readonly = Boolean(readonly);
        if (readonly) {
            me.dom.addClass('readonly');
        } else {
            me.dom.removeClass('readonly');
        }
        me.setReadonly(readonly);
    },
    getValue: jmaa.emptyFn,
    getRawValue() {
        return this.getValue();
    },
    getDirtyValue() {
        return this.getRawValue();
    },
    setValue: jmaa.emptyFn,
    resetValue() {
        let me = this;
        me.loading = true;
        me.setValue();
    },
    loadValue(value) {
        let me = this;
        me.loading = true;
        me.setValue(value);
    },
    onValueChange: jmaa.emptyFn,
    setReadonly: jmaa.emptyFn,
    valid: jmaa.emptyFn,
    setAttr(attr, value) {
        let me = this;
        me.dom.find('#' + me.getId()).attr(attr, value);
    }
});
jmaa.editor = function (name, define) {
    if (typeof define === "function") {
        define = define();
    }
    define.extends = define.extends || 'editors.Editor';
    jmaa.editors[name] = jmaa.component('editors.' + name, define);
}
jmaa.searchEditor = function (name, define) {
    if (typeof define === "function") {
        define = define();
    }
    define.extends = define.extends || 'editors.Editor';
    jmaa.searchEditors[name] = jmaa.component('searchEditors.' + name, define);
}
/**
 * 只显示文本, 不能编辑
 */
jmaa.editor('span', {
    noEdit: true,
    getTpl() {
        return `<input type="text" disabled="disabled" class="form-control" id="${this.name + '-' + jmaa.nextId()}"/>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl());
        me.usePresent = me.field.type == 'many2one';
    },
    setValue(value) {
        let me = this;
        if (me.field.type == 'many2one') {
            if (value && value[0]) {
                me.dom.find('input').attr("data", value).val(value[1]);
            } else {
                me.dom.find('input').removeAttr("data").val('');
            }
        } else {
            if (value === null || value === undefined) {
                value = '';
            }
            me.dom.find('input').val(value);
        }
    },
    getValue() {
        let me = this;
        let value = me.dom.find('input').val();
        if (me.field.type == 'many2one') {
            let data = me.dom.attr("data");
            if (data) {
                return [data, value];
            }
            return null;
        }
        if (value) {
            return value;
        }
        return null;
    },
    getRawValue() {
        let me = this;
        let value = me.field.type == 'many2one' ? me.dom.find('input').attr("data") : me.dom.find('input').val();
        return value || null;
    }
});
