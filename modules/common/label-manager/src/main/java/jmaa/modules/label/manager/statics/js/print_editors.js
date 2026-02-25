//@ sourceURL=print_editors.js
jmaa.editor('label-editor', {
    getTpl: function () {
        let me = this;
        return `<div class="label-editor" id="${this.getId()}">
                    <div class="e-check">
                        <input id="label-${this.getId()}" type="checkbox" checked="checked">
                        <label for="label-${this.getId()}">${me.label.t()}</label>
                    </div>
                    <div>
                        <input type="text" class="form-control label-input"/>
                    </div>
                </div>`;
    },
    init: function () {
        let me = this, dom = me.dom;
        me.label = dom.attr('label');
        dom.html(me.getTpl());
        me.rows = 0;
        dom.on('click', '.e-check input', function () {
            if ($(this)[0].checked) {
                dom.find('.label-input').removeAttr('disabled');
            } else {
                dom.find('.label-input').val('').attr('disabled', true);
            }
        });
        let key = me.model + ":" + me.field.name;
        dom.html(me.getTpl()).on('change', '.e-check input', function () {
            window.localStorage.setItem(key, $(this).is(':checked') ? 1 : 0);
        });
        let checked = '0' != window.localStorage.getItem(key);
        dom.find('.e-check input').prop("checked", checked);
        if (!checked) {
            dom.find('.label-input').val('').attr('disabled', true);
        }
    },
    getValue: function () {
        let val = this.dom.find('.label-input').val()
        if (this.trim) {
            val = val.trim();
        }
        return val;
    },
    setValue(v) {
        this.dom.find('.label-input').val(v);
    },
    valid: function () {
        let me = this, val = this.getValue();
        if (this.dom.find('.e-check input').is(':checked') && !val) {
            return '不能为空'.t();
        }
    }
});
