jmaa.editor('date', {
    css: 'e-date',
    format: 'YYYY-MM-DD',
    max: '2100-12-31',
    min: '1920-1-1',
    todayBtn: true,
    sideBySide: true,
    icon: 'fa-calendar-day',
    getTpl() {
        let me = this;
        let id = me.getId();
        return `<div class="date-edit input-group" id="${id}" data-target-input="nearest">
                    <input type="text" class="form-control datetimepicker-input" data-target="#${id}" />
                    <div class="input-suffix">
                        <div style="position: relative;line-height: 1">
                            <i class="fa ${me.icon} date-icon" data-target="#${id}" data-toggle="datetimepicker"></i>
                            <i class="fa fa-times date-clear" style="display: none;"></i>
                        </div>
                    </div>
                    <div class="date-dropdown">
                        <div class="dropdown"></div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        let getAttr = function (attr) {
            let value = me.dom.attr(attr);
            //可以使用 moment()、Date()，需要eval计算
            return value && /.*\((.*)\)/.test(value) ? eval(jmaa.utils.decode(value)) : value;
        }
        me.max = me.nvl(getAttr('max'), me.field.max, me.max);
        me.min = me.nvl(getAttr('min'), me.field.min, me.min);
        me.format = me.nvl(me.dom.attr('format'), me.format);
        me.initDom();
        me.prepareLocation();
        // dom.find('.form-control').on('focus', function (e) {
        //     if (!me.readonly()) {
        //         $(this).attr('readonly', true);// 屏蔽默认键盘弹出，第二次点击才会弹出
        //         setTimeout(() => $(this).attr('readonly', false), 5);
        //     }
        // });
    },
    prepareLocation() {
        let me = this;
        let updateLocation = function () {
            let dropdown = me.dom.find('.date-dropdown');
            let offset = me.dom.offset();
            dropdown.css({
                'left': offset.left,
                'top': offset.top
            });
        }
        $(window).on('resize', function () {
            updateLocation();
        });
        me.dom.parents().on('scroll', function () {
            updateLocation();
        });
    },
    initDom() {
        let me = this;
        let dom = me.dom;
        dom.html(me.getTpl()).on('click', '.input-suffix', function () {
            let item = $(this);
            if (!me.readonly() && dom.find('input').val()) {
                me.setValue();
            }
        }).on('mouseover', '.form-control,.input-suffix', function () {
            if (!me.readonly() && dom.find('input').val()) {
                dom.find('.input-suffix i.date-icon').hide();
                dom.find('.input-suffix i.date-clear').show();
            }
        }).on('mouseout', '.form-control,.input-suffix', function () {
            if (!me.readonly()) {
                dom.find('.input-suffix i.date-icon').show();
                dom.find('.input-suffix i.date-clear').hide();
            }
        }).find('.date-edit').datetimepicker({
            allowInputToggle: true,
            format: me.format,
            locale: moment.locale('zh-cn'),
            minDate: me.min ? new Date(me.min).format(me.format) : undefined,
            maxDate: me.max ? new Date(me.max).format(me.format) : undefined,
            todayBtn: me.todayBtn,
            sideBySide: me.sideBySide,
            widgetParent: me.dom.find('.dropdown'),
            ignoreReadonly: true
        });
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change.datetimepicker', '.date-edit', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        this.dom.find('input').attr('disabled', readonly);
    },
    getValue() {
        let me = this;
        let text = me.dom.find('input').val();
        if (text) {
            return me.dom.find('.date-edit').datetimepicker('viewDate').format(me.format);
        }
        return '';
    },
    setValue(value) {
        let me = this;
        let date = me.dom.find('.date-edit');
        if (!value) {
            date.datetimepicker('clear');
        } else {
            if (/moment|Date/.test(value)) {
                value = eval(value);
            }
            date.datetimepicker('date', value);
        }
    },
});

jmaa.searchEditor('date', {
    extends: 'editors.date',
    getCriteria() {
        const val = this.getValue();
        if (val) {
            return [[this.name, '=', val]];
        }
        return [];
    },
    getText() {
        return this.getValue();
    },
});
