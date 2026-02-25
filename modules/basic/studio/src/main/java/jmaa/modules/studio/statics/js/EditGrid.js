//@ sourceURL=EditGrid.js
jmaa.define("EditGrid", {
    extends: 'EditView',
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
    filterColumn: true,
    ordering: true,
    /**
     * 是否可配置
     */
    customizable: false,
    getTpl() {
        return `<div class="d-header">
                <div class="d-toolbar">
                    <button type="button" disabled="disabled" class="btn btn-flat btn-default btn-undo">${'撤销'}</button>
                    <button type="button" disabled="disabled" class="btn btn-flat btn-default btn-redo">${'恢复'}</button>
                    <button type="button" class="btn btn-flat btn-default btn-source">${'XML'}</button>
                </div>
            </div>
            <div class="d-body">
                <aside class="left-aside sidebar_content">
                    <div class="a-components"></div>
                    <h6 class="s-collapse open">${'现有字段'.t()}<i class="s-field-icon fa fa-caret-down ml-2"></i></h6>
                    <div>
                        <h6 class="small">${'当前未使用的字段'.t()}</h6>
                        <input type="text" class="form-control mb-2"/>
                        <div class="s-fields"></div>
                    </div>
                </aside>
                <div class="m-body">
                    <div class="content-header p-2">
                        <div class="btn-row">
                            <div part="toolbar" class="toolbar"></div>
                        </div>
                    </div>
                    <div class="grid-content containment">
                        <div class="jui-grid">
                            <div class="row">
                                <div class="col-sm-12 grid-body"></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`
    },
    __init__(opt) {
        let me = this;
        jmaa.utils.apply(true, me, opt);
        me._fields = [];
        me._presentFields = [];
        me._customFields = [];
        me.view = {
            auths: '@all'
        }
        me.dom.addClass('edit-grid').html(me.getTpl()).on('change', '.sidebar_content input', function () {
            me.filterFields();
        }).on('keyup', '.sidebar_content input', function () {
            clearTimeout(me.keyupTimer);
            me.keyupTimer = setTimeout(function () {
                me.filterFields();
            }, 100);
        }).on('mouseenter', '.table [data-field]', function () {
            let col = $(this);
            let field = col.attr('data-field');
            me.dom.find(`[data-field=${field}]`).addClass('hover');
        }).on('mouseleave', '.table [data-field]', function () {
            let col = $(this);
            let field = col.attr('data-field');
            me.dom.find(`[data-field=${field}]`).removeClass('hover');
        }).on('click', '.component-tools .btn-edit', function (e) {
            let el = $(this).closest('[xpath]');
            me.editItem(el);
        }).on('click', '.component-tools .btn-remove,.btn-remove-tab', function (e) {
            let btn = $(this);
            jmaa.msg.confirm({
                title: '确认'.t(),
                content: '确认删除?'.t(),
                submit() {
                    let xpath = btn.closest('[xpath]').attr('xpath');
                    me.removeItem(xpath);
                }
            })
        }).on('click', '.btn-redo', function () {
            me.memo.redo();
        }).on('click', '.btn-undo', function () {
            me.memo.undo();
        });
        if (eval(jmaa.web.cookie('ctx_debug'))) {
            me.dom.find('.btn-source').show().on('click', function () {
                me.studio.editSource(me.type);
            });
        }
        me.initMemo(function () {
            me.dom.find('.btn-redo').attr('disabled', !me.memo.canRedo());
            me.dom.find('.btn-undo').attr('disabled', !me.memo.canUndo());
        });
        me.loadViewData();
    },
    initDroppable(dom) {
        let me = this;
        dom.droppable({
            tolerance: "pointer",
            over: function (event, ui) {
                let target = $(this);
                ui.helper.addClass("dragging-over");
                setTimeout(() => {
                    if (target.hasClass('holder-item')) {
                        return me.dom.find('.holder-item').addClass('drop-target').attr('position', 'inside');
                    }
                    let field = target.attr('data-field');
                    let targetWidth = target.width();
                    let mouseX = event.pageX - target.offset().left;
                    if (mouseX < targetWidth / 2) {
                        me.dom.find(`.table [data-field=${field}]`).attr('position', 'before').addClass('drop-target');
                    } else {
                        me.dom.find(`.table [data-field=${field}]`).attr('position', 'after').addClass('drop-target');
                    }
                }, 1);
            },
            out: function (event, ui) {
                ui.helper.removeClass("dragging-over");
                me.dom.find(".drop-target").removeClass('drop-target').removeAttr('position');
            }
        });
    },
    onMoveDragStop(e, ui) {
        let me = this;
        $(e.target).find('.title').css("display", "block");
        let target = me.dom.find(".drop-target");
        if (target.length) {
            let xpath = ui.helper.attr('xpath');
            me.moveField(xpath, target);
        }
    },
    onMoveDragStart(e) {
        $(e.target).find('.title').css("display", "none");
    },
    onViewLoaded() {
        let me = this;
        me.renderGrid();
        me.filterFields();
    },
    editItem(el) {
        let me = this;
        let xpath = el.attr('xpath');
        let label = el.attr('data-label');
        jmaa.showDialog({
            title: '编辑'.t(),
            css: 'modal-sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                        <editor name="label" type="char" label="标题"></editor>
                    </form>`,
                });
                dialog.form.setData({id: 'id', label});
                dialog.form.clean();
            },
            submit(dialog) {
                let dirty = dialog.form.getSubmitData();
                delete dirty.id;
                let getArch = function () {
                    let attrs = [];
                    for (let attr of Object.keys(dirty)) {
                        attrs.push(`${attr}="${dirty[attr]}"`);
                    }
                    return attrs.join(' ');
                }
                let arch = `<xpath expr="${xpath}" position="attribute"><field ${getArch()}></field></xpath>`;
                if (me.studioView) {
                    let xml = jmaa.utils.parseXML(me.studioView.arch);
                    xml.append(arch);
                    me.saveView(xml.html());
                } else {
                    me.saveView(arch);
                }
                dialog.close();
            },
        })
    },
    renderGrid() {
        let me = this;
        me._fields = [];
        let arch = jmaa.utils.parseXML(me.primaryView.arch);
        me.combineViews(arch, me.extensionViews);
        let grid = arch.find('grid');
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
        me.toolbar = new JToolbar({
            dom: me.dom.find('[part=toolbar]'),
            arch: tb.prop('outerHTML'),
            defaultButtons: 'query|create|edit|delete|export|import',
            target: me.grid,
            view: me.view,
            auths: "@all",
            design: true,
        });
        tb.remove();
        me.footer = me.nvl(eval(grid.attr('footer')), false);
        me.ordering = me.nvl(eval(grid.attr('ordering')), me.ordering);
        let opt = {
            header: [],
            body: [],
            footer: [],
        };
        if (me.checkSelect) {
            opt.header.push(`<th style="width:1%;padding-right:8px;">
                        <div class="title" style="max-width:18px">
                            ${me.multiSelect ? `<input title="${'全选页数据'.t()}" type="checkbox" class="all-check-select"/>` : ''}
                        </div>
                    </th>`);
            opt.body.push(`<td><input type="checkbox" class="check-select"></td>`);
            opt.footer.push('<td></td>');
        }
        if (me.showRowNum) {
            opt.header.push('<th style="width:1%;padding-right:10px;"><span class="title" style="max-width:10px">#</span></th>');
            opt.body.push(`<td></td>`);
            opt.footer.push('<td></td>');
        }
        me.initColumns(grid, opt);
        grid.replaceWith(`<table class="table table-bordered table-hover dataTable">
            <thead><tr>${opt.header.join('')}</tr></thead>
            <tbody>
                <tr class="odd">${opt.body.join('')}</tr>
                <tr class="even">${opt.body.join('')}</tr>
                <tr class="odd">${opt.body.join('')}</tr>
                <tr class="even">${opt.body.join('')}</tr>
                <tr class="odd">${opt.body.join('')}</tr></tbody>
            ${me.footer ? `<tfoot><tr>${opt.footer.join('')}</tr></tfoot>` : ''}
            </table>`);
        me.dom.find('.jui-grid .grid-body').html(arch.children().prop('outerHTML'));
        me.initDroppable(me.dom.find('.table [data-field],.table .holder-item'));
        me.moveDraggable(me.dom.find('.table th[data-field]'));
    },
    initColumns(grid, opt) {
        let me = this;
        let columns = grid.children('field,column');
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
                field = {label: `<span class="text-danger">**${'字段不存在'.t()}**</span>`};
            }
            let style = col.attr('style') || '';
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
            let title = col.attr('title') || '';
            if (title) {
                title = title.replaceAll('<', '&lt;').replaceAll('>', '&gt;');
                title = `<sup class="fa fa-question-circle btn-help p-1" data-role="tooltip" data-tooltip="${title}"></sup>`;
            }
            if (filter) {
                const span = $('<span class="column-filter"><i class="fa fa-filter column-filter-btn"></i></span>');
                const criteria = col.attr('criteria');
                if (criteria) {
                    span.attr('criteria', criteria);
                }
                filter = span[0].outerHTML;
            }
            if (style) {
                style = ` style="${style}"`;
            }
            let xpath = me.getXPath(col);
            let css = me.ordering && eval(col.attr('ordering') || field.sortable) ? 'sorting' : 'sorting_disabled';
            opt.header.push(`<th xpath="${xpath}" class="column-item ${css}" data-field='${name}' data-label='${label}'${style}>
                <div class="title">
                    <span>${label}</span>${title}${filter || ''}
                </div>
                <div class="component-tools">
                    <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                    <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                </div>
            </th>`);
            opt.body.push(`<td xpath="${xpath}" data-field='${name}'></td>`);
            opt.footer.push('<td></td>');
        });
        if (!columns.length) {
            opt.header.push(`<th xpath="grid" class="holder-item">
                <div class="title">
                    <span></span>
                </div>
            </th>`);
            opt.body.push(`<td xpath="grid" class="holder-item"></td>`);
            opt.footer.push('<td></td>');
        }
    }
});
