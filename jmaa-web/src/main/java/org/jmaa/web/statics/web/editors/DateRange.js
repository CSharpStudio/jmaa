jmaa.editor('date-range', {
    css: 'e-date-range',
    format: 'YYYY-MM-DD',
    timePicker: false,
    customRangeLabel: '自定义范围',
    ranges: {
        最近1天: [moment().startOf('day'), moment().endOf('day')],
        最近2天: [moment().startOf('day'), moment().endOf('day').add(1, 'days')],
        最近3天: [moment().startOf('day'), moment().endOf('day').add(2, 'days')],
        最近7天: [moment().startOf('day'), moment().endOf('day').add(6, 'days')],
    },
    getTpl: function () {
        return `<div class="input-group">
                    <input id="${this.getId()}" type="text" class="form-control"/>
                    <div class="input-suffix">
                        <i class="far fa-calendar-alt"></i>
                    </div>
                </div>`;
    },
    init: function () {
        let me = this;
        let dom = me.dom;
        me.format = me.nvl(me.dom.attr('format'), me.format);
        me.startDate = me.nvl(dom.attr('startDate'), me.startDate, new Date().format(this.format));
        me.endDate = me.nvl(dom.attr('endDate'), me.endDate, new Date().format(this.format));
        dom.html(me.getTpl())
            .find('#' + me.id).daterangepicker({
                startDate: me.startDate,
                endDate: me.endDate,
                locale: {
                    format: me.format,
                    customRangeLabel: me.customRangeLabel,
                },
                timePicker: me.timePicker,
                timePicker24Hour: me.timePicker,
                timePickerSeconds: me.timePicker,
                ranges: me.ranges,
            }
        ).on('apply.daterangepicker', function (e, picker) {
            me.startDate = picker.startDate.format(me.format);
            me.endDate = picker.endDate.format(me.format);
        });
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('apply.daterangepicker', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        if (readonly) {
            this.dom.find('input').attr('disabled', true);
        } else {
            this.dom.find('input').removeAttr('disabled');
        }
    },
    getValue: function () {
        let me = this;
        return me.dom.find('#' + me.getId()).val();
    },
    setValue: function (v) {
        let me = this;
        if (!v || v === undefined || v === '') {
            me.dom.find('input').val('').trigger('change');
            me.startDate = null;
            me.endDate = null;
        } else {
            let values = v.split(' - ');
            me.dom.find('input').val(values[0] + " - " + values[1]).trigger('change');
            me.startDate = values[0];
            me.endDate = values[1];
        }
    }
});
jmaa.searchEditor('date-range', {
    extends: 'editors.date-range',
    getCriteria: function () {
        let me = this;
        if (this.dom.find('input').val()) {
            return ['&', [me.name, '>=', me.startDate], [me.name, '<=', me.endDate]];
        }
        return [];
    },
    getText: function () {
        let me = this,
            text = me.dom.find('input').val();
        if (text) {
            return [me.startDate + "至".t() + me.endDate];
        }
        return "";
    },
});
