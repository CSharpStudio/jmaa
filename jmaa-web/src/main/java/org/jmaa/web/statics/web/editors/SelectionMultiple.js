jmaa.editor('multi-selection', {
    extends: 'editors.selection',
    css: 'e-multi-selection',
    sortable: false,
    getTpl() {
        let me = this;
        let options = [];
        for (const key in me.options) {
            options.push(`<li class="options" value="${key}">${me.options[key]}</li>`);
        }
        return `<div id="${this.getId()}">
                    <div class="form-control">
                        <ul class="select-results">
                            <li class="input-item">
                                <input type="text" class="form-control"/>
                            </li>
                        </ul>
                    </div>
                    <div class="dropdown-select">
                        <ul>${options.join('')}</ul>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        let opt = dom.attr('options');
        if (opt) {
            me.options = eval("(" + opt + ")");
        } else {
            me.options = me.nvl(me.options, me.field.options || {});
        }
        me.sortable = me.nvl(eval(dom.attr('sortable')), me.sortable);
        me.selected = [];
        dom.html(me.getTpl()).on('click', '.form-control', function (e) {
            if (!me.readonly()) {
                me.showDropdown();
                me.filterData();
                e.preventDefault();
            }
        }).on('click', '.remove-item', function () {
            let value = $(this).parent().attr('data-value');
            me.removeValue(value);
            if (me.open) {
                me.showDropdown();
            }
        }).on('openChange', function () {
            if (me.open) {
                dom.find('.input-item input').focus();
            } else {
                dom.find('.input-item input').val('');
                dom.find('.form-control').removeClass('focus');
            }
        }).on('keyup paste', ".input-item input", function (e) {
            if (!me.readonly() && !['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Enter'].includes(e.key)) {
                let input = $(this);
                me.onInputKeyup(input);
            }
        });
        me.prepareDropdown();
        if (me.sortable) {
            me.dom.addClass('sortable');
            me.initDrop(me.dom.find('.select-results'));
        }
    },
    showDropdown() {
        let me = this;
        //先增加focus样式控制input显示，再计算下拉位置
        me.dom.find('.form-control').addClass('focus');
        me.callSuper();
    },
    selectItem(item) {
        let me = this;
        let value = item.attr('value');
        if (me.selected.includes(value)) {
            me.removeValue(value);
        } else {
            me.addValue(value);
        }
        me.placeDropdown();
    },
    addValue(value) {
        let me = this;
        let readonly = me.readonly();
        let values = Array.isArray(value) ? value : [value];
        for (let key of values) {
            let text = me.options[key];
            if (text) {
                me.selected.push(key);
                let html = $(`<li class="select-item" data-value="${key}" data-text="${text}">
                        <span class="remove-item" ${readonly ? 'style="display:none"' : ''}>×</span>
                        <span class="item-text">${text}</span>
                    </li>`);
                me.dom.find(`li.options[value="${key}"]`).addClass('selected');
                me.dom.find('.input-item').before(html);
                me.sortable && me.initDrag(html);
            }
        }
        me.dom.triggerHandler('valueChange', [me]);
    },
    removeValue(value) {
        let me = this;
        me.selected.remove(value);
        me.dom.find(`li.options[value="${value}"]`).removeClass('selected');
        me.dom.find(`.select-results [data-value="${value}"]`).remove();
        me.dom.triggerHandler('valueChange', [me]);
    },
    getRawValue() {
        return this.selected.join();
    },
    getValue() {
        let me = this;
        let result = [];
        me.dom.find('li.select-item').each(function () {
            let item = $(this);
            result.push([item.attr('data-value'), item.attr('data-text')]);
        });
        return result;
    },
    setValue(value) {
        let me = this;
        if (value === undefined || value === null) {
            value = '';
        }
        if (value != me.getValue()) {
            me.selected = [];
            let values = value.split(',');
            me.dom.find(`li.options.selected`).removeClass('selected');
            me.dom.find(`.select-results .select-item`).remove();
            me.addValue(values);
        }
    },
    initDrop(dom) {
        let me = this;
        let list = me.dom.find('.select-results');
        dom.on('dragover', function (e) {
            e.preventDefault();
            let dragging = $(this).find('.dragging');
            let placeholder = $(this).find('.placeholder');
            let target = $(e.target).closest('li:not(.dragging):not(.placeholder)');
            if (!placeholder.length) {
                placeholder = $('<li class="placeholder"></li>').insertBefore(dragging);
            }
            if (target.length) {
                let targetTop = target.offset().top;
                let targetHeight = target.outerHeight();
                let mouseY = e.clientY;
                if (mouseY < targetTop + targetHeight / 2) {
                    placeholder.insertBefore(target);
                } else {
                    placeholder.insertAfter(target);
                }
            } else {
                placeholder.insertBefore(list.find('.input-item'));
            }
        });
        dom.on('drop', function (e) {
            e.preventDefault();
            let dragging = $(this).find('.dragging');
            let placeholder = $(this).find('.placeholder');
            dragging.insertBefore(placeholder);
            me.selected = [];
            let list = me.dom.find('.select-results');
            list.find('.select-item').each(function () {
                me.selected.push($(this).attr('data-value'));
            });
            me.dom.triggerHandler('valueChange', [me]);
        });
    },
    initDrag(dom) {
        let me = this;
        let list = me.dom.find('.select-results');
        dom.attr('draggable', 'true');
        dom.on('dragstart', function (e) {
            $(this).addClass('dragging');
            e.originalEvent.dataTransfer.setData('text/plain', $(this).index());
            setTimeout(() => $(this).addClass('d-none'), 0);
        });
        dom.on('dragend', function () {
            $(this).removeClass('dragging d-none');
            list.find('.placeholder').remove();
        });

    }
});

jmaa.searchEditor('multi-selection', {
    extends: "editors.multi-selection",
    getCriteria() {
        let me = this;
        let selected = this.selected;
        if (selected.length) {
            return [[me.name, 'in', selected]];
        }
        return [];
    },
    getRawValue() {
        return this.selected;
    },
    getText() {
        let me = this;
        let result = [];
        for (let value of me.selected) {
            result.push(me.options[value]);
        }
        return result.join();
    }
});
jmaa.searchEditor('selection', {
    extends: "searchEditors.multi-selection",
});
