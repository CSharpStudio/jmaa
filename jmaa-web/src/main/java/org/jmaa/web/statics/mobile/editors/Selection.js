jmaa.editor('selection', {
    extends: 'editors.selection',
    getTpl() {
        let me = this;
        let options = [];
        for (const key in me.options) {
            options.push(`<li class="options" value="${key}">${me.options[key]}</li>`);
        }
        let placeholder = me.placeholder ? ` placeholder="${me.placeholder}"` : '';
        let label = me.dom.attr('label') || '';
        label && (label = label.t());
        return `<div class="input-group" id="${this.getId()}">
                    <input type="text" readonly="readonly" class="form-control select-input"${placeholder}/>
                    <div class="input-suffix">
                        <span>
                            <i style="padding: 5px 8px" class="fa fa-angle-down icon-down"></i>
                            <i style="padding: 6px 8px" class="fa fa-times clear-input"></i>
                        </span>
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
        });
    },
    placeDropdown() {
        let me = this;
        return me.dom.find('.dropdown-select');
    },
    onInputBlur() {
    },
    selectItem(item) {
        let me = this;
        me.setValue([item.attr('value'), item.html()]);
        me.dom.find('input.form-control').focus();
    },
});
