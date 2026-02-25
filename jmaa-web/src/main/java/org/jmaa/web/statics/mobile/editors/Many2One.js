jmaa.editor('many2one', {
    extends: 'editors.selection',
    css: 'e-selection e-many2one',
    /**
     * 分页大小
     */
    limit: 10,
    /**
     * 是否显示link按钮
     */
    link: true,
    valueField: 'id',
    displayField: 'present',
    /**
     * 是否只加载state为true的数据
     */
    activeTest: true,
    companyTest: true,
    getTpl() {
        let me = this;
        let placeholder = me.placeholder ? ` placeholder="${me.placeholder}"` : '';
        let label = me.dom.attr('label') || '';
        label && (label = label.t());
        return `<div class="input-group" id="${this.getId()}">
                    <input type="text" class="form-control select-input"${placeholder} readonly="readonly"/>
                    <div class="input-suffix">
                        <span>
                            <i style="padding: 5px 8px" class="fa fa-angle-down icon-down"></i>
                            <i style="padding: 6px 8px" class="fa fa-times clear-input"></i>
                        </span>
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
                                    <input type="text" class="ui-mini lookup-input" placeholder="${me.placeholder || '搜索'.t()}"/>
                                    <a data-btn="clear-input" class="ui-btn"><i class="fa fa-times"></i></a>
                                    <a data-btn="search" class="ui-btn"><i class="fa fa-search"></i></a>
                                </div>
                                <button type="button" data-btn="prev" class="ui-btn"><i class="fa fa-angle-left"></i></button>
                                <button type="button" data-btn="next" class="ui-btn"><i class="fa fa-angle-right"></i></button>
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.callSuper();
        let dom = me.dom;
        me.offset = 0;
        me.keyword = '';
        me.name = me.field.name || dom.parent().attr('data-editor');
        me.comodel = dom.attr('comodel') || me.field.comodel;
        me.label = dom.attr('label') || me.field.label || me.name;
        me.initDom();
        let viewKey = me.dom.attr('view');
        if (viewKey) {
            me.initLookupList(viewKey);
        }
    },
    initDom() {
        let me = this;
        let dom = me.dom;
        dom.html(me.getTpl()).on('click', '[data-btn=clear]', function (e) {
            me.offset = 0;
            me.keyword = '';
            me.setValue();
        }).on('click', '[data-btn=next]', function (e) {
            if (!$(this).hasClass('disabled')) {
                me.offset += me.limit;
                me.lookup();
            }
        }).on('click', '[data-btn=clear-input]', function (e) {
            me.keyword = '';
            me.dom.find('.lookup-input').val('');
            me.lookup();
        }).on('click', '[data-btn=search]', function (e) {
            me.keyword = me.dom.find('.lookup-input').val();
            me.lookup();
        }).on('click', '[data-btn=prev]', function (e) {
            if (!$(this).hasClass('disabled')) {
                me.offset -= me.limit;
                if (me.offset < 0) {
                    me.offset = 0;
                }
                me.lookup();
            }
        }).on('keyup', '.lookup-input', function (e) {
            if (e.key == 'Enter') {
                me.keyword = $(this).val();
                me.lookup();
            }
        }).on('openChange', function () {
            if (!me.open) {
                me.offset = 0;
                me.keyword = '';
                me.dom.find('.lookup-input').val('');
            } else {
                me.lookup();
                let input = me.dom.find('.lookup-input').attr('readonly', true).addClass('focus').focus();
                setTimeout(() => input.attr('readonly', false), 100);
            }
        }).on('mouseenter', 'table tbody tr', function () {
            me.dom.find('li.hover').removeClass('hover');
            $(this).addClass('hover');
        }).on('mouseleave', 'table tbody tr', function () {
            $(this).removeClass('hover');
        }).find('.form-control').on('focus', function (e) {
            if (!me.readonly()) {
                $(this).attr('readonly', true);// 屏蔽默认键盘弹出，第二次点击才会弹出
                setTimeout(() => $(this).attr('readonly', false), 5);
            }
        });
    },
    initLookupList(viewKey) {
        const me = this;
        let key = viewKey == 'default' ? null : viewKey;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadLookup',
            args: {
                model: me.field.comodel,
                key,
            },
            async: false,
            onsuccess(r) {
                let arch = r.data.arch;
                arch = arch.replaceAll('<lookup', '<list pull-up="0" reload="0"').replaceAll('/lookup>', '/list>');
                me.lookupList = new JList({
                    dom: me.dom.find('.lookup-data'),
                    model: me.comodel,
                    module: me.module,
                    arch,
                    fields: r.data.fields,
                    view: me.view,
                    owner: me,
                    active: true,
                    on: {
                        selected(e, list, sel) {
                            if (sel.length > 0) {
                                me.setValue([sel[0], list.data.find(r => r.id == sel[0]).present]);
                            }
                        }
                    },
                    ajax(list, callback) {
                        let value = me.dom.attr('data-value');
                        if (value) {
                            list.selected = [value];
                        }
                        callback({
                            data: me.data || [],
                        });
                        let id = me.dom.attr('data-value');
                        if (id) {
                            list.dom.find('#' + id).addClass('selected');
                        }
                    },
                });
            },
        });
    },
    lookup() {
        let me = this;
        me.dom.find('.lookup-info').show().html('加载中'.t());
        me.dom.find('.lookup-data').hide();
        let fields = ['present'];
        if (me.lookupList) {
            fields.push(...me.lookupList.getFields())
        }
        me.searchRelated(r => {
            if (r.data?.values?.length) {
                me.renderItems(r.data.values);
            } else {
                me.renderNoData();
            }
            me.placeDropdown();
            if (r.data?.hasNext) {
                me.dom.find('[data-btn=next]').attr('disabled', false);
            } else {
                me.dom.find('[data-btn=next]').attr('disabled', true);
            }
            if (me.offset > 0) {
                me.dom.find('[data-btn=prev]').attr('disabled', false);
            } else {
                me.dom.find('[data-btn=prev]').attr('disabled', true);
            }
        }, fields);
    },
    renderNoData() {
        let me = this;
        me.dom.find('.lookup-data ul').html('');
        let html = `<div>${'没有数据'.t()}</div>`;
        me.dom.find('.lookup-info').html(html);
    },
    renderItems(values) {
        let me = this;
        me.dom.find('.lookup-info').hide();
        let body = me.dom.find('.lookup-data').show();
        me.data = values;
        if (me.lookupList) {
            me.lookupList.selected = [];
            me.lookupList.load();
            return;
        }
        let options = [];
        $.each(values, function () {
            let selected = this[me.valueField] === me.dom.attr('data-value') ? ` selected` : '';
            options.push(`<li class="options${selected}" value="${this[me.valueField]}">${this[me.displayField]}</li>`);
        });
        body.html(`<ul>${options.join('')}</ul>`);
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
    searchRelated(callback, fields) {
        let me = this;
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
    loadPresent(id, el) {
        let me = this;
        jmaa.rpc({
            model: me.owner.model,
            module: me.owner.module,
            method: "searchByField",
            args: {
                relatedField: me.field.name,
                criteria: [['id', '=', id]],
                fields: ["present"],
                limit: 1,
                options: {
                    criteria: [['id', '=', id]],
                    fields: ["present"],
                    nextTest: false
                }
            },
            onsuccess(result) {
                let value = result.data.values;
                if (value.length > 0) {
                    me.setValue([value[0].id, value[0].present]);
                }
            }
        });
    },
    /**
     * 获取数据[id, present]
     * @returns {[*,*]|null}
     */
    getValue() {
        let me = this;
        let value = me.dom.attr('data-value');
        if (value) {
            return [value, me.dom.data('text')];
        }
        return null;
    },
    /**
     * 获取纯数据 id
     * @returns {*|null}
     */
    getRawValue() {
        let me = this;
        let value = me.dom.attr('data-value');
        return value || null;
    },
    setValue(value) {
        let me = this;
        let input = me.dom.find('input.form-control');
        if (jQuery.isArray(value) && value[0] && value[1]) {
            input.val(value[1]).attr('placeholder', '');
            me.dom.find('li.selected').removeClass('selected');
            me.dom.find(`li[value="${value[0]}"]`).addClass('selected');
            me.dom.attr('data-value', value[0]).data('text', value[1]).trigger('valueChange');
            me.dom.find('.icon-down').hide();
            me.dom.find('.input-suffix .clear-input').show();
        } else if (value && typeof value === "string") {
            me.loadPresent(value, input);
        } else {
            input.val('').attr('placeholder', me.placeholder);
            me.dom.find('li.selected').removeClass('selected');
            me.dom.attr('data-value', '').data('text', '').trigger('valueChange');
            me.dom.find('.icon-down').show();
            me.dom.find('.input-suffix .clear-input').hide();
        }
    },
    selectItem(item) {
        let me = this;
        me.setValue([item.attr('value'), item.html()]);
        me.hideDropdown();
        me.dom.find('input.form-control').focus();
    },
});
