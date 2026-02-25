jmaa.editor('time-range', {
    css: 'e-date',
    format: 'HH:mm:ss',
    getTpl() {
        let id = this.getId();
        return `<div class="d-flex" id="${id}">
                    <input type="text" class="form-control datetimepicker-input date-start"/>
                    <span class="ml-2 mr-2">-</span>
                    <input type="text" class="form-control datetimepicker-input date-end"/>
                </div>
        `;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.format = me.nvl(dom.attr('format'), me.format);
        dom.html(me.getTpl())
            .find('.date-start, .date-end')
            .datetimepicker({
                allowInputToggle: true,
                format: me.format,
            });
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change.datetimepicker', '.date-start, .date-end', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        this.dom.find('input').attr('disabled', readonly);
    },
    getValue() {
        let me = this;
        let result = '';
        let start = me.dom.find('.date-start').val();
        if (start) {
            result += me.dom.find('.date-start').datetimepicker('viewDate').format(me.format);
        }
        result += " - ";
        let end = me.dom.find('.date-end').val();
        if (end) {
            result += me.dom.find('.date-end').datetimepicker('viewDate').format(me.format);
        }
        return result == ' - ' ? null : result;
    },
    setValue(value) {
        let me = this;
        let date = me.dom.find('.date-edit');
        if (!value) {
            me.dom.find('.date-start, .date-end').datetimepicker('clear');
        } else {
            let values = value.split(' - ');
            me.dom.find('.date-start').datetimepicker('date', values[0]);
            me.dom.find('.date-end').datetimepicker('date', values[1]);
        }
    },
    valid() {
        let me = this;
        let value = this.getValue();
        if (value) {
            let values = value.split(' - ');
            if (values[0] && values[1] && values[0] > values[1]) {
                return '结束时间不能大于开始时间'.t();
            }
        }
    },
});
jmaa.searchEditor('time-range', {
    extends: "editors.time-range",
    getCriteria() {
        let value = this.getValue();
        let result = [];
        if (value) {
            let values = value.split(' - ');
            if (values[0]) {
                result.push([this.name, '>=', values[0]])
            }
            if (values[1]) {
                result.push([this.name, '<=', values[1]])
            }
        }
        return result;
    },
    getText() {
        return this.getValue();
    },
});
