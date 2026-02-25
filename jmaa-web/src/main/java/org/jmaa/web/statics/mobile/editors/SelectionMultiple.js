jmaa.editor('multi-selection', {
    extends: 'editors.multi-selection',
    css: 'e-selection e-multi-selection',
    getTpl() {
        let me = this;
        let options = [];
        for (const key in me.options) {
            options.push(`<li class="options" value="${key}">${me.options[key]}</li>`);
        }
        let label = me.dom.attr('label') || '';
        label && (label = label.t());
        return `<div id="${this.getId()}">
                    <div class="form-control">
                        <ul class="select-results">
                            <li class="input-item">
                            </li>
                        </ul>
                    </div>
                    <div class="dropdown-select">
                        <div class="dropdown-body">
                            <div class="header"><label>${label}</label><a class="btn-close float-right">关闭</a></div>
                            <ul>${options.join('')}</ul>
                        </div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.callSuper();
        me.dom.on('click', '.btn-close,.dropdown-select', function (e) {
            let target = $(e.target);
            if (target.hasClass('btn-close') || target.hasClass('dropdown-select')) {
                me.hideDropdown();
            }
        }).find('.form-control input').on('focus', function (e) {
            if (!me.readonly()) {
                $(this).attr('readonly', true);// 屏蔽默认键盘弹出，第二次点击才会弹出
                setTimeout(() => $(this).attr('readonly', false), 5);
            }
        });
    },
    placeDropdown() {
        let me = this;
        return me.dom.find('.dropdown-select');
    },
    onInputBlur() {
    },
});
