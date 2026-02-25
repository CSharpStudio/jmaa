jmaa.editor('many2one', {
    extends: 'editors.selection',
    css: 'e-many2one',
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
     * 只有一条记录时自动选中
     */
    autoSelectOption: false,
    /**
     * 是否只加载state为true的数据
     */
    activeTest: true,
    companyTest: true,
    usePresent: true,
    noDataText: '没有数据',
    getTpl() {
        let me = this;
        let linkBtn = me.link ?
            `<button type="button" data-btn="link" class="btn">
                <i class="fa fa-external-link-alt"></i>
            </button>` : '';
        let placeholder = me.placeholder ? ` placeholder="${me.placeholder}"` : '';
        return `<div class="input-group" id="${this.getId()}">
                    <input type="text"${placeholder} class="form-control select-input"/>
                    <div class="input-suffix">
                        ${linkBtn}
                        <span class="icon-down">
                            <i class="fa fa-angle-down"></i>
                        </span>
                    </div>
                    <div class="dropdown-select">
                        <div class="lookup-info"></div>
                        <div class="lookup-data dropdown-content"></div>
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
        let dom = me.dom;
        me.placeholder = jmaa.utils.decode(me.nvl(dom.attr('placeholder'), me.placeholder, ''));
        me.noDataText = jmaa.utils.decode(me.nvl(dom.attr('nodata-text'), me.noDataText));
        //弹窗里再弹窗显示有问题，所以限制不能再弹窗
        me.link = !me.view?.urlHash?.top && eval(me.nvl(dom.attr('link'), me.link)) && me.field.link;
        let creatable = dom.attr('creatable')
        if (creatable) {
            me.canCreate = eval(creatable);
        }
        if (!me.link) {
            //创建需要弹窗，点不开弹窗就不能创建
            me.canCreate = false;
        }
        me.name = me.field.name || dom.parent().attr('data-editor');
        me.comodel = dom.attr('comodel') || me.field.comodel;
        me.label = dom.attr('label') || me.field.label || me.name;
        me.offset = 0;
        me.keyword = '';
        me.order = dom.attr('order');
        me.initDom();
        me.prepareDropdown();
        let viewKey = dom.attr('view');
        if (viewKey) {
            me.initLookupGrid(viewKey);
        }
    },
    initDom() {
        let me = this, dom = me.dom;
        dom.html(me.getTpl()).on('click', '.btn-create', function () {
            me.showLinkModel({present: dom.find('input').val()});
        }).on('click', '[data-btn=link]', function (e) {
            me.showLinkModel({id: dom.attr('data-value')});
        }).on('click', 'input', function (e) {
            if (!me.readonly()) {
                me.showDropdown();
                e.preventDefault();
            }
        }).on('click', '[data-btn=clear]', function (e) {
            me.offset = 0;
            me.keyword = '';
            me.setValue();
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
        }).on('openChange', function () {
            if (!me.open) {
                me.offset = 0;
                me.keyword = '';
            } else {
                me.lookup();
            }
        }).on('blur', '.select-input', function (e) {
            me.keyword = '';
        }).on('mouseenter', 'table tbody tr', function () {
            me.dom.find('li.hover').removeClass('hover');
            $(this).addClass('hover');
        }).on('mouseleave', 'table tbody tr', function () {
            $(this).removeClass('hover');
        });
    },
    initLookupGrid(viewKey) {
        const me = this;
        let key = viewKey == 'default' ? null : viewKey;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadLookup',
            args: {
                model: me.comodel,
                key,
            },
            async: false,
            onsuccess(r) {
                let arch = r.data.arch;
                arch = arch.replaceAll('<lookup', '<grid').replaceAll('/lookup>', '/grid>');
                me.lookupGrid = me.dom.find('.lookup-data').JGrid({
                    model: me.comodel,
                    module: me.module,
                    arch,
                    fields: r.data.fields,
                    view: me.view,
                    ordering: false,
                    multiSelect: false,
                    showRowNum: false,
                    owner: me,
                    checkSelect: false,
                    vid: me.owner.vid + me.name,
                    on: {
                        selected(e, grid, sel) {
                            if (sel.length > 0) {
                                me.selectValue([sel[0], grid.data.find(r => r.id == sel[0]).present]);
                            }
                        }
                    },
                    ajax(grid, callback, data, settings) {
                        callback({
                            data: me.data || [],
                        });
                        let id = me.dom.attr('data-value');
                        if (id) {
                            grid.dom.find('#' + id).addClass('selected');
                        }
                    },
                });
            },
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
    onEnter() {
        let me = this;
        if (me.lookupGrid) {
            let current = me.dom.find('tr.hover');
            if (current.length) {
                let row = me.lookupGrid.table.row(current).data();
                me.selectValue([row.id, row.present]);
            }
        } else {
            me.callSuper();
        }
    },
    hoverItem(direction) {
        let me = this;
        if (me.lookupGrid) {
            let current = me.dom.find('tr.hover');
            if (!current.length) {
                current = me.dom.find('tr.selected:first');
            }
            if (!current.length) {
                current = me.dom.find('tbody tr:first');
            }
            let to = direction === 'up' ? current.prev() : current.next();
            if (!to.length) {
                to = direction === 'up' ? me.dom.find('tbody tr:last') : me.dom.find('tbody tr:first');
            }
            me.dom.find('tr.hover').removeClass('hover');
            to.addClass('hover');
        } else {
            me.callSuper(direction);
        }
    },
    loadPresent(id, input) {
        let me = this;
        if (me.design) {
            return;
        }
        jmaa.rpc({
            model: me.owner.model,
            module: me.owner.module,
            method: "searchByField",
            args: {
                relatedField: me.name,
                criteria: [['id', '=', id]],
                fields: ["present"],
                limit: 1,
            },
            context: {
                active_test: false,
                company_test: false,
            },
            onsuccess(result) {
                let value = result.data.values;
                if (value.length > 0) {
                    input.val(value[0].present);
                    if (me.link) {
                        me.dom.addClass('link');
                    }
                    me.dom.attr('data-value', value[0].id).data('text', value[0].present);
                }
            }
        });
    },
    showLinkModel(rec) {
        let me = this;
        me.linkDialog = jmaa.showDialog({
            id: 'm2o-' + me.id,
            submitText: '保存'.t(),
            title: '打开'.t() + ' ' + me.label.t(),
            init(dialog) {
                dialog.body.html(`<iframe style="width:100%;height:100%;border:0;padding-right:1px;margin-bottom:-8px" src="${jmaa.web.getTenantPath()}/view#model=${me.comodel}&views=form&top=1${rec.id ? '&id=' + rec.id : '&present=' + rec.present}"></iframe>`);
                dialog.frame = dialog.body.find('iframe');
                dialog.body.css('height', $(document.body).height() - 180);
            }
        });
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
        if (me.design) {
            callback({data: []});
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "searchByField",
            args: {
                relatedField: me.name,
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
    lookup() {
        let me = this;
        me.dom.find('.lookup-info').show().html('加载中'.t());
        me.dom.find('.lookup-data').hide();
        let fields = ['present'];
        if (me.lookupGrid) {
            fields.push(...me.lookupGrid.getFields())
        }
        me.searchRelated(r => {
            if (r.data?.values?.length) {
                me.renderItems(r.data.values);
            } else {
                me.renderNoData(me.dom.find('input').val());
            }
            me.placeDropdown();
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
    renderNoData(newValue) {
        let me = this;
        me.dom.find('.lookup-data ul').html('');
        let nodata = function () {
            let html = `<div>${me.noDataText.t()}</div>`;
            if (me.canCreate) {
                html += `<button type="button" class="btn btn-create"><i class="mr-2">${'创建'.t()}</i>"${newValue}"</button>`;
            }
            me.dom.find('.lookup-info').html(html);
        }
        if (me.canCreate == undefined) {
            jmaa.rpc({
                model: 'rbac.security',
                method: "canCreate",
                args: {
                    model: me.comodel
                },
                onsuccess(r) {
                    me.canCreate = r.data;
                    nodata();
                }
            });
        } else {
            nodata();
        }
    },
    renderItems(values) {
        let me = this;
        me.dom.find('.lookup-info').hide();
        let body = me.dom.find('.lookup-data').show();
        me.data = values;
        if (me.lookupGrid) {
            me.lookupGrid.selected = [];
            me.lookupGrid.load();
            me.autoSelect(values);
            return;
        }
        let options = [];
        $.each(values, function () {
            let selected = this[me.valueField] === me.dom.attr('data-value') ? ` selected` : '';
            options.push(`<li class="options${selected}" value="${this[me.valueField]}">${this[me.displayField]}</li>`);
        });
        body.html(`<ul>${options.join('')}</ul>`);
        me.autoSelect(values);
    },
    selectValue(value) {
        let me = this;
        me.offset = 0;
        me.keyword = '';
        me.setValue(value);
        me.hideDropdown();
        me.dom.find('input.form-control').focus();
    },
    autoSelect(values) {
        let me = this;
        if (window.env.testing || values.length == 1 && me.autoSelectOption) {
            let item = values[0];
            me.setValue([item[me.valueField], item[me.displayField]]);
            me.dom.find('.select-input').select();
        }
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
        let input = me.dom.find('input');
        if (jQuery.isArray(value) && value[0] && value[1]) {
            input.val(value[1]);
            if (me.link) {
                me.dom.addClass('link');
            }
            if (!me.lookupGrid) {
                me.dom.find('li.selected').removeClass('selected');
                me.dom.find(`li[value="${value[0]}"]`).addClass('selected');
            }
            me.dom.attr('data-value', value[0]).data('text', value[1]).trigger('valueChange');
        } else if (value && typeof value === "string") {
            me.loadPresent(value, input);
        } else {
            input.val('');
            if (me.link) {
                me.dom.removeClass('link');
            }
            if (!me.lookupGrid) {
                me.dom.find('li.selected').removeClass('selected');
            }
            me.dom.attr('data-value', '').data('text', '').trigger('valueChange');
        }
    },
});

jmaa.searchEditor('many2one', {
    extends: "editors.many2one",
    link: false,
    activeTest: false,
    companyTest: false,
    getCriteria() {
        let value = this.getRawValue();
        if (value) {
            return [[this.name, '=', value]];
        }
        return [];
    },
    getText() {
        return this.dom.find('input').val();
    },
});
