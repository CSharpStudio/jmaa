//@ sourceURL=report.design.js
jmaa.component('report.designer', {
    getTpl() {
        return `<div class="rpt-search jui-report"></div>
            <div class="btn-row">
                <div class="toolbar">
                    <button type="button" class="btn btn-secondary btn-flat btn-query">查询</button>
                    <button type="button" class="btn btn-default btn-flat btn-reset" title="重置查询条件">重置</button>
                    <button type="button" class="btn btn-default btn-flat btn-export">导出</button>
                </div>
            </div>
            <div class="rpt-ds-bind">
                <button type="button" class="btn btn-flat btn-icon">${'数据源'.t()}: <span class="rpt-ds-code"></span></button>
            </div>
            <div class="rpt-grid containment"></div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).on('click', '.rpt-ds-bind button', function () {
            me.bindDataSet();
        }).on('mouseenter', '.table [data-field]', function () {
            let col = $(this);
            let field = col.attr('data-field');
            me.dom.find(`[data-field=${field}]`).addClass('hover');
        }).on('mouseleave', '.table [data-field]', function () {
            let col = $(this);
            let field = col.attr('data-field');
            me.dom.find(`[data-field=${field}]`).removeClass('hover');
        }).on('click', '.component-tools .btn-edit', function (e) {
            let btn = $(this);
            if (btn.attr('type') == 'grid') {
                let field = btn.closest('[data-field]');
                me.editGridColumn(field.attr('data-field'));
            } else {
                let editor = $(this).closest('[form-editor]');
                me.editEditor(editor.attr('form-editor'));
            }
        }).on('click', '.component-tools .btn-remove,.btn-remove-tab', function (e) {
            let btn = $(this);
            me.removeItem(btn);
        });
        me.initDraggable(me.view.dom.find('.report-content .s-component'));
    },
    load() {
        let me = this;
        me.view.rpc(me.view.model, 'read', {
            ids: [me.view.custom.dataId],
            fields: ['name', 'content'],
        }).then(data => {
            me.arch = data[0].content;
            me.render();
            me.view.dom.find('.r-title .rpt-name').html(data[0].name);
        });
    },
    loadDataSet(ds) {
        let me = this;
        me.dataSetColumns = [];
        me.params = [];
        if (!ds) {
            return me.dom.find('.rpt-ds-code').html('').removeAttr('ds');
        }
        me.view.rpc(me.view.model, 'readDataSet', {
            ids: [me.view.custom.dataId],
            ds,
        }).then(data => {
            me.dataSetColumns = data.columns;
            me.params = data.params;
            me.dom.find('.rpt-ds-code').html(data.name).attr('ds', data.code);
            let columns = [];
            for (let row of me.dataSetColumns) {
                columns.push(row[0]);
            }
            me.dom.find('.rpt-grid th[data-field]').each(function () {
                let field = $(this);
                if (!columns.includes(field.attr('data-field'))) {
                    field.addClass('text-danger').find('.title').append(`<i>(${'失效'.t()})</i>`);
                }
            });
            me.dom.find('.rpt-search [form-editor]').each(function () {
                let editor = $(this);
                if (!me.params.includes(editor.attr('form-editor'))) {
                    editor.find('label').addClass('text-danger').append(`<i>(${'失效'.t()})</i>`);
                }
            });
        });
    },
    bindDataSet() {
        let me = this;
        jmaa.showDialog({
            title: '数据源'.t(),
            init(dialog) {
                let html = $(`<div class="d-flex">
                        <div class="bind-ds-list"></div>
                        <div class="bind-ds-columns">
                        </div>
                    </div>`);
                let ds = [];
                let data = me.view.dataSet.data || [];
                for (let row of data) {
                    ds.push(`<div ds-id="${row.id}" ds-code="${row.code}" class="ds-list-item">
                        <i class="fa fa-database mr-1 text-info"></i>
                        <span>${row.name}</span>
                    </div>`);
                }
                html.find('.bind-ds-list').html(ds.join(''));
                dialog.body.html(html).on('click', '.ds-list-item', function () {
                    html.find('.ds-list-item.selected').removeClass('selected');
                    let selected = $(this).addClass('selected').attr('ds-id');
                    me.view.rpc(me.view.model, 'readDataSetColumns', {
                        dataSetId: selected,
                    }).then(data => {
                        me.renderBindColumns(dialog.body.find('.bind-ds-columns'), data);
                    });
                }).on('change', '.all-check-select', function () {
                    const ckb = $(this);
                    if (ckb.is(':checked')) {
                        dialog.body.find('.check-select').prop('checked', true);
                    } else {
                        dialog.body.find('.check-select').prop('checked', false);
                    }
                }).on('change', '.check-select', function () {
                    dialog.body.find('.all-check-select').prop('checked', false);
                });
                let dsCode = me.dom.find('.rpt-ds-code').attr('ds');
                dialog.body.find(`[ds-code="${dsCode}"]`).click();
            },
            submit(dialog) {
                let selected = dialog.body.find('.check-select:checked');
                if (!selected.length) {
                    return jmaa.msg.error('请选择字段'.t());
                }
                let ds = dialog.body.find('.ds-list-item.selected').attr('ds-code');
                me.saveGridArch(ds, dialog.body.find('.bind-column'), function () {
                    dialog.close();
                    jmaa.msg.show('操作成功'.t());
                    me.load();
                });
            }
        })
    },
    removeItem(e) {
        let me = this;
        jmaa.msg.confirm({
            title: '确认'.t(),
            content: '确认删除?'.t(),
            submit() {
                let arch = jmaa.utils.parseXML(me.arch);
                if (e.attr('type') == 'grid') {
                    let field = e.closest('[data-field]');
                    arch.find(`grid column[name="${field.attr('data-field')}"]`).attr('visible', false);
                } else {
                    let editor = e.closest('[form-editor]').attr('form-editor');
                    arch.find(`search editor[name="${editor}"]`).remove();
                }
                let content = arch.html();
                me.saveReport(content, function () {
                    jmaa.msg.show('操作成功'.t());
                    me.load();
                });
            }
        });
    },
    addEditor(type, target) {
        let me = this;
        jmaa.showDialog({
            title: '属性'.t(),
            css: 'modal-md',
            init(dialog) {
                let paramOption = [];
                for (let p of me.params) {
                    paramOption.push(`'${p}':'${p}'`);
                }
                let lookup = [];
                for (let row of me.view.dataSet.data) {
                    lookup.push(`'${row.code}':'${row.name}'`);
                }
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="2">
                        <editor name="label" label="标题" type="char" required="1" colspan="2"></editor>
                        <editor name="name" label="参数" type="selection" required="1" colspan="2" options="{${paramOption.join(',')}}"></editor>
                        <editor name="options" label="选项" type="text" colspan="2" t-visible="type=='multi-selection'" required="1"></editor>
                        <editor name="dataset" label="数据集" type="selection" colspan="2" t-visible="type=='lookup'" options="{${lookup.join(',')}}" help="${'提供id,present字段的数据集'.t()}" required="1"></editor>
                        <editor name="colspan" label="跨列" type="integer"></editor>
                        <editor name="rowspan" label="跨行" type="integer"></editor>
                        <editor name="type" type="char" visible="0"></editor>
                    </form>`,
                });
                dialog.form.create({type, colspan: 1, rowspan: 1});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                let editor = $(`<editor name="${data.name}" label="${data.label}" type="${data.type}"></editor>`);
                if (data.type == 'multi-selection') {
                    editor.attr('options', data.options);
                }
                if (data.colspan > 1) {
                    editor.attr('colspan', data.colspan);
                } else {
                    editor.removeAttr('colspan');
                }
                if (data.rowspan > 1) {
                    editor.attr('rowspan', data.rowspan);
                } else {
                    editor.removeAttr('rowspan');
                }
                if (data.type == 'lookup') {
                    editor.attr('dataset', data.dataset);
                }
                let position = target.attr('position');
                let arch = jmaa.utils.parseXML(me.arch);
                if (position == 'inside') {
                    let search = arch.find('search');
                    if (!search.length) {
                        search = $('<search></search>');
                        arch.append(search);
                    }
                    search.append(editor);
                } else {
                    let xpath = target.attr('xpath');
                    let to = arch.find(`search editor[name="${xpath}"]`);
                    if (position == 'before') {
                        editor.insertBefore(to);
                    } else if (position == 'after') {
                        editor.insertAfter(to);
                    }
                }
                let content = arch.html();
                me.saveReport(content, function () {
                    dialog.close();
                    jmaa.msg.show('操作成功'.t());
                    me.load();
                });
            },
            cancel() {
                target.remove();
            }
        });
    },
    editEditor(name) {
        let me = this;
        let arch = jmaa.utils.parseXML(me.arch);
        let editor = arch.find(`search editor[name="${name}"]`);
        jmaa.showDialog({
            title: '编辑列'.t(),
            css: 'modal-md',
            init(dialog) {
                let paramOption = [];
                for (let p of me.params) {
                    paramOption.push(`'${p}':'${p}'`);
                }
                let lookup = [];
                for (let row of me.view.dataSet.data) {
                    lookup.push(`'${row.code}':'${row.name}'`);
                }
                let types = `{'char':'${'文本'.t()}','boolean':'${'复选框'.t()}','date':'${'日期'.t()}','date-range':'${'日期范围'.t()}','datetime-range':'${'日期时间'.t()}','float':'${'小数'.t()}','integer':'${'整数'.t()}','lookup':'${'关联查找'.t()}','multi-selection':'${'下拉框'.t()}'}`;
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="2">
                        <editor name="label" label="标题" type="char" required="1" colspan="2"></editor>
                        <editor name="name" label="参数" type="selection" required="1" colspan="2" options="{${paramOption.join(',')}}"></editor>
                        <editor name="type" label="类型" type="selection" required="1" colspan="2" options="${types}"></editor>
                        <editor name="options" label="选项" type="text" t-visible="type=='multi-selection'" required="1" colspan="2"></editor>
                        <editor name="dataset" label="数据集" type="selection" colspan="2" t-visible="type=='lookup'" options="{${lookup.join(',')}}" help="${'提供id,present字段的数据集'.t()}" required="1"></editor>
                        <editor name="colspan" label="跨列" type="integer"></editor>
                        <editor name="rowspan" label="跨行" type="integer"></editor>
                    </form>`,
                });
                let label = editor.attr('label');
                let type = editor.attr('type') || 'char';
                let options = editor.attr('options');
                let colspan = editor.attr('colspan') || 1;
                let rowspan = editor.attr('rowspan') || 1;
                let dataset = editor.attr('dataset');
                dialog.form.create({name, colspan, rowspan, type, label, options, dataset});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                editor.attr('name', data.name).attr('label', data.label).attr('type', data.type);
                if (data.type == 'multi-selection') {
                    editor.attr('options', data.options);
                }
                if (data.colspan > 1) {
                    editor.attr('colspan', data.colspan);
                } else {
                    editor.removeAttr('colspan');
                }
                if (data.rowspan > 1) {
                    editor.attr('rowspan', data.rowspan);
                } else {
                    editor.removeAttr('rowspan');
                }
                if (data.type == 'lookup') {
                    editor.attr('dataset', data.dataset);
                }
                let content = arch.html();
                me.saveReport(content, function () {
                    dialog.close();
                    me.load();
                });
            }
        });
    },
    editGridColumn(name) {
        let me = this;
        let arch = jmaa.utils.parseXML(me.arch);
        let column = arch.find(`grid column[name="${name}"]`);
        jmaa.showDialog({
            title: '编辑列'.t(),
            css: 'modal-md',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                        <editor name="name" label="字段" type="char" readonly="1"></editor>
                        <editor name="label" label="标题" type="char" required="1"></editor>
                        <editor name="type" label="类型" type="selection" required="1" options="{'char':'文本','date':'日期','datetime':'日期时间','float':'数字','selection':'选项'}"></editor>
                        <editor name="options" label="选项" type="text" t-visible="type=='selection'" required="1"></editor>
                    </form>`,
                });
                let label = column.attr('label');
                let type = column.attr('type') || 'char';
                let options = column.attr('options');
                dialog.form.create({name, type, label, options});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                column.attr('label', data.label).attr('type', data.type);
                if (data.type == 'selection') {
                    column.attr('options', data.options);
                }
                let content = arch.html();
                me.saveReport(content, function () {
                    dialog.close();
                    me.load();
                });
            }
        });
    },
    saveGridArch(ds, columns, callback) {
        let me = this;
        let arch = jmaa.utils.parseXML(me.arch);
        let grid = arch.find('grid');
        if (!grid.length) {
            grid = $(`<grid></grid>`);
            arch.append(grid);
        }
        let change = ds != grid.attr('dataset');
        if (change) {
            grid.attr('dataset', ds);
            grid.find('column').remove();
        }
        let fields = {};
        columns.each(function () {
            let row = $(this);
            let type = row.attr('type');
            let name = row.attr('field');
            let visible = row.find('.check-select').is(':checked');
            let label = row.find('.column-label').val();
            fields[name] = {name, type, visible, label};
        });
        grid.find('column').each(function () {
            let col = $(this);
            if (!(col.attr('name') in fields)) {
                $(this).remove();
            }
        });
        let updateColumn = function (column, type) {
            if (type == 'java.lang.Boolean') {
                column.attr('type', 'boolean');
            } else if (type == 'java.sql.Date') {
                column.attr('type', 'date');
            } else if (type == 'java.sql.Timestamp') {
                column.attr('type', 'datetime');
            } else if (type == 'java.lang.Integer' || type == 'java.lang.Double') {
                column.attr('type', 'float');
            }
        }
        for (let name of Object.keys(fields)) {
            let field = fields[name];
            if (change) {
                let column = $(`<column name="${field.name}" label="${field.label}"${!field.visible ? ' visible="0"' : ''}></column>`);
                updateColumn(column, field.type);
                grid.append(column);
            } else {
                let col = grid.find(`column[name="${field.name}"]`);
                if (col.length) {
                    col.attr('label', field.label);
                    if (field.visible) {
                        col.removeAttr('visible');
                    } else {
                        col.attr('visible', field.visible);
                    }
                } else {
                    let column = $(`<column name="${field.name}" label="${field.label}"${!field.visible ? ' visible="0"' : ''}></column>`);
                    updateColumn(column, field.type);
                    grid.append(column);
                }
            }
        }
        let content = arch.html();
        me.saveReport(content, callback);
    },
    renderBindColumns(dom, columns) {
        let me = this;
        let body = [];
        let getTitle = function (str) {
            let words = str.split('_').filter(word => word);
            return words.map(word => {
                return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
            }).join(' ');
        }
        for (let row of columns) {
            let field = row[0];
            let column = me.columns[field] || {};
            let label = column.label || getTitle(field);
            body.push(`<tr class="bind-column" field="${field}" type="${row[2]}">
                <td><input type="checkbox" class="check-select"${column.visible ? ' checked="checked"' : ''}></td>
                <td>${field}</td>
                <td>${row[1]}</td>
                <td><input type="text" class="column-label" value="${label}"></input></td>
            </tr>`)
        }
        dom.html(`<table class="table table-bordered dataTable">
                <thead>
                    <tr>
                        <th style="width:1%;padding-right:8px;">
                            <div class="title" style="max-width:18px">
                                <input title="全选页数据" type="checkbox" class="all-check-select">
                            </div>
                        </th>
                        <th>${'字段'.t()}</th>
                        <th>${'类型'.t()}</th>
                        <th>${'标题'.t()}</th>
                    </tr>
                </thead>
                <tbody>${body.join('')}</tbody>
            </table>`);
    },
    renderSearch(arch) {
        let me = this;
        let search = arch.find('search');
        let editors = search.children('editor');
        let html = [];
        if (editors.length) {
            editors.each(function () {
                let el = $(this);
                let name = el.attr('name');
                let label = el.attr('label') || name;
                let val = el.attr('default') || el.attr('defaultValue');
                let attrs = [];
                let editor = el.attr('type');
                let colspan = Math.min(el.attr('colspan') || 1, 4);
                let rowspan = el.attr('rowspan') || 1;
                let css = ['form-group form-item'];
                let style = [];
                if (colspan < 5) {
                    css.push(`grid-colspan-${colspan}`);
                } else {
                    style.push(`grid-column:span ${colspan};`);
                }
                if (rowspan < 5) {
                    css.push(`grid-rowspan-${rowspan}`);
                } else {
                    style.push(`grid-row:span ${rowspan};`);
                }
                $.each(this.attributes, function (i, attr) {
                    let v = jmaa.utils.encode(attr.value);
                    if (['name', 'type', 'label'].indexOf(attr.name) == -1) {
                        if (attr.name === 'class') {
                            css.push(attr.value);
                        } else if (attr.name === 'style') {
                            style.push(attr.value);
                        } else {
                            attrs.push(`${attr.name}="${v}"`);
                        }
                    }
                });
                attrs.push(`${style.length ? `style="${style.join('')}" ` : ''}class="${css.join(' ')}"`);
                label = label.t();
                name = name.replaceAll('\.', '__');
                html.push(`<div form-editor="${name}" ${attrs.join(' ')}><label>${label}</label>
                <div data-label="${label}"
                ${val ? ` data-default="${val}"` : ''}
                ${editor ? ` data-editor="${editor}"` : ''}>
                    <input type="text" class="form-control" placeholder="#{${name}}"></input>
                </div>
                <div class="component-tools">
                    <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                    <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                    <div class="btn btn-move"><i class="fa fa-arrows-alt"></i></div>
                </div>
            </div>`);
            });
            me.dom.find('.rpt-search').html(`<div class="search jui-form">
                <div class="form-body">
                    <div class="form-card">
                        <div class="d-grid e-form grid-template-columns-4">${html.join('')}</div>
                    </div>
                </div>
            </div>`);
            me.initDroppable(me.dom.find('.form-item'));
            me.moveDraggable(me.dom.find('.form-item'));
        } else {
            let holder = $('<div class="holder-item" text="查询条件区域">查询条件区域</div>');
            me.dom.find('.rpt-search').html(holder);
            me.initDroppable(holder);
        }
    },
    renderGrid(arch) {
        let me = this;
        let grid = arch.find('grid');
        let ds = grid.attr('dataset');
        let columns = grid.children('column');
        let head = [`<th style="width:1%;padding-right:8px;">
                        <div class="title" style="max-width:18px">
                            <input title="${'全选页数据'.t()}" type="checkbox" class="all-check-select"/>
                        </div>
                    </th>
                    <th style="width:1%;padding-right:10px;"><span class="title" style="max-width:10px">#</span></th>`];
        let body = [`<td><input type="checkbox" class="check-select"></td><td></td>`];
        me.columns = {};
        if (columns.length) {
            columns.each(function () {
                let col = $(this);
                let name = col.attr('name');
                let label = col.attr('label');
                let type = col.attr('type');
                let format = col.attr('format');
                let options = col.attr('options');
                let visible = eval(col.attr('visible') || 1);
                if (visible) {
                    head.push(`<th label="${label}" data-field="${name}">
                        <div class="title">
                            <span>${label || name}</span>
                        </div>
                        <div class="component-tools">
                            <div class="btn btn-edit" type="grid"><i class="fa fa-edit"></i></div>
                            <div class="btn btn-remove" type="grid"><i class="fa fa-times"></i></div>
                        </div>
                    </th>`);
                    body.push(`<td data-field="${name}">#${name}</td>`);
                }
                me.columns[name] = {label, visible, type, format, options};
            });
        } else {
            head.push('<th></th>');
            body.push('<td></td>');
        }
        body = body.join('');
        me.dom.find('.rpt-grid').html(`<table class="table table-bordered dataTable">
                <thead><tr>${head.join('')}</tr></thead>
                <tbody>
                    <tr class="odd">${body}</tr>
                    <tr class="even">${body}</tr>
                    <tr class="odd">${body}</tr>
                    <tr class="even">${body}</tr>
                    <tr class="odd">${body}</tr>
                </tbody>
            </table>`);
        me.initDroppable(me.dom.find('.table [data-field]'));
        me.moveDraggable(me.dom.find('.rpt-grid .table th[data-field]'));
        me.loadDataSet(ds);
    },
    render() {
        let me = this;
        let arch = jmaa.utils.parseXML(me.arch);
        me.renderSearch(arch);
        me.renderGrid(arch);
    },
    onDragging(e) {
        let me = this;
        let body = me.dom.find('.containment');
        let scrollOffset = body.offset();
        let scrollWidth = body.width();
        let scrollHeight = body.height();
        let mouseX = e.pageX;
        let mouseY = e.pageY;
        if (mouseX > scrollOffset.left - 30 && mouseX < scrollOffset.left + scrollWidth + 30) {
            if (mouseY < scrollOffset.top + 30) {
                body.scrollTop(body.scrollTop() - 5);
            }
            if (mouseY > scrollOffset.top + scrollHeight - 30) {
                body.scrollTop(body.scrollTop() + 5);
            }
        }
        if (mouseY > scrollOffset.top - 30 && mouseY < scrollOffset.top + scrollHeight + 30) {
            if (mouseX < scrollOffset.left + 50) {
                body.scrollLeft(body.scrollLeft() - 5);
            }
            if (mouseX > scrollOffset.left + scrollWidth - 50) {
                body.scrollLeft(body.scrollLeft() + 5);
            }
        }
    },
    moveDraggable(dom) {
        let me = this;
        dom.draggable({
            scroll: true,
            zIndex: 999,
            cursor: "move",
            containment: 'window',
            cursorAt: {left: 0, top: 0},
            helper: function (e) {
                let helper = $(e.currentTarget).clone(true).css({
                    zIndex: "99999",
                    position: 'absolute',
                }).addClass('move-dragging');
                $("body").append(helper);
                return helper;
            },
            start: function (e) {
                let target = $(this);
                if (target.attr('data-field')) {
                    target.find('.title').css("display", "none");
                } else {
                    target.css("display", "none");
                }
            },
            drag: function (e) {
                me.onDragging(e);
            },
            stop: function (e, ui) {
                let target = $(this);
                let dropTarget = me.dom.find(".drop-target");
                let field = target.attr('data-field');
                if (dropTarget.length) {
                    let arch = jmaa.utils.parseXML(me.arch);
                    if (field) {
                        let column = arch.find(`grid column[name="${field}"]`);
                        let toPath = dropTarget.attr('data-field');
                        let position = dropTarget.attr('position');
                        let toColumn = arch.find(`grid column[name="${toPath}"]`);
                        if (position == 'before') {
                            column.insertBefore(toColumn);
                        } else if (position == 'after') {
                            column.insertAfter(toColumn);
                        }
                    } else {
                        let editor = arch.find(`search editor[name="${target.attr('form-editor')}"]`);
                        let toPath = dropTarget.attr('xpath');
                        let position = dropTarget.attr('position');
                        let toEditor = arch.find(`search editor[name="${toPath}"]`);
                        if (position == 'before') {
                            editor.insertBefore(toEditor);
                        } else if (position == 'after') {
                            editor.insertAfter(toEditor);
                        }
                    }
                    let content = arch.html();
                    me.saveReport(content, function () {
                        me.load();
                    });
                }
                if (field) {
                    target.find('.title').css("display", "block");
                } else {
                    target.css("display", "flex");
                }
            }
        });
    },
    initDraggable(dom) {
        let me = this;
        dom.draggable({
            scroll: true,
            zIndex: 999,
            cursor: "none",
            cursorAt: {left: 5, top: 10},
            containment: 'window',
            helper: function (e) {
                return $(e.currentTarget).clone(true).css({
                    zIndex: "99999",
                    position: 'absolute',
                }).addClass('dragging');
            },
            drag: function (e) {
                me.onDragging(e);
            },
            stop: function (e, ui) {
                let target = me.dom.find(".drop-target");
                if (target.length) {
                    let type = ui.helper.attr('type');
                    me.addEditor(type, target);
                }
            }
        });
    },
    initDroppable(dom) {
        let me = this;
        dom.droppable({
            tolerance: "pointer",
            over: function (event, ui) {
                let target = $(this);
                let field = target.attr('data-field');
                let dragField = ui.helper.attr('data-field');
                if (!field != !dragField) {
                    return;
                }
                setTimeout(() => {
                    if (field) {
                        ui.helper.addClass("dragging-over");
                        let targetWidth = target.width();
                        let mouseX = event.pageX - target.offset().left;
                        if (mouseX < targetWidth / 2) {
                            me.dom.find(`.table [data-field=${field}]`).attr('position', 'before').addClass('drop-target');
                        } else {
                            me.dom.find(`.table [data-field=${field}]`).attr('position', 'after').addClass('drop-target');
                        }
                    } else {
                        ui.helper.addClass("dragging-over");
                        if (target.hasClass('holder-item')) {
                            return target.html(`<div position="inside" class='drop-target'></div>`);
                        }
                        target.siblings(".drop-target").remove();
                        let targetWidth = target.width();
                        let mouseX = event.pageX - target.offset().left;
                        if (mouseX < targetWidth / 2) {
                            $(`<div position="before" xpath="${target.attr('form-editor')}" class='drop-target'></div>`).insertBefore(target);
                        } else {
                            $(`<div position="after" xpath="${target.attr('form-editor')}" class='drop-target'></div>`).insertAfter(target);
                        }
                    }
                }, 1);
            },
            out: function (event, ui) {
                let target = $(this);
                let field = target.attr('data-field');
                ui.helper.removeClass("dragging-over");
                if (field) {
                    me.dom.find(".drop-target").removeClass('drop-target').removeAttr('position');
                } else {
                    me.dom.find(".drop-target").remove();
                    if (target.hasClass('holder-item')) {
                        target.text(target.attr('text'));
                    }
                }
            }
        });
    },
    saveReport(content, callback) {
        let me = this;
        let arch = me.arch;
        me.view.memo.add({
            undo() {
                me.view.saveReport({content: arch}, callback);
            },
            redo() {
                me.view.saveReport({content}, callback);
            }
        });
        me.view.saveReport({content}, callback);
    }
});
