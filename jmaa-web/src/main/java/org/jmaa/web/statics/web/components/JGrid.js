/**
 * JGrid表格，使用DataTable生成界面
 * arch:
 *  <grid>
 *      <toolbar/>
 *      <field name="..."/>
 *  </grid>
 */
jmaa.component('JGrid', {
    /**
     * 分页数
     */
    limit: 50,
    /**
     * 是否允许编辑，是则表格行内或者对话框编辑，否则转到表单编辑
     */
    editable: false,
    /**
     * 是否对话框编辑
     */
    dialog: false,
    /**
     * 是否显示行号
     */
    showRowNum: true,
    /**
     * 是否允许多选
     */
    multiSelect: true,
    /**
     * 是否通过勾选框选中
     */
    checkSelect: true,
    filterColumn: false,
    ordering: true,
    /**
     * 是否可配置
     */
    customizable: false,
    /**
     * ajax请求，通过callback绑定数据
     * @param grid 表格
     * @param callback DataTable的绑定回调
     * @param data DataTable的参数
     * @param settings DataTable的参数
     */
    ajax(grid, callback, data, settings) {
        callback({data: []});
    },
    /**
     * 加载编辑的数据
     * @param grid 表格
     * @param id
     * @param callback
     */
    loadEdit(grid, id, callback) {
        jmaa.rpc({
            model: grid.model,
            module: grid.module,
            method: 'read',
            args: {
                ids: [id],
                fields: grid.editForm.getFields(),
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                callback({data: r.data[0]});
            },
        });
    },
    /**
     * 行内编辑的按钮模板
     * @returns
     */
    getEditFBarTpl() {
        return `<td colspan="500"><div class="grid-edit"></div>
                    <div class="grid-edit-tbar">
                        <button name="cancel" type="button" class="btn btn-outline-secondary">${'取消'.t()}</button>
                        <button name="confirm" type="button" class="btn btn-info">${'确定'.t()}</button>
                    </div>
                </td>`;
    },
    /**
     * 初始化JGrid
     */
    init() {
        const me = this;
        const columnDefs = [];
        me._fields = [];
        me._presentFields = [];
        me._customFields = [];
        if (me.arch) {
            const arch = jmaa.utils.parseXML(me.arch);
            const grid = arch.children('grid');
            if (grid.length > 0) {
                me.limit = me.nvl(eval(grid.attr('limit')), me.limit);
                me.dialog = me.nvl(eval(grid.attr('dialog')), me.dialog);
                me.editable = eval(grid.attr('inline') || grid.attr('editable')) || me.dialog || me.editable;
                if (me.pager) {
                    me.pager.limit = me.limit;
                }
                let attrs = {};
                $.each(grid[0].attributes, function (i, attr) {
                    attrs[attr.name] = attr.value;
                });
                me.dom.attr(attrs);
                me.showRowNum = me.nvl(eval(grid.attr('showRowNum')), me.showRowNum);
                me.filterColumn = me.nvl(eval(grid.attr('filterColumn')), me.filterColumn);
                me.customizable = me.nvl(eval(grid.attr('customizable')), me.customizable);
                me.multiSelect = me.nvl(eval(grid.attr('multiSelect')), me.multiSelect);
                me.checkSelect = me.nvl(eval(grid.attr('checkSelect')), me.checkSelect);
                let tb = grid.children('toolbar');
                me.tbarArch = tb.prop('outerHTML');
                tb.remove();
                let edit = grid.children('edit');
                let editArch = $('<form></form>');
                if (edit.length > 0) {
                    editArch.html(edit.prop('innerHTML'));
                    let editAttrs = {};
                    $.each(edit[0].attributes, function (i, attr) {
                        editAttrs[attr.name] = attr.value;
                    });
                    editArch.attr(editAttrs);
                } else {
                    editArch.html(grid.prop('innerHTML'));
                }
                me.editArch = editArch.prop('outerHTML');
                me.isTree = eval(grid.attr('tree'));
                me.footer = me.nvl(eval(grid.attr('footer')), false);
                me.ordering = me.nvl(eval(grid.attr('ordering')), me.ordering);
                if (me.isTree) {
                    me.parentField = grid.attr('parent_field') || 'parent_id';
                    me._fields.push(me.parentField);
                }
                let opt = {
                    html: ['<table class="table table-bordered table-hover"><thead><tr>'],
                    footer: [],
                    columnIndex: 0,
                };
                if (me.checkSelect) {
                    columnDefs.push({
                        searchable: false,
                        orderable: false,
                        data: null,
                        render(data, type, row, opt) {
                            return '<input type="checkbox" class="check-select">';
                        },
                        targets: opt.columnIndex++,
                        createdCell(td, cellData, rowData, row, col) {
                            $(td).addClass('select-column');
                        },
                    });
                    opt.html.push(`<th style="width:1%;padding-right:8px;">
                        <div class="title" style="max-width:18px">
                            ${me.multiSelect ? `<input title="${'全选页数据'.t()}" type="checkbox" class="all-check-select"/>` : ''}
                        </div>
                    </th>`);
                    opt.footer.push('<td></td>');
                }
                if (me.showRowNum) {
                    columnDefs.push({
                        searchable: false,
                        orderable: false,
                        data: null,
                        render(data, type, row, opt) {
                            return opt.row + 1;
                        },
                        targets: opt.columnIndex++
                    });
                    opt.html.push('<th style="width:1%;padding-right:10px;"><span class="title" style="max-width:10px">#</span></th>');
                    opt.footer.push('<td></td>');
                }
                me.initColumns(grid, columnDefs, opt);
                me.initHilite(grid);
                if (me.footer) {
                    opt.html.push(`</tr></thead><tfoot><tr>${opt.footer.join('')}</tr></tfoot></table>`);
                } else {
                    opt.html.push('</tr></thead></table>');
                }
                grid.replaceWith(opt.html.join(''));
                me.onEvent('init', grid.attr('on-init'));
                me.onEvent('load', grid.attr('on-load'));
                me.onEvent('create', grid.attr('on-create'));
                me.onEvent('save', grid.attr('on-save'));
                me.onEvent('selected', grid.attr('on-selected'));
                let rowDblClick = grid.attr('on-row-dblclick');
                if (rowDblClick) {
                    //指定双击事件时，移除原来绑定的事件
                    me.dom.off('rowDblClick');
                    me.onEvent('rowDblClick', rowDblClick);
                }
            }
            me.dom.html(arch.children().prop('outerHTML')).addClass('jui-grid');
        }
        me.initTable(columnDefs);
        me.initColumnFilter();
        me.initColumnConfig();
        me.selected = [];
        me.dom.triggerHandler('init', [me]);
        me.redraw = true;
        me.table.ajax.reload();
        me.redraw = false;
    },
    initColumns(grid, columnDefs, opt) {
        let me = this;
        let state = me.loadViewState(me.vid) || {};
        me._hiddenFields = state.field_hidden ? state.field_hidden.split(',') : [];
        let columns = grid.children('field,column');
        columns = me.updateColumnOrder(columns, state);
        $.each(columns, function () {
            let col = $(this);
            let name = col.attr('name');
            let label = col.attr('label');
            let field = col.is('field') ? me.fields[name] : {
                name,
                type: col.attr('type'),
                label,
            };
            if (!field) {
                window.postError && window.postError('模型' + me.model + '找不到字段' + name);
                return console.error('模型' + me.model + '找不到字段' + name);
            }
            if (!field.deny) {
                let css = col.attr('class');
                let style = col.attr('style');
                let visible = me.nvl(eval(col.attr('visible')), true);
                let filter = me.filterColumn && eval(col.attr('filter') || 1);
                if (!label) {
                    label = field.label || field.name;
                }
                label = label.t();
                col.is('field') && me._fields.push(name);
                if (visible) {
                    me._customFields.push({name, label})
                }
                if (field.type === 'many2many' || field.type === 'one2many' || field.type === 'many2one') {
                    me._presentFields.push(name);
                }
                opt.html.push(`<th data-field='${name}' data-label='${label}'`);
                if (style) {
                    opt.html.push(` style="${style}"`);
                }
                opt.html.push(`><div class="title"><span>${label}</span>`);
                let title = col.attr('title');
                if (title) {
                    title = title.replaceAll('<', '&lt;').replaceAll('>', '&gt;');
                    opt.html.push(`<sup class="fa fa-question-circle btn-help p-1" data-role="tooltip" data-tooltip="${title}"></sup>`);
                }
                if (filter) {
                    const f = $('<span class="column-filter"><i class="fa fa-filter column-filter-btn"></i></span>');
                    const criteria = col.attr('criteria');
                    if (criteria) {
                        f.attr('criteria', criteria);
                    }
                    opt.html.push(f[0].outerHTML);
                }
                opt.html.push('</div></th>');
                columnDefs.push({
                    render: new (jmaa.columns[col.attr('column') || field.type] || jmaa.columns.default)({
                        field,
                        arch: col,
                        owner: me,
                    }).render(),
                    data: name,
                    className: css,
                    targets: opt.columnIndex++,
                    orderable: Boolean(eval(col.attr('ordering') || field.sortable)),
                    visible: !me._hiddenFields.includes(name) && visible,
                    createdCell(td, cellData, rowData, row, col) {
                        $(td).attr('data-field', columnDefs[col].data);
                    }
                });
                opt.footer.push('<td></td>');
            }
        });
    },
    initColumnConfig() {
        let me = this;
        if (!me.customizable) {
            return;
        }
        me.dom.addClass('customizable').find('.title').off().on('click', function (e) {
            if ($(this).parent().is(':last-child')) {
                const clickX = e.pageX - $(this).offset().left;
                const width = $(this).outerWidth();
                if (clickX > width) {
                    e.stopPropagation();
                    let html = [];
                    for (let col of me._customFields) {
                        let itemId = 'd-' + jmaa.nextId();
                        let check = me._hiddenFields.includes(col.name) ? '' : 'checked="checked"';
                        html.push(`<div class="e-check">
                            <input type="checkbox" id="${itemId}" data-field="${col.name}" ${check}>
                            <label for="${itemId}"><span class="flex-grow-1">${col.label}</span></label>
                        </div>`);
                    }
                    let menu = $(`<label class="jui-grid-customize dropdown-menu show">
                            ${html.join('')}
                            <div class="border-bottom"></div>
                            <label class="reset text-center m-0">${'恢复默认配置'.t()}</label>
                        </div>`);
                    $('body').append(menu);
                    let top = $(this).offset().top + 10;
                    let winHeight = $(window).height();
                    if (top + menu.height() > winHeight) {
                        menu.css({
                            left: (e.pageX - menu.width()) + 'px',
                            top: 'auto',
                            bottom: '5px',
                            'max-height': (winHeight - 10) + 'px',
                        });
                    } else {
                        menu.css({
                            left: (e.pageX - menu.width()) + 'px',
                            top: top + 'px',
                            bottom: 'auto',
                        });
                    }
                    let timeout;
                    menu.on('change', 'input', function () {
                        let col = $(this);
                        let name = col.attr('data-field');
                        if (col.is(':checked')) {
                            me._hiddenFields.remove(name);
                            me.table.column(`[data-field=${name}]`).visible(true);
                        } else {
                            me._hiddenFields.push(name);
                            me.table.column(`[data-field=${name}]`).visible(false);
                        }
                        clearTimeout(timeout);
                        timeout = setTimeout(function () {
                            me.saveViewState({field_hidden: me._hiddenFields.join()});
                        }, 2000);
                    }).on('click', '.reset', function () {
                        me.saveViewState({field_order: '', field_hidden: ''}, function () {
                            window.location.reload();
                        });
                    });
                }
            }
        });
    },
    initColumnFilter() {
        const me = this;
        if (!me.filterColumn) {
            return;
        }
        me.dom.find('.column-filter').off().on('click', function () {
            return false;
        });
        me.dom.find('.column-filter-btn').off().on('click', function () {
            const btn = $(this);
            const filter = btn.parent();
            const panel = filter.find('.column-filter-panel');
            const thead = filter.parents('th');
            const column = thead.attr('data-field');
            const label = thead.attr('data-label');
            const field = me.fields[column];
            const expr = filter.attr('criteria');
            const submitSearch = function () {
                filter.find('.column-filter-panel').hide();
                me.search.remove(column);
                me.search.remove(column + '-col');
                const input = filter.find('.column-filter-input');
                let val = input.val();
                if (val) {
                    val = val.trim();
                    let text = val;
                    if (field.type == 'selection') {
                        text = field.options[val];
                    }
                    const criteria = [];
                    if (expr) {
                        if (field.type === 'datetime' || field.type === 'date') {
                            val = val.split(' - ');
                        }
                        const q = new Function('value', 'return ' + expr + ';')(val);
                        for (const c of q) {
                            criteria.push(c);
                        }
                    } else {
                        const op = field.type == 'many2one' || field.type == 'many2many' || field.type == 'char' ? 'like' : '=';
                        if (field.type == 'char') {
                            const values = val.trim().split(';');
                            if (values.length > 1) {
                                criteria.push([column, 'in', values]);
                            }
                            if (val.endsWith("$")) {
                                criteria.push([column, '=', val.substr(0, val.length - 1)]);
                            } else {
                                criteria.push([column, 'like', val.trim()]);
                            }
                        } else if (field.type == 'many2one' || field.type == 'many2many') {
                            criteria.push([column + '.present', 'like', val]);
                        } else if (field.type === 'datetime' || field.type === 'date') {
                            const dateArr = val.split(' - ');
                            dateArr.forEach((v, i) => {
                                criteria.push([column, i === 0 ? '>=' : '<=', v]);
                            });
                        } else {
                            if (field.type == 'boolean') {
                                val = eval(val);
                            }
                            criteria.push([column, '=', val]);
                        }
                    }
                    me.search.addCriteria(column + '-col', label, text, criteria);
                }
                input.val('');
                me.load();
            };
            const getEditor = function (field) {
                if (field.type == 'selection' || field.type == 'boolean') {
                    let editor = `<select class='column-filter-input'>
                                    <option value="">&nbsp;</option>`;
                    if (field.type == 'boolean') {
                        editor += `<option value="true">${'是'.t()}</option>
                                    <option value="false">${'否'.t()}</option>`;
                    } else {
                        for (const key in field.options) {
                            editor += `<option value="${key}">${field.options[key]}</option>`;
                        }
                    }
                    editor += '</select>';
                    return editor;
                } else if (field.type === 'datetime' || field.type === 'date') {
                    return `<div class="input-group dateRanges" style="flex:1">
                    <input type="text" class="form-control float-right column-filter-input"/>
                    <div class="input-group-append">
                        <span class="input-group-text">
                            <span class="far fa-calendar-alt"></span>
                        </span>
                    </div>
                </div>`;
                }
                return `<input class='column-filter-input search-input' type='text'/>`;
            };
            if (panel.length == 0) {
                filter.append(`<div class='column-filter-panel dropdown-menu1'>
                    <span>${'筛选'.t()}</span>
                    <div class="input-group" style="display:flex;">
                        ${getEditor(field)}
                        <div class="input-group-append" >
                            <button type="button" class="btn btn-primary">
                                <i class="fa fa-check"></i>
                            </button>
                        </div>
                    </div>
                </div>`);
                if (field.type === 'datetime' || field.type === 'date') {
                    $('.daterange-panel').remove();
                    $('body').append('<div class="daterange-panel" style="position: relative;top:0;left:0;z-index: 30000"></div>');
                    const optionsType = {
                        datetime: {
                            timePicker: true,
                            timePickerSeconds: true,
                            timePicker24Hour: true,
                            startDate: moment().startOf('day'),
                            endDate: moment().endOf('day'),
                            locale: {format: 'YYYY-MM-DD HH:mm:ss', customRangeLabel: '自定义范围'},
                            ranges: {
                                今天: [moment().startOf('day'), moment().endOf('day')],
                                昨天: [moment().startOf('day').subtract(1, 'days'), moment().endOf('day').subtract(1, 'days')],
                                '7天前': [moment().startOf('day').subtract(6, 'days'), moment().endOf('day')],
                                '30天前': [moment().startOf('day').subtract(29, 'days'), moment().endOf('day')],
                                这个月: [moment().startOf('month'), moment().endOf('month')],
                                上个月: [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                            },
                            parentEl: $('.daterange-panel'),
                        },
                        date: {
                            opens: 'left',
                            startDate: moment().startOf('day'),
                            endDate: moment().endOf('day'),
                            locale: {format: 'YYYY-MM-DD', customRangeLabel: '自定义范围'},
                            ranges: {
                                今天: [moment(), moment()],
                                昨天: [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
                                '7天前': [moment().subtract(6, 'days'), moment()],
                                '30天前': [moment().subtract(29, 'days'), moment()],
                                这个月: [moment().startOf('month'), moment().endOf('month')],
                                上个月: [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
                            },
                            parentEl: $('.daterange-panel'),
                        },
                    };
                    filter.find('.column-filter-input')
                        .attr('id', 'filter_date_range' + column)
                        .daterangepicker(optionsType[field.type]);
                }
                filter.find('input').on('keypress', function (event) {
                    if (event.keyCode == '13') {
                        event.preventDefault();
                        event.stopPropagation();
                        submitSearch();
                    }
                }).focus();
                filter.find('button').on('click', function () {
                    submitSearch();
                });
            } else {
                panel.show();
            }
            const left = btn.offset().left - me.dom.offset().left < 320;
            filter.find('.column-filter-panel')
                .css('left', left ? '0' : 'auto')
                .css('right', left ? 'auto' : '0')
                .on('mousemove', function () {
                    return false;
                });
            return false;
        });
    },
    // 处理表头排序
    updateColumnOrder(columns, state) {
        let me = this;
        if (!state.field_order) {
            return columns;
        }
        let map = {};
        columns.each(function () {
            let dom = $(this);
            let name = dom.attr('name');
            map[name] = dom;
        });
        let list = [];
        for (let field of state.field_order.split(',')) {
            let item = map[field];
            if (item) {
                list.push(item);
                delete map[field];
            }
        }
        for (let key of Object.keys(map)) {
            list.push(map[key]);
        }
        return list;
    },
    // 保存视图顺序
    saveViewState(values, callback) {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.state',
            method: 'saveViewState',
            args: {
                ids: [],
                viewId: me.vid,
                values,
            },
            onsuccess(r) {
                callback && callback();
            },
            onerror(err) {
                console.log(err);
            },
        });
    },
    // 加载视图顺序
    loadViewState(viewId) {
        let result;
        jmaa.rpc({
            model: 'ir.ui.state',
            method: 'loadViewState',
            args: {
                ids: [],
                viewId,
            },
            async: false,
            onsuccess(r) {
                result = r.data;
            },
            onerror(err) {
                console.log(err);
            },
        });
        return result;
    },
    // 初始化行高亮参数
    initHilite(grid) {
        let me = this;
        me.hilite = [];
        grid.children('hilite').each(function () {
            let dom = $(this);
            let expr = dom.attr('t-if');
            let opt = {
                fn: new Function('$data', `with($data){return ${expr};}`),
                color: dom.attr('color'),
                class: dom.attr('class')
            };
            if (dom.attr('style')) {
                let style = {};
                const s = dom.attr('style').toLowerCase().replace(/-(.)/g, function (m, g) {
                    return g.toUpperCase();
                }).replace(/;\s?$/g, "").split(/:|;/g)

                for (var i = 0; i < s.length; i += 2) {
                    style[s[i].replace(/\s/g, "")] = s[i + 1].replace(/^s+|\s+$/g, "");
                }
                opt.style = style;
            }
            me.hilite.push(opt);
        });
    },
    /**
     * 初始化DataTable
     * @param columnDefs 列定义，参考https://datatables.net/reference/option/columnDefs
     */
    initTable(columnDefs) {
        const me = this;
        let sortedColumns;
        me.table = me.dom.find('table').DataTable({
            paging: false,
            lengthChange: false,
            searching: false,
            ordering: me.ordering,
            info: false,
            autoWidth: false,
            responsive: true,
            processing: true,
            serverSide: true,
            rowId: 'id',
            colReorder: true,
            language: {
                processing: '加载中'.t(),
                zeroRecords: '没有数据'.t(),
            },
            createdRow(row, data, dataIndex) {
                for (let opt of me.hilite) {
                    if (opt.fn(data)) {
                        if (opt.color) {
                            $(row).addClass('text-' + opt.color);
                        }
                        if (opt.class) {
                            $(row).addClass(opt.class);
                        }
                        if (opt.style) {
                            $(row).css(opt.style);
                        }
                    }
                }
            },
            ajax(data, callback, settings) {
                if (me.redraw && me.data) {
                    callback({data: me.data});
                    me.onLoaded();
                } else if (me.table) {
                    me.ajax(
                        me,
                        function (d) {
                            if (me.isTree) {
                                d = me.buildTreeData(d);
                            }
                            me.data = d.data;
                            callback(d);
                            me.onLoaded();
                        },
                        data,
                        settings,
                    );
                } else {
                    callback({data: []});
                }
            },
            order: [],
            columnDefs,
        });
        me.table.on('change', '.check-select', function () {
            const ckb = $(this);
            const id = me.table.row(ckb.parents('tr')).id();
            if (ckb.is(':checked')) {
                if (me.multiSelect) {
                    me.selected.push(id);
                } else {
                    me.dom.find('.check-select').prop('checked', false);
                    ckb.prop('checked', true);
                    me.selected = [id];
                }
            } else {
                me.selected.remove(id);
            }
            // 自动触发勾选全选按钮
            let targetLength = 0;
            if (Array.isArray(me.data)) {
                targetLength = me.data.length;
            } else {
                const ids = [];
                for (const dataKey in me.data) {
                    ids.push(dataKey);
                }
                targetLength = ids.length;
            }
            if (me.multiSelect && me.selected.length === targetLength) {
                me.dom.find('.all-check-select').prop('checked', true);
            } else {
                me.dom.find('.all-check-select').prop('checked', false);
            }
            me.dom.triggerHandler('selected', [me, me.selected]);
        }).on('change', '.all-check-select', function () {
            const ckb = $(this);
            if (ckb.is(':checked')) {
                if (Array.isArray(me.data)) {
                    me.selected = me.data.map((item) => item.id);
                } else {
                    for (const dataKey in me.data) {
                        me.selected.push(dataKey);
                    }
                }
                me.dom.find('.check-select').prop('checked', true);
            } else {
                me.selected = [];
                me.dom.find('.check-select').prop('checked', false);
            }
            me.dom.triggerHandler('selected', [me, me.selected]);
        }).on('click', 'tbody tr', function (event) {
            let row = $(this);
            let id = me.table.row(this).id();
            if (!id || row.hasClass('edit') || row.children('.dataTables_empty').length === 1) {
                return;
            }
            if (me.checkSelect) {
                if ($(event.target).closest('.select-column').length > 0) {
                    return;
                }
                if (me.multiSelect && event.ctrlKey) {
                    row.find('.check-select').prop('checked', true);
                    me.selected.push(id);
                } else {
                    me.dom.find('.check-select').prop('checked', false);
                    row.find('.check-select').prop('checked', true);
                    me.selected = [id];
                }
            } else if (me.multiSelect && event.ctrlKey) {
                if (row.hasClass('selected')) {
                    row.removeClass('selected');
                    me.selected.remove(id);
                } else {
                    row.addClass('selected');
                    me.selected.push(id);
                }
            } else {
                me.table.$('tr.selected').removeClass('selected');
                row.addClass('selected');
                me.selected = [id];
            }
            me.dom.triggerHandler('selected', [me, me.selected]);
        }).on('dblclick', 'tbody tr', function () {
            const row = $(this);
            if (row.hasClass('edit') || row.children('.dataTables_empty').length === 1) {
                return;
            }
            const id = me.table.row(this).id();
            me.dom.triggerHandler('rowDblClick', [me, id]);
        }).on('column-visibility.dt', function (e, settings, column, state) {
            me.initColumnFilter();
            me.initColumnConfig();
        }).on('click', '[data-role=tooltip]', function () {
            me._showTooltip($(this));
        }).on('column-reorder', function (e, s, details) {
            const columns = sortedColumns || columnDefs.map(item => item.data);
            let swapItems = function (array, index1, index2) {
                const temp = array[index1];
                array[index1] = array[index2];
                array[index2] = temp;
                return array;
            };
            sortedColumns = swapItems(columns, details.from, details.to);
            me.saveViewState({field_order: sortedColumns.join()});
        });
        if (me.footer) {
            me.table.on('draw', function () {
                me.table.columns('.sum').every(function () {
                    let data = this.data();
                    let sum = data.length == 0 ? 0 : data
                        .reduce(function (a, b) {
                            return a + b;
                        });
                    if (!isNaN(sum) && sum % 1 !== 0) {
                        sum = sum.toFixed(2);
                    }
                    $(this.footer()).html(sum);
                });
                me.table.columns('.total-text').every(function () {
                    $(this.footer()).html('合计');
                });
            });
        }
    },
    _showTooltip(item) {
        let help = item.attr('data-tooltip');
        let offset = item.offset();
        let tooltip = $(`<div class="tooltip fade show" style="position:absolute;top:0;left:0;will-change:transform;">
                <div class="arrow" style="left:10px"></div>
                <div class="tooltip-inner">${help}</div>
            </div>`);
        $('body').append(tooltip);
        let height = tooltip.height();
        let left;
        if (offset.left + tooltip.width() > $(window).width()) {
            left = offset.left - tooltip.width() + 25;
            tooltip.find('.arrow').css('left', `${tooltip.width() - 22}px`);
        } else {
            left = offset.left - 7;
            tooltip.find('.arrow').css('left', '10px');
        }
        if (offset.top - height - 10 < 0) {
            tooltip.addClass('bs-tooltip-bottom').css('transform', `translate3d(${left}px, ${offset.top + 15}px, 0px)`);
        } else {
            tooltip.addClass('bs-tooltip-top').css('transform', `translate3d(${left}px, ${offset.top - height - 10}px, 0px)`);
        }
    },
    buildTreeData(data) {
        const me = this;
        const map = {};
        const result = [];
        const addChildren = function (d) {
            for (const c of d.$children) {
                c.$depth = c.$parent.$depth + 1;
                result.push(c);
                addChildren(c);
            }
        };
        for (const d of data.data) {
            map[d.id] = d;
            d.$children = [];
        }
        for (const d of data.data) {
            let pid = d[me.parentField];
            if (pid) {
                if ($.isArray(pid)) {
                    pid = pid[0];
                }
                const parent = map[pid];
                if (parent) {
                    parent.$children.push(d);
                    d.$parent = parent;
                }
            }
        }
        for (const d of data.data) {
            if (!d.$parent) {
                d.$depth = 0;
                result.push(d);
                addChildren(d);
            }
        }

        return {data: result};
    },
    /**
     * 重新渲染表格
     * @private
     */
    _redraw() {
        const me = this;
        me.redraw = true;
        me.table.draw();
        me.redraw = false;
        me.dom.find('tr.edit').removeClass('edit');
        me.editing = false;
    },
    initEditForm(dom, opt) {
        let me = this;
        let form = dom.JForm(
            $.extend(
                {
                    arch: me.editArch,
                    fields: me.fields,
                    model: me.model,
                    module: me.module,
                    owner: me,
                    view: me.view,
                },
                opt,
            ),
        );
        form.loadActionHandler(me.dom.attr('on-edit-load-action'));
        let onEditLoad = me.dom.attr('on-edit-load');
        if (onEditLoad) {
            let fn = me.getFunction(onEditLoad);
            form.onEvent('load', function (e, form) {
                fn(e, me);
            })
        }
        let onValid = me.dom.attr('on-valid');
        if (onValid) {
            let fn = me.getFunction(onValid);
            form.onEvent('valid', function (e, form) {
                return fn(e, me);
            })
        }
        form.onEvent('load', function (e, form) {
            if (me.owner?.readonly instanceof Function && me.owner.readonly()) {
                form.setReadonly(true);
            }
        })
        return form;
    },
    /**
     * 渲染编辑对话框
     * @param id
     * @param opt 选项
     * @param callback 对话框关闭后回调
     */
    renderDialogEdit(id, opt, callback) {
        const me = this;
        me.editing = true;
        let save = async function (dialog, db) {
            dialog.busy();
            if (me.editForm.valid()) {
                await me.save();
                dialog.close();
                db && db();
            } else {
                const errors = me.editForm.getErrors();
                jmaa.msg.error(errors);
            }
        }
        me.editDialog = jmaa.showDialog({
            id: 'edit-' + jmaa.nextId(),
            title: id ? '编辑'.t() : '添加'.t(),
            init(dialog) {
                me.editForm = me.initEditForm(dialog.body, opt);
                if (!id) {
                    dialog.dom.find('.buttons-right').append(`<button type="button" t-click="saveDialog" class="btn btn-flat btn-info">${'确定并关闭'}</button>`);
                }
                me.dom.triggerHandler('editFormInit', [me, me.editForm, dialog]);
            },
            async saveDialog(e, dialog) {
                await save(dialog);
            },
            async submit(dialog) {
                await save(dialog, callback);
            },
        });
    },
    /**
     * 渲染行内编辑
     * @param row 行
     * @param id id
     * @param opt 选项
     */
    renderRowEdit(row, id, opt) {
        const me = this;
        me.editing = true;
        row.addClass('edit').html(me.getEditFBarTpl());
        row.find('[name=cancel]').on('click', function () {
            me._redraw();
        });
        row.find('[name=confirm]').on('click', async function () {
            if (me.editForm.valid()) {
                const btn = $(this);
                btn.attr('disabled', true);
                await me.save();
                btn.attr('disabled', false);
            } else {
                const errors = me.editForm.getErrors();
                jmaa.msg.error(errors);
            }
        });
        me.editForm = me.initEditForm(row.find('.grid-edit'), opt);
        me.dom.triggerHandler('editFormInit', [me, me.editForm, row]);
    },
    /**
     * 添加行数据
     * @param values 数据
     * @param callback 回调函数
     */
    async create(values) {
        const me = this;
        let result = await me.dom.triggerHandler('create', [me, values]);
        if (result === true) {
            return;
        } else if (typeof result !== 'undefined') {
            me.createData(result);
        } else {
            await me.createData(values);
        }
    },
    async createData(values) {
        const me = this;
        if (!me.dialog && me.editing) {
            if (me.editForm.valid()) {
                await me.save();
            } else {
                return;
            }
        }
        me._redraw();
        if (me.dialog) {
            let v = $.extend(true, {}, values);
            me.renderDialogEdit(null, {}, function () {
                me.create(v);
            });
        } else {
            me.dom.find('table tbody').prepend('<tr id="addNew"></tr>');
            const row = me.dom.find('#addNew');
            me.renderRowEdit(row, null, {});
        }
        me.editForm.create(values);
    },
    /**
     * 提交编辑数据
     */
    async save() {
        const me = this;
        await me.dom.triggerHandler('save', [me, me.editForm.getSubmitData(), $.extend({}, me.editForm.getData())]);
        me.editing = false;
        me.load();
        me.dom.triggerHandler('editValueChange', [me, me.editForm]);
    },
    /**
     * 删除，触发delete事件
     */
    delete() {
        let me = this;
        me.dom.triggerHandler('delete', [me, me.selected]);
    },
    /**
     * 编辑行数据
     * @param id
     */
    async edit(id) {
        const me = this;
        id = id || me.selected[0];
        if (id) {
            if (!me.dialog && me.editing && me.editForm.dirty) {
                if (me.editForm.valid()) {
                    //await me.save();
                    await me.dom.triggerHandler('save', [me, me.editForm.getSubmitData(), $.extend({}, me.editForm.getData())]);
                } else {
                    return;
                }
            }
            me._redraw();
            if (me.dialog) {
                me.renderDialogEdit(id, {
                    ajax(form, callback) {
                        me.loadEdit(me, id, callback);
                    },
                });
            } else {
                const row = me.dom.find('#' + id);
                me.renderRowEdit(row, id, {
                    ajax(form, callback) {
                        me.loadEdit(me, id, callback);
                    },
                });
            }
            me.editForm.load();
        }
        return me.editForm;
    },
    /**
     * 获取DataTable对象
     * @returns {*}
     */
    getTable() {
        return this.table;
    },
    /**
     * 获取表格的字段
     * @returns {[]}
     */
    getFields() {
        return this._fields;
    },
    getUsePresent() {
        return this._presentFields;
    },
    /**
     * 获取选中行 [ids]
     * @returns {[]|[*]|[*]|*}
     */
    getSelected() {
        return this.selected;
    },
    /**
     * 获取选中行值 [{}]
     * @returns {[]|[*]|[*]|*}
     */
    getSelectedData() {
        const me = this;
        const data = [];
        for (const s of me.selected) {
            for (const d of me.data) {
                if (d.id == s) {
                    data.push($.extend({}, d));
                }
            }
        }
        return data;
    },
    select(id) {
        let me = this;
        me.table.$('tr.selected').removeClass('selected');
        me.dom.find('.all-check-select').prop('checked', false);
        me.dom.find('.check-select').prop('checked', false);
        if (typeof id === 'undefined') {
            me.selected = [];
        } else if (Array.isArray(id)) {
            for (let r of id) {
                if (me.checkSelect) {
                    me.table.$('#' + r).find('.check-select').prop('checked', true);
                } else {
                    me.table.$('#' + r).addClass('selected');
                }
            }
            me.selected = [...id];
        } else {
            if (me.checkSelect) {
                me.table.$('#' + id).find('.check-select').prop('checked', true);
            } else {
                me.table.$('#' + id).addClass('selected');
            }
            me.selected = [id];
        }
        me.dom.triggerHandler('selected', [me, me.selected]);
    },
    /**
     * 获取表格排序条件
     * @returns {string}
     */
    getSort() {
        const me = this;
        let order = '';
        if (me.table) {
            $.each(me.table.order(), function (i, o) {
                const ds = me.table.column(o[0]).dataSrc();
                if (ds) {
                    if (order != '') {
                        order += ',';
                    }
                    order += ds + ' ' + o[1];
                }
            });
        }
        return order;
    },
    onLoaded() {
        let me = this;
        me.dom.triggerHandler('load', [me]);
        me.dom.find('.long-text').each(function () {
            if ($(this).width() > 450) {
                $(this).css('min-width', '450px').css('white-space', 'normal');
            }
        });
        me.dom.find('.all-check-select').prop('checked', false);
        for (let id of me.selected) {
            if (me.checkSelect) {
                me.table.$('#' + id).find('.check-select').prop('checked', true);
            } else {
                me.table.$('#' + id).addClass('selected');
            }
        }
        me.dom.triggerHandler('selected', [me, me.selected]);
    },
    /**
     * 加载数据，触发ajax方法
     */
    load() {
        this.editing = false;
        this.table.ajax.reload();
    },
    destroy() {
        this.table.destroy();
        $(this.dom).removeData('JGrid');
    }
});
$(function () {
    $(document).on('mousedown', function (e) {
        if ($(e.target).closest('.column-filter-panel,.daterangepicker').length == 0) {
            $('.daterange-panel').remove();
            $('.column-filter-panel').remove();
        }
        if ($(e.target).closest('.jui-grid-customize').length == 0) {
            $('.jui-grid-customize').remove();
        }
    });
});
$.fn.JGrid = function (opt) {
    let com = $(this).data(name);
    if (!com) {
        com = new JGrid($.extend({dom: this}, opt));
        $(this).data(name, com);
    }
    return com;
};
