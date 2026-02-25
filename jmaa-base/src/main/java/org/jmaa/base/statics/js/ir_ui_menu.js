//@ sourceURL=ir_ui_menu.js
jmaa.editor('menu_view', {
    extends: 'editors.multi-selection',
    options: {grid: '表格', card: '卡片', form: '表单', mobile: '移动端', custom: '自定义'},
    sortable: true,
    getTpl: function () {
        let me = this;
        let options = [];
        for (const key in me.options) {
            options.push(`<li class="options" value="${key}">${me.options[key]}</li>`);
        }
        return `<div id="${this.getId()}">
                    <div class="view-group">
                        <ul class="select-results form-control">
                            <li class="input-item">
                                <input type="text" class="form-control"/>
                            </li>
                        </ul>
                        <input type="text" class="view-key"/>
                    </div>
                    <div class="dropdown-select">
                        <ul>${options.join('')}</ul>
                    </div>
                </div>`;
    },
    init: function () {
        let me = this;
        me.callSuper();
        me.dom.find('.view-key').on('change', function (e) {
            me.dom.trigger('valueChange');
        });
        me.dom.find('.view-key').attr('placeholder', '视图组'.t());
    },
    onValueChange: function (handler) {
        let me = this;
        me.dom.find('input').on('change', function (e) {
            handler(e, me);
        });
    },
    setReadonly: function (v) {
        if (v) {
            this.dom.find('input').attr('readonly', true);
        } else {
            this.dom.find('input').removeAttr('readonly');
        }
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            handler(e, me);
        });
    },
    getRawValue() {
        return this.getValue();
    },
    getValue: function () {
        let me = this;
        let key = me.dom.find('.view-key').val();
        let types = me.selected.join();
        return types + (key ? '|' + key : '');
    },
    getText: function () {
        return this.dom.find('input').val();
    },
    setValue: function (value) {
        let me = this;
        let key = me.dom.find('.view-key');
        if (value === undefined || value === null) {
            value = '';
        }
        if (value != me.getValue()) {
            me.selected = [];
            let array = value.split('|');
            key.val(array[1] || '');
            let types = array[0].split(',');
            me.dom.find(`li.options.selected`).removeClass('selected');
            me.dom.find(`.select-results .select-item`).remove();
            me.addValue(types);
        }
    }
});
