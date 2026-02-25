/**
 * 查询控件
 * arch:
 *  <search>
 *      <fixed>
 *          <field name="..."/>
 *      </fixed>
 *      <field name="..."/>
 *      <aside>
 *          <field name="..."/>
 *      </aside>
 *  </search>
 */
jmaa.component("JSearch", {
    /**
     * 获取控件模板
     * @returns
     */
    getTpl() {
        return `<div class="jui-search">
                    <div class="input-group">
                        <div class="input-group-prepend">
                            <button data-btn="submitSearch" type="button" class="btn btn-search">
                                <i class="fa fa-search"></i>
                            </button>
                        </div>
                        <div class="jui-search-container">
                            <div class="jui-search-fixed e-form"></div>
                            <div class="input-group">
                                <span class="jui-search-selection">
                                    <ul class="jui-search-selection-body"></ul>
                                </span>
                                <div class="input-group-append">
                                    <button type="button" class="btn btn-search dropdown-toggle" data-btn="dropdownSearch"></button>
                                </div>
                            </div>
                        </div>
                        <div class="container-fluid dropdown-menu search-dropdown" style="min-width:300px">
                            <div class="search-form e-form"></div>
                            <div class="card-footer">
                                <button data-btn="clearSearch" class="btn btn-flat btn-outline-secondary" style="margin-right:5px">${'清空'.t()}</button>
                                <button data-btn="resetSearch" class="btn btn-flat btn-outline-secondary" style="margin-right:5px">${'重置'.t()}</button>
                                <button data-btn="confirmSearch" class="btn btn-flat btn-primary float-right" style="min-width:100px">${'确定'.t()}</button>
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    /**
     * 初始化控件
     */
    init() {
        let me = this, dom = me.dom;
        me.query = {};
        me.editors = {};
        me._fields = [];
        dom.html(me.getTpl())
            .on('click', '.jui-search-selection', function (e) {
                me.showDropdown();
                e.preventDefault();
                e.stopPropagation();
            }).on('click', '[data-btn=clearSearch]', function () {
            me.clear();
        }).on('click', '[data-btn=resetSearch]', function () {
            me.reset();
        }).on('click', '[data-btn=confirmSearch]', function () {
            me.confirm();
        }).on('click', '[data-btn=submitSearch]', function () {
            me._updateCriteria();
            dom.triggerHandler("submitting", [me]);
        }).on('click', '[data-btn=dropdownSearch]', function (e) {
            me.showDropdown();
            e.preventDefault();
            e.stopPropagation();
        });
        me.dropdown = dom.find('.search-dropdown');
        me.body = dom.find('.jui-search-selection-body');
        $(document).on('mousedown', function (e) {
            if ($(e.target).closest('.search-dropdown, .daterangepicker').length == 0) {
                me.hideDropdown();
            }
        });
        me.renderSearch();
        me.onSubmitting(me.submitting);
        me.dom.on('keyup', 'input,select', function (e) {
            if (e.keyCode == 13) {
                me.confirm();
            }
        });
        me.dom.triggerHandler('init', [me]);
    },
    clear() {
        let me = this;
        me.query = {};
        me.body.empty();
        $.each(me._fields, function (i, field) {
            me.editors[field].setValue('');
        });
        me._updateCriteria();
        me.dom.triggerHandler("submitting", [me]);
    },
    /**
     * 确定条件
     */
    confirm() {
        let me = this;
        me._updateCriteria();
        me.dropdown.removeClass('show');
        me.dom.triggerHandler("submitting", [me]);
        me.hideDropdown();
    },
    /**
     * 注册提交事件
     * @param handler
     */
    onSubmitting(handler) {
        this.dom.on("submitting", handler);
    },
    /**
     * 显示下拉条件
     */
    showDropdown() {
        let me = this, el = me.dom.find('.dropdown-menu');
        $(".toolbar div").each((index, element) => {
            if (element.className.indexOf('show') > -1) {
                element.children[1].click()
            }
        })
        el.show().addClass('show');
        el.find('[focusable]:first').focus();

    },
    /**
     * 隐藏下拉条件
     */
    hideDropdown() {
        let me = this;
        me.dom.find('.search-dropdown').hide().removeClass('show');
    },
    /**
     * 初始化aside布局
     * @param arch
     */
    renderAside(arch) {
        let me = this, searchPanel = arch.find('aside');
        if (searchPanel.length > 0) {
            me.panel = new JSearchPanel({
                dom: $("[part=search-panel]").show(),
                arch: searchPanel.prop("outerHTML"),
                model: me.model,
                module: me.module,
                fields: me.fields,
                on: {
                    selected(e, panel, node) {
                        me.dom.triggerHandler("submitting", [me]);
                    },
                },
                ajax(panel, callback) {
                    jmaa.rpc({
                        model: me.model,
                        module: me.module,
                        method: "searchByField",
                        args: {
                            relatedField: panel.field.name,
                            criteria: panel.getLookup(),
                            fields: panel.getFields(),
                            limit: panel.limit
                        },
                        onsuccess(r) {
                            callback(r.data.values);
                        }
                    });
                }
            });
            me.panel.load();
        }
        searchPanel.remove();
    },
    /**
     * 初始化下拉条件
     * @param arch
     */
    renderDropdown(arch) {
        let me = this;
        let fields = arch.children('field');
        let col = arch.col || (fields.length <= 3 ? 1 : fields.length <= 6 ? 2 : 3);
        me.dropdown.css('max-width', col == 3 ? '100%' : col == 2 ? '66.666667%' : '33.333333%');
        me.dropdown.find('.search-form').css('grid-template-columns', `repeat(${col},1fr)`);
        let html = [];
        fields.each(function (i, e) {
            let el = $(e);
            let name = el.attr('name');
            let field = me.fields[name] || {};
            if (!field.deny) {
                let label = el.attr('label');
                let val = el.attr('default') || el.attr('defaultValue');
                let op = el.attr('op');
                let criteria = el.attr('criteria');
                let lookup = el.attr('lookup');
                let attrs = '';
                let editor = el.attr('editor');
                if (!label) {
                    label = field.label || field.name || name;
                }
                $.each(this.attributes, function (i, attr) {
                    let v = jmaa.utils.encode(attr.value);
                    if (['name', 'editor', 'label'].indexOf(attr.name) == -1) {
                        attrs += attr.name + '="' + v + '" ';
                    }
                });
                label = label.t();
                name = name.replaceAll('\.', '__');
                html.push(`<div class="form-group"><label>${label}</label>
                    <div data-label="${label}" data-field="${name}"
                    ${val ? ` data-default="${val}"` : ''}
                    ${op ? ` data-op="${op}"` : ''}
                    ${editor ? ` data-editor="${editor}"` : ''}
                    ${criteria ? ` data-criteria="${jmaa.utils.encode(criteria)}"` : ''}
                    ${lookup ? ` lookup="${jmaa.utils.encode(lookup)}"` : ''}
                    ${attrs}></div></div>`);
            }
        });
        me.dropdown.find('.search-form').prepend(html.join(''));
    },
    /**
     * 初始化固定条件
     * @param arch
     */
    renderFixed(arch) {
        let me = this;
        let fixed = arch.find("fixed>field");
        if (fixed.length) {
            let html = [];
            fixed.each(function (i, e) {
                let el = $(e);
                let name = el.attr('name');
                let field = me.fields[name] || {};
                if (!field.deny) {
                    let label = el.attr('label');
                    let val = el.attr('default');
                    let op = el.attr('op');
                    let criteria = el.attr('criteria');
                    let attrs = "";
                    let editor = el.attr('editor');
                    if (!label) {
                        label = field.label || field.name || name;
                    }
                    $.each(this.attributes, function (i, attr) {
                        let v = jmaa.utils.encode(attr.value);
                        if (['name', 'editor'].indexOf(attr.name) == -1) {
                            attrs += attr.name + '="' + v + '" ';
                        }
                    });
                    label = label.t();
                    name = name.replaceAll('\.', '__');
                    html.push(`<div class="search-fixed-item">
                        <label>${label}</label>
                        <div data-label="${label}" data-field="${name}" ${val ? ` data-default="${val}"` : ''}
                            ${op ? ` data-op="${op}"` : ''}
                            ${editor ? ` data-editor="${editor}"` : ''}
                            ${criteria ? ` data-criteria="${jmaa.utils.encode(criteria)}"` : ''}
                            ${attrs}>
                        </div>
                    </div>`);
                }
            });
            me.dom.find('.jui-search-fixed').html(html.join(''));
        } else {
            me.dom.find('.jui-search-fixed').hide();
        }
    },
    /**
     * 初始化查询控件
     */
    renderSearch() {
        let me = this;
        if (me.arch) {
            let arch = jmaa.utils.parseXML(me.arch).children('search');
            me.renderAside(arch);
            me.renderFixed(arch);
            me.renderDropdown(arch);
            me.criteria = eval(arch.attr('criteria'));
            me.dom.find('[data-field]').each(function () {
                let el = $(this),
                    name = el.attr('data-field'),
                    field = me.fields[name] || {},
                    editor = el.attr('data-editor') || field.type,
                    ctl = jmaa.searchEditors[editor];
                if (!ctl) {
                    window.postError && window.postError(name + '找不到编辑器:' + editor);
                    return console.error(name + '找不到编辑器:' + editor);
                }
                me._fields.push(name);
                me.editors[name] = new ctl({
                    dom: el,
                    name,
                    field: field,
                    model: me.model,
                    module: me.module,
                    owner: me,
                    allowNull: true,
                    op: el.attr('data-op'),
                    criteria: el.attr('data-criteria'),
                    label: el.attr('data-label')
                });
            });
        }
        me._reset();
    },
    /**
     * 更新查询条件
     * @private
     */
    _updateCriteria() {
        let me = this;
        for (let field of me._fields) {
            delete me.query[field];
            me.body.find('[data-field=' + field + ']').remove();
        }
        $.each(me._fields, function (i, field) {
            let editor = me.editors[field];
            if (editor.criteria) {
                let val = editor.getRawValue ? editor.getRawValue() : editor.getValue();
                let hasValue = val;
                if ($.isArray(val)) {
                    hasValue = val.length;
                }
                if (hasValue) {
                    let expr = jmaa.utils.decode(editor.criteria),
                        f = new Function("value", "return " + expr + ";");
                    let criteria = f(val);
                    me.remove(field + '-col');
                    me.add(field, editor.label, editor.getText(), criteria);
                }
            } else {
                let criteria = editor.getCriteria();
                if (criteria.length > 0) {
                    me.remove(field + '-col');
                    me.add(field, editor.label, editor.getText(), criteria);
                }
            }
        });
    },
    _reset() {
        let me = this;
        $.each(me._fields, function (i, field) {
            me.editors[field].setValue('');
        });
        me.dom.find('[data-default]').each(function () {
            let e = $(this),
                val = e.attr('data-default'),
                name = e.attr('data-field'),
                editor = me.editors[name];
            if (val == 'env.user') {
                val = [env.user.id, env.user.name];
            } else if (val == 'env.company') {
                val = [env.company.id, env.company.name];
            } else if (val == 'env.user.id' || val == 'env.company.id' || val == '[env.company.id]') {
                val = eval(val);
            }
            if (editor.xtype.includes('2many')) {
                val = [val];
            }
            editor.setValue(val);
        });
        me._updateCriteria();
    },
    /**
     * 重置查询
     */
    reset() {
        let me = this;
        me._reset();
        me.dom.triggerHandler("submitting", [me]);
    },
    /**
     * 添加条件显示
     * @param field
     * @param label
     * @param text
     * @param expr
     */
    add(field, label, text, expr) {
        let me = this;
        me.query[field] = expr;
        let svg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 512 512"><path d="M256 48C141.31 48 48 141.31 48 256s93.31 208 208 208s208-93.31 208-208S370.69 48 256 48zm75.31 260.69a16 16 0 1 1-22.62 22.62L256 278.63l-52.69 52.68a16 16 0 0 1-22.62-22.62L233.37 256l-52.68-52.69a16 16 0 0 1 22.62-22.62L256 233.37l52.69-52.68a16 16 0 0 1 22.62 22.62L278.63 256z" fill="currentColor"></path></svg>`
        let html = `<li class="jui-search-choice"  data-field="${field}">
                <p class="filter-name">${label}</p>
                <p class="filter-value"><span>${text}</span><span class="jui-search-choice-remove" role="presentation">${svg}</span></p>
            </li>`;
        me.body.append(html);
        let el = me.body.find('[data-field=' + field + ']');
        el.on('click', function (e) {
            e.stopPropagation();
        }).on('click', '.jui-search-choice-remove', function (e) {
            me.remove(field);
            me.dom.triggerHandler("submitting", [me]);
            e.stopPropagation();
        });
    },
    /**
     * 显示条件
     * @param field
     */
    remove(field) {
        let me = this;
        delete me.query[field];
        me.editors[field]?.setValue('');
        me.body.find('[data-field=' + field + ']').remove();
    },
    /**
     * 添加条件
     */
    addCriteria(field, label, text, criteria) {
        let me = this;
        me.add(field, label, text, criteria);
        me.dom.triggerHandler("submitting", [me]);
    },
    setCriteria() {

    },
    /**
     * 获取条件
     * @returns {[]}
     */
    getCriteria() {
        let me = this, criteria = [], vals = Object.values(me.query);
        $.each(vals, function () {
            $.each(this, function () {
                criteria.push(this);
            });
        });
        if (me.criteria) {
            $.each(me.criteria, function () {
                criteria.push(this);
            });
        }
        if (me.panel) {
            $.each(me.panel.getCriteria(), function () {
                criteria.push(this);
            });
        }
        return criteria;
    },
    /**
     * 获取存储格式的数据
     * @returns {{}}
     */
    getRawData() {
        let me = this, data = {};
        $.each(me._fields, function (i, field) {
            let editor = me.getEditor(field);
            if (editor.getRawValue) {
                data[field] = editor.getRawValue();
            } else {
                data[field] = editor.getValue();
            }
        });
        return data;
    },
    /**
     * 获取编辑器
     * @param name
     * @returns {*}
     */
    getEditor(name) {
        let e = this.editors[name];
        if (!e) {
            throw new Error("找不到name=[" + name + "]的editor");
        }
        return e;
    },
    getSelected() {
        return [];
    }
});
