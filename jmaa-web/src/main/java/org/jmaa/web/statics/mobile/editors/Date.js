jmaa.editor('date', {
    extends: 'editors.date',
    getTpl() {
        let me = this;
        let id = me.getId();
        let label = me.dom.attr('label') || '';
        label && (label = label.t());
        return `<div class="date-edit input-group" id="${id}" data-target-input="nearest">
                    <input readonly="readonly" type="text" class="form-control datetimepicker-input" data-target="#${id}" />
                    <div class="input-suffix">
                        <i class="fa ${me.icon}" data-target="#${id}" data-toggle="datetimepicker"></i>
                        <i class="fa fa-times clear-input" style="display: none;"></i>
                    </div>
                    <div class="date-dropdown">
                        <div class="dropdown-body">
                            <div class="header">
                                <label>${label}</label>
                                <a class="btn-close pl-3 float-right">关闭</a>
                                <a data-btn="clear" class="float-right">清空</a>
                            </div>
                            <div class="dropdown"></div>
                        </div>
                    </div>
                </div>`;
    },
    prepareLocation() {
        let me = this;
        me.dom.find('.form-control').attr('readonly', true);
        me.dom.on('show.datetimepicker', function () {
            me.dom.find('.date-dropdown').show();
            me.dom.find('.bootstrap-datetimepicker-widget-readonly').removeClass('bootstrap-datetimepicker-widget-readonly');
        }).on('hide.datetimepicker', function () {
            me.dom.find('.date-dropdown').hide();
        }).on('mousedown', '[data-btn=clear]', function () {
            me.setValue();
        });
    }
});
