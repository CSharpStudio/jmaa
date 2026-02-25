jmaa.editor('many2many-tags', {
    extends: 'editors.selection',
    css: 'e-multi-selection',
    limit: 10, // 显示条数
    activeTest: true,
    companyTest: true,
    valueField: 'id',
    displayField: 'present',
    usePresent: true,
    getTpl() {
        let me = this;
        return `<div id="${me.getId()}">
                    <div class="form-control">
                        <ul class="select-results">
                            <li class="input-item">
                                <input type="text" class="form-control"/>
                            </li>
                        </ul>
                    </div>
                    <div class="dropdown-select">
                        <div class="lookup-info p-3"></div>
                        <div class="lookup-data"></div>
                        <div class="lookup-footer">
                            <button type="button" data-btn="clear" class="btn btn-sm btn-default">${'清空'.t()}</button>
                            <div class="btn-group">
                                <button type="button" data-btn="prev" class="btn btn-sm btn-default">
                                    <i class="fa fa-angle-left"></i>
                                </button>
                                <button type="button" data-btn="next" class="btn btn-sm btn-default">
                                    <i class="fa fa-angle-right"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.offset = 0;
        me.selected = [];
        me.values = [];
        me.delete = [];
        me.create = [];
        me.keyword = '';
        me.initDom();
        me.prepareDropdown();
        if (me.sortable) {
            me.dom.addClass('sortable');
            me.initDrop(me.dom.find('.select-results'));
        }
    },
    initDom() {
        let me = this;
        let dom = me.dom;
        me.sortable = me.nvl(eval(dom.attr('sortable')), me.sortable);
        dom.html(me.getTpl()).on('click', '[data-btn=clear]', function (e) {
            me.offset = 0;
            me.keyword = '';
            me.clearValue();
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
        }).on('click', '.remove-item', function () {
            let value = $(this).parent().attr('data-value');
            me.removeValue(value);
            if (me.open) {
                me.showDropdown();
            }
        }).on('openChange', function () {
            if (me.open) {
                me.dom.find('.input-item input').focus();
            } else {
                me.offset = 0;
                me.keyword = '';
                me.dom.find('.input-item input').val('');
                me.dom.find('.form-control').removeClass('focus');
            }
        });
    },
    onInputKeyup(input) {
        let me = this;
        if (!me.open) {
            me.showDropdown();
        }
        clearTimeout(me.keyupTimer);
        me.keyupTimer = setTimeout(function () {
            me.offset = 0;
            me.keyword = input.val().trim();
            me.lookup();
        }, 500);
    },
    filterCriteria() {
        let me = this;
        return [["present", "like", me.keyword]];
    },
    getFilter() {
        let me = this;
        let criteria = me.filterCriteria();
        let filter = me.dom.attr('search') || me.dom.attr('lookup');
        let tFilter = me.dom.attr('t-filter');
        if (filter) {
            filter = jmaa.utils.decode(filter);
            let data = me.owner.getRawData();
            data.__filter = new Function("with(this) return " + filter);
            filter = data.__filter();
            $.each(filter, function () {
                criteria.push(this);
            });
        }
        if (tFilter) {
            const fn = me.view[tFilter];
            if (!fn) {
                console.error("未定义t-filter方法:" + tFilter);
            } else {
                criteria = fn.call(me.view, criteria, me);
            }
        }
        return criteria;
    },
    lookup() {
        let me = this;
        me.dom.find('.lookup-info').show().html('加载中'.t());
        me.dom.find('.lookup-data').hide();
        let fields = ['present'];
        me.searchRelated(r => {
            me.renderItems(r.data.values);
            if (r.data?.hasNext) {
                me.dom.find('[data-btn=next]').removeClass('disabled');
            } else {
                me.dom.find('[data-btn=next]').addClass('disabled');
            }
            if (me.offset > 0) {
                me.dom.find('[data-btn=prev]').removeClass('disabled');
            } else {
                me.dom.find('[data-btn=prev]').addClass('disabled');
            }
        }, fields);
    },
    searchRelated(callback, fields) {
        let me = this;
        if (me.design) {
            callback({data: []});
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "searchByField",
            args: {
                relatedField: me.field.name,
                limit: me.limit,
                offset: me.offset,
                criteria: me.getFilter(),
                fields,
                order: me.order,
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    },
    renderItems(values) {
        let me = this;
        me.data = values;
        if (values.length) {
            me.dom.find('.lookup-info').hide();
            let body = me.dom.find('.lookup-data').show();
            let options = [];
            $.each(values, function () {
                let selected = me.selected.includes(this[me.valueField]) ? ` selected` : '';
                options.push(`<li class="options${selected}" value="${this[me.valueField]}"><span>${this[me.displayField]}</span></li>`);
            });
            body.html(`<ul>${options.join('')}</ul>`);
        } else {
            me.dom.find('.lookup-info').html('没有数据'.t());
        }
        me.placeDropdown();
    },
    loadPresent() {
        let me = this;
        if (me.design) {
            return;
        }
        jmaa.rpc({
            model: me.owner.model,
            module: me.owner.module,
            method: "searchByField",
            args: {
                relatedField: me.field.name,
                criteria: [['id', 'in', me.values]],
                fields: ["present"],
                limit: me.values.length,
            },
            context: {
                active_test: false,
                company_test: false,
            },
            onsuccess(result) {
                me.addSelected(result.data.values);
                me.dirty = false;
            }
        });
    },
    selectItem(item) {
        let me = this;
        let value = item.attr('value');
        if (me.selected.includes(value)) {
            me.removeValue(value);
        } else {
            if (me.values.includes(value)) {
                me.delete.remove(value);
            } else {
                me.create.push(value);
            }
            let data = {};
            data[me.valueField] = value;
            data[me.displayField] = item.html();
            me.addSelected([data]);
        }
        me.placeDropdown();
    },
    removeValue(value) {
        let me = this;
        me.selected.remove(value);
        me.create.remove(value);
        if (me.values.includes(value)) {
            me.delete.push(value);
        }
        me.dom.find(`li.options[value="${value}"]`).removeClass('selected');
        me.dom.find(`.select-results [data-value="${value}"]`).remove();
        me.dom.triggerHandler('valueChange', [me]);
    },
    addSelected(list) {
        let me = this;
        for (let row of list) {
            let key = row.id;
            let text = row.present;
            me.selected.push(key);
            let html = $(`<li class="select-item" data-value="${key}" data-text="${text}">
                        <span class="remove-item">×</span>
                        <span class="item-text">${text}</span>
                    </li>`);
            me.dom.find(`li.options[value="${key}"]`).addClass('selected');
            me.dom.find('.input-item').before(html);
            me.sortable && me.initDrag(html);
        }
        me.dom.triggerHandler('valueChange', [me]);
    },
    clearValue() {
        let me = this;
        let value = me.values;
        me.setValue();
        me.delete = value;
    },
    showDropdown() {
        let me = this;
        //先增加focus样式控制input显示，再计算下拉位置
        me.dom.find('.form-control').addClass('focus');
        me.callSuper();
    },
    /**
     * 获取用于提交的数据，使用指令创建或者删除
     * [[4, id, 0]] / [[3, id, 0]]
     * @returns {*[]}
     */
    getDirtyValue() {
        let me = this;
        if (me.sortable) {
            return [[6, 0, me.selected]];
        }
        let result = [];
        for (let item of me.create) {
            result.push([4, item, 0]);
        }
        for (let item of me.delete) {
            result.push([3, item, 0]);
        }
        return result;
    },
    getRawValue() {
        return this.selected;
    },
    /**
     * 获取数据[id1, id2]
     * @returns {*[]}
     */
    getValue() {
        return this.selected;
    },
    resetValue() {
        let me = this;
        if (me.values.length) {
            me.delete = [...me.values];
            me.values = [];
            me.create = [];
            me.selected = [];
            me.dom.find(`li.options.selected`).removeClass('selected');
            me.dom.find(`.select-results .select-item`).remove();
        }
    },
    setValue(value) {
        let me = this;
        me.values = [];
        me.delete = [];
        me.create = [];
        me.selected = [];
        me.dom.find(`li.options.selected`).removeClass('selected');
        me.dom.find(`.select-results .select-item`).remove();
        if (value && value.length) {
            let loadPresent = false;
            for (let item of value) {
                if (Array.isArray(item)) {
                    if (item.length == 3) {
                        // 编辑指令[4, id, 0]
                        if (item[0] == 4) {
                            me.create.push(item[1]);
                        }
                        me.values.push(item[1]);
                        loadPresent = true;
                    } else if (item.length == 2) {
                        // [id, present]
                        let data = {};
                        data[me.valueField] = item[0];
                        data[me.displayField] = item[1];
                        me.addSelected([data]);
                        me.values.push(item[0]);
                    }
                } else {
                    me.values.push(item);
                    loadPresent = true;
                }
            }
            if (loadPresent) {
                me.loadPresent();
            }
        } else {
            me.dom.triggerHandler('valueChange');
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

jmaa.searchEditor('many2many-tags', {
    extends: 'editors.many2many-tags',
    link: false,
    activeTest: false,
    getCriteria() {
        let me = this;
        let value = me.getValue();
        if (value.length) {
            return [[me.name, 'in', value]];
        }
        return [];
    },
    getText() {
        let me = this;
        let text = [];
        me.dom.find('li.select-item').each(function () {
            let item = $(this);
            text.push(item.attr('data-text'));
        });
        return text.join(',');
    },
});

jmaa.searchEditor('many2many', {
    extends: 'searchEditors.many2many-tags',
});
