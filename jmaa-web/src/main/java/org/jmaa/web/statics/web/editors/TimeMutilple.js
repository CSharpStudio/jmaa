jmaa.editor('multi-time', {
    css: 'e-multi-time e-date e-multi-selection',
    format: 'HH:mm:ss',
    length: 10,
    getTpl() {
        let me = this;
        return `<div id="${me.getId()}">
                    <div class="form-control input-group">
                        <ul class="select-results">
                        </ul>
                        <div class="input-suffix btn-dropdown">
                            <i class="fa fa-clock"></i>
                        </div>
                        <div class="dropdown-menu dropdown-menu-right">
                            <div class="time-edit">
                                <div class="time-dropdown">
                                    <div class="dropdown"></div>
                                </div>
                                <div class="border-top d-flex mt-1">
                                    <div class="btn btn-add m-auto">
                                        <i class="fa fa-check"></i>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.format = me.nvl(me.dom.attr('format'), me.format);
        me.length = me.nvl(me.dom.attr('length'), me.length);
        me.dom.html(me.getTpl()).on('click', '.btn-dropdown', function (e) {
            e.stopPropagation();
            me.dom.find('.dropdown-menu').toggleClass('show');
        }).on('click', '.remove-item', function (e) {
            let time = $(this).parent().find(".item-text").html();
            me.removeTime(time);
        }).find('.time-edit').datetimepicker({
            allowInputToggle: true,
            format: me.format,
            inline: true,
            locale: moment.locale('zh-cn'),
            sideBySide: true,
            widgetParent: me.dom.find('.dropdown'),
            ignoreReadonly: true,
            minDate: undefined,
            maxDate: undefined,
        });
        me.dom.find('.dropdown-menu').click(function (e) {
            e.stopPropagation();
        });
        me.dom.find('.btn-add').click(function (e) {
            let time = me.dom.find('.time-edit').datetimepicker('viewDate').format(me.format);
            me.addTime(time);
            me.dom.find('.dropdown-menu').removeClass('show');
        });
        $(document).on('click', function () {
            me.dom.find('.dropdown-menu').removeClass('show');
        });
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            handler(e, me);
        });
    },
    addTime(time) {
        let me = this;
        if (me.values.includes(time)) {
            return;
        }
        me.values.push(time);
        let html = `<li class="select-item" data-value="${time}">
                        <span class="remove-item">×</span>
                        <span class="item-text">${time}</span>
                    </li>`;
        me.dom.find('.select-results').append(html);
        me.dom.triggerHandler('valueChange', [me]);
    },
    removeTime(time) {
        let me = this;
        me.values.remove(time);
        me.dom.find(`.select-results [data-value="${time}"]`).remove();
        me.dom.triggerHandler('valueChange', [me]);
    },
    getValue() {
        let me = this;
        return me.values.join();
    },
    setValue(value) {
        let me = this;
        me.values = [];
        me.dom.find('.select-results').html('');
        if (value) {
            for (let time of value.split(',')) {
                me.addTime(time);
            }
        }
        me.dom.triggerHandler('valueChange');
    },
    valid() {
        let me = this;
        if (me.values.length > me.length) {
            return '不能超过{0}个值'.t().formatArgs(me.length);
        }
    }
});
