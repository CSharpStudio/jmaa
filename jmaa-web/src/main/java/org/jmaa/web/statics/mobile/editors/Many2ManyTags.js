jmaa.editor('many2many-tags', {
    extends: 'editors.many2many-tags',
    css: 'e-selection e-multi-selection',
    limit: 10, // 显示条数
    activeTest: true,
    companyTest: true,
    valueField: 'id',
    displayField: 'present',
    getTpl() {
        let me = this;
        let label = me.dom.attr('label') || '';
        label && (label = label.t());
        return `<div id="${me.getId()}">
                    <div class="form-control">
                        <ul class="select-results">
                            <li class="input-item"></li>
                        </ul>
                    </div>
                    <div class="dropdown-select">
                        <div class="dropdown-body">
                            <div class="header">
                                <label>${label}</label>
                                <a class="btn-close float-right pl-3">关闭</a>
                                <a data-btn="clear" class="float-right">清空</a>
                            </div>
                            <div class="lookup-info"></div>
                            <div class="lookup-data"></div>
                            <div class="ui-footer">
                                <div class="input-group">
                                    <input type="text" class="ui-mini lookup-input"/>
                                    <a data-btn="clear-input" class="ui-btn"><i class="fa fa-times"></i></a>
                                    <a data-btn="search" class="ui-btn"><i class="fa fa-search"></i></a>
                                </div>
                                <a data-btn="prev" class="ui-btn"><i class="fa fa-angle-left"></i></a>
                                <a data-btn="next" class="ui-btn"><i class="fa fa-angle-right"></i></a>
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    placeDropdown() {
        let me = this;
        return me.dom.find('.dropdown-select');
    },
    initDom(){
        let me = this;
        let dom = me.dom;
        dom.html(me.getTpl()).on('click', '[data-btn=clear]', function (e) {
            me.offset = 0;
            me.keyword = '';
            me.clearValue();
            me.hideDropdown();
        }).on('click', '[data-btn=next]', function (e) {
            if (!$(this).hasClass('disabled')) {
                me.offset += me.limit;
                me.lookup();
            }
        }).on('click', '[data-btn=prev]', function (e) {
            if (!$(this).hasClass('disabled')) {
                me.offset -= me.limit;
                if (me.offset < 0) {
                    me.offset = 0;
                }
                me.lookup();
            }
        }).on('click', '.form-control', function (e) {
            if (!me.readonly()) {
                me.showDropdown();
                me.lookup();
                e.preventDefault();
            }
        }).on('click', '.btn-close,.dropdown-select', function (e) {
            let target = $(e.target);
            if (target.hasClass('btn-close') || target.hasClass('dropdown-select')) {
                me.hideDropdown();
            }
        }).on('click', '[data-btn=clear-input]', function (e) {
            me.keyword = '';
            me.dom.find('.lookup-input').val('');
            me.lookup();
        }).on('click', '[data-btn=search]', function (e) {
            me.keyword = me.dom.find('.lookup-input').val();
            me.lookup();
        }).on('keyup', '.lookup-input', function (e) {
            if (e.key == 'Enter') {
                me.keyword = $(this).val();
                me.lookup();
            }
        }).on('openChange', function () {
            if (me.open) {
                setTimeout(() => me.dom.find('.lookup-input').addClass('focus').focus(), 500);
            } else {
                me.offset = 0;
                me.keyword = '';
                me.dom.find('.form-control').removeClass('focus');
            }
        }).find('.form-control input').on('focus', function (e) {
            if (!me.readonly()) {
                $(this).attr('readonly', true);// 屏蔽默认键盘弹出，第二次点击才会弹出
                setTimeout(() => $(this).attr('readonly', false), 5);
            }
        });
    },
});
