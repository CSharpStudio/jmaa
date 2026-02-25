/**
 * 多对一编辑控件
 */
jmaa.editor('one2many', {
    css: 'e-x2many',
    /**
     * 默认加载10行
     */
    limit: 10,
    buttonsTpl: {
        create: `<button name="create" class="btn-primary btn-edit-group" t-click="create">${'添加'.t()}</button>`,
        edit: `<button name="edit" auth="update" class="btn-primary btn-edit-group" t-enable="id" t-click="edit">${'编辑'.t()}</button>`,
        delete: `<button name="delete" t-enable="ids" class="btn-danger btn-edit-group" t-click="delete" confirm="${'确定删除?'.t()}">${'删除'.t()}</button>`,
        reload: `<button name="reload" auth="read" position="after" t-click="reload">${'刷新'.t()}</button>`,
        import: `<button name="import" auth="create|update" class="btn-default btn-edit-group" position="after" t-click="import">${'导入'.t()}</button>`,
        export: `<button name="export" auth="read" position="after" t-click="export">${'导出'.t()}</button>`,
    },
    /**
     * 控件模板
     * @returns
     */
    getTpl() {
        return `<div id="${this.getId()}" class="header">
                    <div role="tbar" class="toolbar"></div>
                    <div role="pager"></div>
                </div>
                <div class="grid-table">${'加载中'.t()}</div>`;
    },
    /**
     * 初始化多对一编译控件
     */
    init() {
        let me = this;
        let dom = me.dom;
        me.paging = me.nvl(eval(dom.attr('pager')), true);
        me.limit = me.nvl(eval(dom.attr('limit')), me.paging ? me.limit : me.field.limit);
        me.delete = [];
        me.create = [];
        me.update = [];
        me.data = [];
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
                    dialog: eval(me.dom.attr('dialog') || 1),
                    design: me.design,
                    pager: me.pager,
                    limit: me.limit,
                    on: {
                        selected(e, grid, sel) {
                            me.onSelect(e, grid, sel);
                        },
                        rowDblClick(e, grid, id) {
                            me.rowDblClick(e, grid, id);
                        },
                        save(e, grid, dirty, data) {
                            me.submitEdit(dirty, data);
                        },
                        delete() {
                            me.deleteData();
                        },
                    },
                    loadEdit(grid, id, callback) {
                        me.readData(grid, id, callback);
                    },
                    ajax(grid, callback, data, settings) {
                        if (me.design) {
                            return callback({data: []});
                        }
                        if (me.data && !grid.exporting) {
                            let sort = me.getSort(grid);
                            me.sortForList(me.data, sort);
                            return callback({
                                data: me.data,
                            });
                        }
                        if (!grid.exporting && (me.update.length || me.delete.length || me.create.length)) {
                            jmaa.msg.confirm({
                                title: '提示'.t(),
                                content: '数据未保存，是否继续？'.t(),
                                submit() {
                                    me.update = [];
                                    me.delete = [];
                                    me.create = [];
                                    me.searchData(grid, callback, data, settings);
                                },
                                cancel() {
                                    callback({data: grid.data});
                                }
                            })
                        } else {
                            me.searchData(grid, callback, data, settings);
                        }
                    },
                    moveUp(seq) {
                        me.moveUp(seq);
                    },
                    moveDown(seq) {
                        me.moveDown(seq);
                    },
                    reload() {
                        me.grid.select();
                        me.load();
                    },
                });
                me.toolbar.target = me.grid;
            },
        });
    },
    initPager() {
        let me = this;
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
     * 初始化工具条
     */
    initToolbar() {
        let me = this;
        let arch = jmaa.utils.parseXML(me.arch).children('grid').children('toolbar').prop('outerHTML') || '<toolbar/>';
        let auths = 'read';
        if (!me.readonly()) {
            if (me.view.auths == '@all') {
                auths = '@all';
            } else {
                for (const auth of me.view.auths) {
                    if (auth[me.field.comodel]) {
                        auths = auth[me.field.comodel];
                        auths.push('read');
                        break;
                    }
                }
            }
        }
        me.toolbar = new JToolbar({
            dom: me.dom.find('[role=tbar]'),
            arch,
            auths,
            defaultButtons: 'create|edit|delete|import|export|reload',
            buttonsTpl: me.buttonsTpl,
            target: me.grid,
            view: me.view,
            design: me.design,
        });
    },
    filterCriteria() {
        let me = this;
        return [[me.field.inverseName, '=', me.owner.dataId]];
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
        me.grid && me.grid.load();
    },
    countData(pager) {
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
    /**
     * 查询数据
     * @param grid 表格
     * @param callback 表格绑定数据的回调
     * @param data DataTable的参数
     * @param settings DataTable的参数
     */
    searchData(grid, callback, data, settings) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: me.field.name,
                criteria: me.getFilter(),
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
                me.renderData(r.data, callback);
            },
        });
    },
    renderData(data, callback) {
        let me = this;
        me.data = data.values;
        if (me.paging && data.values.length > 0) {
            me.pager.update(data);
        } else {
            me.pager.noData();
        }
        callback && callback({
            data: data.values,
        });
    },
    /**
     * 根据id读取数据
     * @param grid 表格
     * @param id 主键 new开头的表示新建的数据，需要从create中找
     * @param callback 回调
     */
    readData(grid, id, callback) {
        let me = this;
        if (!id) return;
        if (id.startsWith('new')) {
            let data = me.create.find(item => item.id === id);
            callback({data});
        } else {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'searchByField',
                args: {
                    relatedField: me.field.name,
                    criteria: [['id', '=', id]],
                    fields: grid.editForm.getFields(),
                },
                context: {
                    usePresent: true,
                },
                onsuccess(r) {
                    let data = r.data.values[0];
                    let dirty = me.update.find(item => item.id === id);
                    if (dirty) {
                        // 使用data数据更新一次，没有使用update的数据更新多次
                        let item = me.data.find(item => item.id === id);
                        $.extend(data, item);
                    }
                    callback({
                        data: data,
                    });
                },
            });
        }
    },
    /**
     * 双击事件
     * @param e Event
     * @param grid 表格
     * @param id id
     */
    rowDblClick(e, grid, id) {
        let me = this;
        me.toolbar.dom.find("[name='edit']").click();
    },
    /**
     * 选中行
     * @param e Event
     * @param grid 表格
     * @param ids 选中的id列表
     */
    onSelect(e, grid, ids) {
        let me = this;
        let selected = [];
        let parent = function () {
            let data = me.owner.getRawData();
            data.id = me.owner.dataId;
            return data;
        }
        $.each(ids, function (i, id) {
            $.each(grid.data, function () {
                if (this.id === id) {
                    selected.push(this);
                }
            });
        });
        if (me.toolbar) {
            me.toolbar.update(selected, parent);
        }
    },
    /**
     * 删除列表所有数据
     */
    deleteAll() {
        let me = this;
        if (me.data) {
            for (let data of me.data) {
                me.grid.getSelected().push(data.id);
            }
            me.deleteData();
        }
    },
    /**
     * 删除数据
     */
    deleteData() {
        let me = this;
        $.each(me.grid.getSelected(), function (i, id) {
            if (id.startsWith('new')) {
                me.create.remove(item => item.id === id);
            } else {
                me.delete.push(id);
            }
            me.data.remove(item => item.id === id);
            me.dom.triggerHandler('valueChange', [me]);
        });
        me.grid.select();
        me.grid.table.draw();
    },
    move(seq, idx) {
        let me = this;
        let preRow = me.data[idx - 1];
        let row = me.data[idx];
        me.data[idx] = preRow;
        me.data[idx - 1] = row;
        let seqValue = row[seq];
        row[seq] = preRow[seq];
        preRow[seq] = seqValue;
        if (row.id.startsWith('new')) {
            let data = me.create.find(function (r) {
                return r.id == row.id;
            });
            data[seq] = row[seq];
        } else {
            let data = me.update.find(function (r) {
                return r.id == row.id;
            });
            if (data) {
                data[seq] = row[seq];
            } else {
                let value = {id: row.id};
                value[seq] = row[seq];
                me.update.push(value);
            }
        }
        if (preRow.id.startsWith('new')) {
            let data = me.create.find(function (r) {
                return r.id == preRow.id;
            });
            data[seq] = preRow[seq];
        } else {
            let data = me.update.find(function (r) {
                return r.id == preRow.id;
            });
            if (data) {
                data[seq] = preRow[seq];
            } else {
                let value = {id: preRow.id};
                value[seq] = preRow[seq];
                me.update.push(value);
            }
        }
        me.grid.load();
        me.dom.triggerHandler('valueChange', [me]);
    },
    moveUp(seq) {
        let me = this;
        let id = me.grid.getSelected()[0];
        for (let idx = 0; idx < me.data.length; idx++) {
            let row = me.data[idx];
            if (idx > 0 && row.id == id) {
                me.move(seq, idx);
                break;
            }
        }
        me.grid.select(id);
    },
    moveDown(seq) {
        let me = this;
        let id = me.grid.getSelected()[0];
        for (let idx = 0; idx < me.data.length; idx++) {
            let row = me.data[idx];
            if (idx < me.data.length - 1 && row.id == id) {
                me.move(seq, idx + 1);
                break;
            }
        }
        me.grid.select(id);
    },
    save(dirty, data) {
        let me = this;
        me.submitEdit(dirty, data);
        me.grid.load();
    },
    /**
     * 保存编辑的数据
     * @param dirty 用于保存的脏数据
     * @param data 用于显示在表格中的数据
     */
    submitEdit(dirty, data) {
        let me = this;
        data = data || $.extend({}, dirty);
        if (data.id) {
            let row = me.data.find(r => r.id == data.id);
            $.extend(row, data);
            if (data.id.startsWith('new')) {
                let row = me.create.find(r => r.id == data.id);
                $.extend(row, dirty);
            } else {
                me.update.push(dirty);
            }
        } else {
            dirty.id = 'new-' + jmaa.nextId();
            data.id = dirty.id;
            me.create.push(dirty);
            me.data.push(data);
        }
        me.dom.triggerHandler('valueChange', [me]);
    },
    /**
     * 注册值变更事件
     * @param handler 处理函数
     */
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            handler(e, me);
        });
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
     * 获取值
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
        let me = this;
        let dirty = [];
        for (let i = 0; i < me.create.length; i++) {
            let values = {};
            $.extend(values, me.create[i]);
            delete values.id;
            dirty.push([0, 0, values]);
        }
        for (let i = 0; i < me.update.length; i++) {
            let values = {};
            $.extend(values, me.update[i]);
            let id = values.id;
            delete values.id;
            dirty.push([1, id, values]);
        }
        for (let i = 0; i < me.delete.length; i++) {
            dirty.push([2, me.delete[i], 0]);
        }
        return dirty;
    },
    /**
     * 获取数据
     */
    getValue() {
        let me = this;
        return this.data;
    },
    resetValue() {
        let me = this;
        if (me.data.length) {
            for (let row of me.data) {
                if (!row.id.startsWith("new-")) {
                    me.delete.push(row.id);
                }
            }
            me.data = [];
            me.values = [];
            me.create = [];
            me.update = [];
            me.grid?.load();
            me.dom.triggerHandler('valueChange', [me]);
        }
    },
    /**
     * 设置值
     * @param v
     */
    setValue(values) {
        let me = this;
        me.pager.reset();
        me.values = values || [];
        delete me.data;
        me.delete = [];
        me.create = [];
        me.update = [];
        if (me.grid) {
            me.grid.selected = [];
            me.grid.load();
        }
    },
    // 设置data
    setData(data, values) {
        let me = this;
        me.pager.reset();
        me.values = values || [];
        me.data = data || [];
        me.delete = [];
        me.create = [];
        me.update = [];
        me.grid?.load();
    },
    getSort(grid) {
        let me = this;
        let order = [];
        if (grid.table) {
            $.each(grid.table.order(), function (i, o) {
                let ds = grid.table.column(o[0]).dataSrc();
                if (ds) {
                    let x = {};
                    x[ds] = o[1];
                    order.push(x);
                }
            });
        }
        return order;
    },
    sortForList(arr, needSorts) {
        let me = this;
        for (let i = 0; i < needSorts.length; i++) {
            const item = needSorts[i];
            const sortByStr = Object.keys(item)[0]; ///正在排序的字段
            const sortBy = Object.values(item)[0]; //按照升/降序排序
            let m2o = me.grid.fields[sortByStr].type == 'many2one';
            for (let j = 0; j < arr.length; j++) {
                //冒泡排序
                for (let k = 0; k < arr.length - 1 - j; k++) {
                    let val1 = arr[k][sortByStr];
                    let val2 = arr[k + 1][sortByStr];
                    if (m2o && val1 && val1.length == 2) {
                        val1 = val1[1];
                    }
                    if (m2o && val2 && val2.length == 2) {
                        val2 = val2[1];
                    }
                    if (sortBy === 'desc') {
                        //降序
                        if (val1 < val2) {
                            let tmp = arr[k];
                            arr[k] = arr[k + 1];
                            arr[k + 1] = tmp;
                        }
                    } else {
                        //升序
                        if (val1 > val2) {
                            let tmp = arr[k];
                            arr[k] = arr[k + 1];
                            arr[k + 1] = tmp;
                        }
                    }
                }
            }
        }
        return arr;
    },
});
