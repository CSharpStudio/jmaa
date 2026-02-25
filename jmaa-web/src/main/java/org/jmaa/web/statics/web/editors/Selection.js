jmaa.editor('selection', {
    css: 'e-selection',
    getTpl() {
        let me = this;
        let options = [];
        for (const key in me.options) {
            options.push(`<li class="options" value="${key}">${me.options[key]}</li>`);
        }
        let placeholder = me.placeholder ? ` placeholder="${me.placeholder}"` : '';
        return `<div class="input-group" id="${this.getId()}">
                    <input type="text" class="form-control select-input"${placeholder}/>
                    <div class="input-suffix">
                        <span>
                            <i style="padding: 5px 8px" class="fa fa-angle-down icon-down"></i>
                            <i style="padding: 6px 8px" class="fa fa-times clear-input"></i>
                        </span>
                    </div>
                    <div class="dropdown-select">
                        <ul class="dropdown-content">${options.join('')}</ul>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.allowNull = me.nvl(eval(dom.attr('allow-null')), me.allowNull, !me.field.required);
        me.placeholder = jmaa.utils.decode(me.nvl(dom.attr('placeholder'), me.placeholder, ''));
        if (me.field.type == 'boolean') {
            me.options = {'true': '是'.t(), 'false': '否'.t()};
        } else {
            let opt = dom.attr('options');
            if (opt) {
                me.options = eval("(" + opt + ")");
            } else {
                me.options = me.nvl(me.options, me.field.options || {});
            }
        }
        dom.html(me.getTpl()).on('click', '.form-control,.icon-down', function (e) {
            if (!me.readonly()) {
                me.showDropdown();
                me.filterData();
                e.preventDefault();
            }
        }).on('click', '.clear-input', function (e) {
            let item = $(this);
            me.setValue();
            e.preventDefault();
        }).on('mouseover', '.form-control,.input-suffix', function () {
            if (me.allowNull && !me.readonly() && dom.attr('data-value')) {
                dom.find('.input-suffix i:first').hide();
                dom.find('.input-suffix i:last').show();
            }
        }).on('mouseout', '.form-control,.input-suffix', function () {
            if (me.allowNull) {
                dom.find('.input-suffix i:first').show();
                dom.find('.input-suffix i:last').hide();
            }
        });
        me.prepareDropdown();
        me.onInputBlur();
    },
    hoverItem(direction) {
        let me = this;
        if (me.dom.find('li.empty').length) {
            return;
        }
        let current = me.dom.find('li.hover');
        if (!current.length) {
            current = me.dom.find('li.selected:first');
        }
        if (!current.length) {
            current = me.dom.find('li.options:first');
        }
        let to = direction === 'up' ? current.prev() : current.next();
        if (!to.length) {
            to = direction === 'up' ? me.dom.find('li.options:last') : me.dom.find('li.options:first');
        }
        me.dom.find('li.hover').removeClass('hover');
        to.addClass('hover');
    },
    prepareDropdown() {
        let me = this;
        me.dom.find('input.form-control').on('keydown', function (e) {
            if (me.open) {
                if (e.key == 'ArrowUp') {
                    e.preventDefault();
                    me.hoverItem('up');
                } else if (e.key == 'ArrowDown') {
                    e.preventDefault();
                    me.hoverItem('down');
                } else if (e.key == 'Enter') {
                    e.preventDefault();
                    me.onEnter();
                }
            } else if (e.key == 'ArrowUp' || e.key == 'ArrowDown' || e.key == 'Enter') {
                e.preventDefault();
                me.showDropdown();
            }
        }).on('wheel', function (e) {
            if (me.open) {
                if (e.originalEvent.deltaY > 0) {
                    me.hoverItem('down');
                } else {
                    me.hoverItem('up');
                }
                e.preventDefault();
            }
        }).on('keyup paste', function (e) {
            if (!me.readonly() && !['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Enter'].includes(e.key)) {
                let input = $(this);
                me.onInputKeyup(input);
            }
        });
        me.dom.on('blur', '.select-input', function (e) {
            $(this).val(me.dom.data('text'));
        }).on('click', 'li.options', function () {
            me.selectItem($(this));
        }).on('mouseenter', 'li.options', function () {
            me.dom.find('li.hover').removeClass('hover');
            $(this).addClass('hover');
        }).on('mouseleave', 'li.options', function () {
            $(this).removeClass('hover');
        });
        $(document).on('mousedown', function (e) {
            if ($(e.target).closest('.dropdown-select').length == 0) {
                me.hideDropdown();
            }
        });
        $(window).on('resize', function () {
            if (me.open) {
                me.placeDropdown();
            }
        });
        me.dom.parents().on('scroll', function () {
            if (me.open) {
                me.placeDropdown();
            }
        });
    },
    onInputBlur() {
        let me = this;
        me.dom.on('mousedown', '.dropdown-select', false);
        me.dom.find('input.form-control').on('blur', function (e) {
            me.hideDropdown();
        })
    },
    onInputKeyup(input) {
        let me = this;
        if (!me.open) {
            me.showDropdown();
        }
        clearTimeout(me.keyupTimer);
        me.keyupTimer = setTimeout(function () {
            me.filterData(input.val().trim());
        }, 100);
    },
    onEnter() {
        let me = this;
        let current = me.dom.find('li.hover');
        if (current.length) {
            me.selectItem(current);
        }
    },
    filterData(keyword) {
        let me = this;
        me.dom.find('.dropdown-select ul .empty').remove();
        if (!keyword) {
            me.dom.find('.dropdown-select .options').show();
        } else {
            let found;
            me.dom.find('.dropdown-select .options').each(function () {
                let item = $(this);
                if (item.html().includes(keyword)) {
                    item.show();
                    found = true;
                } else {
                    item.hide();
                }
            });
            if (!found) {
                me.dom.find('.dropdown-select ul').append(`<li class="empty">${'无匹配数据'.t()}</li>`);
            }
        }
    },
    placeDropdown() {
        let me = this;
        let dropdown = me.dom.find('.dropdown-select').css('width', 'auto');
        let parent = dropdown.parent();
        let parentWidth = parent.width();
        let parentHeight = parent.height();
        let offset = parent.offset();
        let width = dropdown.width();
        let height = dropdown.height();
        let winWidth = $(window).width();
        let winHeight = $(window).height();
        let left = offset.left;
        if (offset.left + width > winWidth) {
            left = offset.left + parentWidth - width;
        }
        let top = offset.top;
        if (top + parentHeight + height > winHeight) {
            let maxHeight = winHeight - top - parentHeight - 60;
            if (maxHeight < 70) {
                top = winHeight - parentHeight - 130;
                maxHeight = 70;
            }
            dropdown.find('.dropdown-content').css('max-height', maxHeight + 'px');
        } else {
            dropdown.find('.dropdown-content').css('max-height', 'unset');
        }
        dropdown.css({
            'left': Math.max(20, left),
            'top': top + parentHeight
        });
        if (width > winWidth - 40) {
            dropdown.css('width', winWidth - 40);
        }
        return dropdown;
    },
    showDropdown() {
        let me = this;
        let input = me.dom.find('.select-input');
        input.select();
        me.placeDropdown().show().addClass('show');
        me.open = true;
        me.dom.triggerHandler("openChange", [me, me.open]);
    },
    hideDropdown() {
        let me = this;
        let el = me.dom.find('.dropdown-select');
        if (el.hasClass('show')) {
            el.hide().removeClass('show');
            me.open = false;
            me.dom.triggerHandler("openChange", [me, me.open]);
        }
    },
    selectItem(item) {
        let me = this;
        me.setValue([item.attr('value'), item.html()]);
        me.hideDropdown();
        me.dom.find('input.form-control').focus();
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        me.dom.find('input').attr('disabled', readonly);
    },
    getValue() {
        let me = this;
        let value = me.dom.attr('data-value');
        if (value) {
            return [value, me.dom.data('text')];
        }
        return null;
    },
    getRawValue() {
        let me = this;
        let value = me.dom.attr('data-value');
        return value || null;
    },
    setValue(value) {
        let me = this;
        if (value != me.getValue()) {
            me.dom.find('li.selected').removeClass('selected');
            if (Array.isArray(value)) {
                me.dom.find(`li[value="${value[0]}"]`).addClass('selected');
                me.dom.find('input').val(value[1]);
                me.dom.attr('data-value', value[0]).data('text', value[1]).trigger('valueChange');
            } else {
                let opt = me.dom.find(`li[value="${value}"]`);
                if (opt.length) {
                    opt.addClass('selected');
                    me.dom.find('input').val(opt.text());
                    me.dom.attr('data-value', value).data('text', opt.text()).trigger('valueChange');
                } else {
                    me.dom.find('input').val('');
                    me.dom.attr('data-value', '').data('text', '').trigger('valueChange');
                }
            }
        }
    },
    setAttr(attr, value) {
        let me = this;
        if ('options' == attr) {
            me.options = value;
            let options = [];
            for (const key in me.options) {
                options.push(`<li class="options" value="${key}">${me.options[key]}</li>`);
            }
            me.dom.find('.dropdown-select ul').html(options.join(''));
        } else {
            me.callSuper(attr, value);
        }
    }
});
