/**
 * 多对多编辑控件
 */
jmaa.editor('many2many', {
    css: 'e-x2many',
    /**
     * 查找数据分页大小
     */
    lookupLimit: 10,
    limit: 10,
    activeTest: true,
    companyTest: true,
    /**
     * 获取控件模板
     * @returns
     */
    getTpl() {
        return `<div id="${this.getId()}">
                    <div class="float-right" role="pager"></div>
                    <div role="tbar" class="toolbar"></div>
                </div>
                <div class="grid-table">${'加载中'.t()}</div>`;
    },
    /**
     * 初始化控件
     */
    init() {
        const me = this;
        const dom = me.dom;
        me.paging = me.nvl(eval(dom.attr('pager')), 1);
        me.values = []
        me.delete = [];
        me.create = [];
        dom.html(me.getTpl()).addClass('d-grid');
        me.initPager();
        me.initToolbar();
        me.initGrid();
    },
    initGrid() {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadFields',
            args: {
                model: me.field.comodel,
            },
            onsuccess(r) {
                me._fields = r.data.fields;
                me.lookupPresent = r.data.present;
                me.grid = me.dom.children('.grid-table').JGrid({
                    model: me.field.comodel,
                    module: me.module,
                    arch: me.arch,
                    fields: me._fields,
                    owner: me,
                    editable: true,
                    view: me.view,
                    vid: me.owner.vid + me.field.name,
                    customizable: true,
                    design: me.design,
                    limit: me.limit,
                    pager: me.pager,
                    on: {
                        selected(e, grid, sel) {
                            me.onSelect(e, grid, sel);
                        },
                        delete() {
                            me.removeValue();
                        },
                        create() {
                            me.addValue();
                            return true;
                        },
                    },
                    ajax(grid, callback, data, settings) {
                        me.loadData(grid, callback, data, settings);
                    },
                    reload() {
                        me.load();
                    }
                });
                me.toolbar.target = me.grid;
            },
        });
    },
    initPager() {
        const me = this;
        me.pager = new JPager({
            dom: me.dom.find('[role=pager]'),
            pageChange(e, pager) {
                me.data = null;
                me.grid.load();
            },
            counting(e, pager) {
                me.countData(pager);
            },
        });
        if (me.paging) {
            me.dom.find('[role=tbar]').css('min-height', '28px');
        } else {
            me.pager.hide();
        }
    },
    /**
     * 选中行
     * @param e Event
     * @param grid 表格
     * @param sel 选中的id列表
     */
    onSelect(e, grid, sel) {
        const me = this;
        const selected = [];
        $.each(sel, function (i, id) {
            $.each(grid.data, function () {
                if (this.id === id) {
                    selected.push(this);
                }
            });
        });
        if (me.toolbar) {
            me.toolbar.update(selected);
        }
    },
    /**
     * 初始化工具条
     */
    initToolbar() {
        let me = this;
        let arch = jmaa.utils.parseXML(me.arch).children('grid').children('toolbar').prop('outerHTML') || '<toolbar/>';
        me.toolbar = new JToolbar({
            arch,
            auths: '@all',
            dom: me.dom.find('[role=tbar]'),
            buttons: 'default',
            defaultButtons: 'create|delete|export',
            buttonsTpl: {
                create: `<button name="create" class="btn-primary btn-edit-group" t-click="create">${'添加'.t()}</button>`,
                delete: `<button name="delete" t-enable="ids" class="btn-danger btn-edit-group" t-click="delete" confirm="${'确定删除?'.t()}">${'删除'.t()}</button>`,
                export: `<button name="export" auth="read" position="after" t-click="export">${'导出'.t()}</button>`,
            },
            target: me.grid,
            view: me.view,
        });
    },
    /**
     * 加载数据
     * @param grid 表格
     * @param callback 表格绑定数据的回调
     * @param data DataTable的参数
     * @param settings DataTable的参数
     */
    loadData(grid, callback, data, settings) {
        const me = this;
        if (me.data || me.design) {
            callback({
                data: me.data || [],
            });
        } else {
            let search = function () {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'searchByField',
                    args: {
                        ids: [me.owner.dataId],
                        relatedField: me.field.name,
                        nextTest: true,
                        offset: grid.pager.getOffset(),
                        limit: grid.pager.getLimit(),
                        fields: grid.getFields(),
                        order: grid.getSort(),
                    },
                    context: {
                        usePresent: true,
                        active_test: false,
                        company_test: false,
                    },
                    onsuccess(r) {
                        me.data = r.data.values;
                        if (me.paging && r.data.values.length > 0) {
                            me.pager.update(r.data);
                        } else {
                            me.pager.noData();
                        }
                        callback({
                            data: r.data.values,
                        });
                    },
                });
            }
            if (me.delete.length || me.create.length) {
                jmaa.msg.confirm({
                    title: '提示'.t(),
                    content: '数据未保存，是否继续？'.t(),
                    submit() {
                        me.delete = [];
                        me.create = [];
                        search();
                    },
                    cancel() {
                        callback({data: grid.data});
                    }
                })
            } else {
                search();
            }
        }
    },
    countData(pager) {
        const me = this;
        if (me.design) {
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'countByField',
            args: {
                ids: [me.owner.dataId],
                relatedField: me.field.name,
            },
            context: {
                active_test: false,
                company_test: false,
            },
            onsuccess(r) {
                pager.update({
                    total: r.data,
                });
            },
        });
    },
    filterCriteria() {
        let me = this;
        let values = me.getRawValue();
        let kw = $(`#dialog-${me.getId()} .lookup-input`).val().trim();
        return [
            ['id', 'not in', values],
            ['present', 'like', kw],
        ];
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
    load() {
        let me = this;
        me.data = null;
        me.grid.load();
    },
    lookupCountData(pager) {
        let me = this;
        if (me.design) {
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'countByField',
            args: {
                relatedField: me.field.name,
                criteria: me.getFilter(),
            },
            context: {
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                pager.update({
                    total: r.data,
                });
            },
        });
    },
    /**
     * 查找数据
     * @param grid 表格
     * @param callback 表格绑定数据的回调
     * @param data DataTable的参数
     * @param settings DataTable的参数
     */
    lookupData(grid, pager, callback, data, settings) {
        const me = this;
        if (me.design) {
            callback({data: []});
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: me.field.name,
                criteria: me.getFilter(),
                nextTest: true,
                offset: pager.getOffset(),
                limit: pager.getLimit(),
                fields: grid.getFields(),
                order: grid.getSort(),
            },
            context: {
                active_test: me.activeTest,
                company_test: me.companyTest,
                usePresent: true,
            },
            onsuccess(r) {
                if (r.data.values.length > 0) {
                    pager.update(r.data);
                } else {
                    pager.noData();
                }
                callback({
                    data: r.data.values,
                });
                me.lookupGrid.data = {};
                $.each(r.data.values, function (i, v) {
                    me.lookupGrid.data[v.id] = v;
                });
            },
        });
    },
    submitData(selected) {
        const me = this;
        for (let i = 0; i < selected.length; i++) {
            const id = selected[i];
            const row = me.lookupGrid.data[id];
            if (me.delete.indexOf(id) > -1) {
                me.delete.remove(id);
            } else {
                me.create.push(id);
            }
            me.data.push(row);
        }
        me.grid.table.draw();
        me.dom.triggerHandler('valueChange', [me]);
    },
    /**
     * 添加数据
     */
    addValue() {
        let me = this;
        jmaa.showDialog({
            id: 'dialog-' + me.getId(),
            title: '选择'.t(),
            init(dialog) {
                let placeholder = [];
                for (let name of me.lookupPresent || []) {
                    let field = me._fields[name];
                    let label = field.label || field.name;
                    if (label) {
                        placeholder.push(label.t());
                    }
                }
                dialog.body.html(`<div class="btn-row mt-2">
                                    <div class="input-group w-auto">
                                        <input type="text" class="form-control lookup-input" placeholder="${placeholder.length ? placeholder.join('、') : ''}"/>
                                        <div class="input-suffix">
                                            <button type="button" class="btn btn-default btn-lookup">
                                                <i class="fa fa-search"></i>
                                            </button>
                                        </div>
                                    </div>
                                    <div class="lookup-pager float-right"></div>
                                </div>
                                <div class="lookup-grid"></div>`);
                let pager = new JPager({
                    dom: dialog.body.find('.lookup-pager'),
                    limit: me.lookupLimit,
                    buttonOnly: true,
                    pageChange(e, pager) {
                        me.lookupGrid.load();
                    },
                    counting(e, pager) {
                        me.lookupCountData(pager);
                    },
                });
                me.lookupGrid = dialog.body.find('.lookup-grid').JGrid({
                    checkSelect: true,
                    model: me.field.comodel,
                    module: me.module,
                    arch: me.arch,
                    fields: me._fields,
                    view: me.view,
                    pager: pager,
                    on: {
                        rowDblClick(e, grid, id) {
                            me.submitData([id]);
                            dialog.close();
                        }
                    },
                    ajax(grid, callback, data, settings) {
                        me.lookupData(grid, pager, callback, data, settings);
                    },
                });
                dialog.body.on('click', '.btn-lookup', function () {
                    me.lookupGrid.pager.reset();
                    me.lookupGrid.load();
                }).on('keyup', '.lookup-keyword', function (event) {
                    if (event.keyCode === 13) {
                        me.lookupGrid.load();
                    }
                });
            },
            submit(dialog) {
                me.submitData(me.lookupGrid.getSelected());
                dialog.close();
            }
        });
    },
    /**
     * 移除数据
     */
    removeValue() {
        const me = this;
        $.each(me.grid.getSelected(), function (i, item) {
            if (me.create.indexOf(item) > -1) {
                me.create.remove(item);
            } else {
                me.delete.push(item);
            }
            for (let i = 0; i < me.data.length; i++) {
                if (me.data[i].id === item) {
                    me.data.splice(i, 1);
                    break;
                }
            }
        });
        me.grid.select();
        me.grid.table.draw();
        me.dom.triggerHandler('valueChange', [me]);
    },
    /**
     * 注册值变更事件
     * @param handler 处理函数
     */
    onValueChange(handler) {
        this.dom.on('valueChange', handler);
    },
    /**
     * 设置为只读状态
     * @param v
     */
    setReadonly(readonly) {
        let me = this;
        let toolbar = me.dom.find('[role=tbar]');
        if (readonly) {
            toolbar.find('.btn-group').hide();
            toolbar.find('[auth=read]').parent('.btn-group').show();
        } else {
            toolbar.find('.btn-group').show();
        }
    },
    /**
     * 获取用于提交的数据
     * 指令说明：
     *  create:[0, 0, {values}]
     *  update:[1, id, {values}]
     *  delete:[2, id, 0]
     *  unlink:[3, id, 0]
     *  link:[4, id, 0]
     *  clear:[5, 0, 0]
     *  set:[6, 0, [ids]]
     * @returns {[]}
     */
    getDirtyValue() {
        const me = this;
        const v = [];
        for (let i = 0; i < me.create.length; i++) {
            v.push([4, me.create[i], 0]);
        }
        for (let i = 0; i < me.delete.length; i++) {
            v.push([3, me.delete[i], 0]);
        }
        return v;
    },
    getRawValue() {
        let me = this;
        let values = [];
        for (let value of me.values) {
            values.push(value);
        }
        for (let value of me.create) {
            values.push(value);
        }
        return values;
    },
    resetValue() {
        let me = this;
        //暂时不支持
    },
    /**
     * 获取值[id1, id2]
     */
    getValue() {
        let me = this;
        return me.values;
    },
    /**
     * 设置值
     * @param v
     */
    setValue(value) {
        let me = this;
        me.values = value || [];
        delete me.data;
        me.delete = [];
        me.create = [];
        me.grid?.load();
    },
});
