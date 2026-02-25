//@ sourceURL=EditLookup.js
jmaa.define("EditLookup", {
    extends: 'EditView',
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
                <div class="m-body e-form containment">
                    <div class="e-many2one">
                        <div class="editor">
                            <div class="input-group">
                                <input type="text" class="form-control select-input">
                                <div class="input-suffix">
                                    <button type="button" data-btn="link" class="btn">
                                        <i class="fa fa-external-link-alt"></i>
                                    </button>
                                    <span class="icon-down">
                                        <i class="fa fa-angle-down"></i>
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div class="dropdown-select">
                            <div class="lookup-data dropdown-content"></div>
                            <div class="lookup-footer">
                                <button type="button" data-btn="clear" class="btn btn-sm btn-default">清空</button>
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
                    </div>
                </div>
            </div>`
    },
    __init__(opt) {
        let me = this;
        jmaa.utils.apply(true, me, opt);
        me.dom.addClass('edit-lookup').html(me.getTpl()).on('change', '.sidebar_content input', function () {
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
                        return me.dom.find(`.holder-item`).addClass('drop-target').attr('position', 'inside');
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
        me.renderLookup();
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
    renderLookup() {
        let me = this;
        me._fields = [];
        let arch = jmaa.utils.parseXML(me.primaryView.arch);
        me.combineViews(arch, me.extensionViews);
        let lookup = arch.find('lookup');
        let columns = lookup.children('field');
        let header = [];
        let body = [];
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
            if (!label) {
                label = field.label || field.name;
            }
            label = label.t();
            me._fields.push(name);
            let title = col.attr('title') || '';
            if (title) {
                title = title.replaceAll('<', '&lt;').replaceAll('>', '&gt;');
                title = `<sup class="fa fa-question-circle btn-help p-1" data-role="tooltip" data-tooltip="${title}"></sup>`;
            }
            if (style) {
                style = ` style="${style}"`;
            }
            let xpath = me.getXPath(col);
            header.push(`<th xpath="${xpath}" class="column-item sorting_disabled" data-field='${name}' data-label='${label}'${style}>
                <div class="title">
                    <span>${label}</span>${title}
                </div>
                <div class="component-tools">
                    <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                    <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                </div>
            </th>`);
            body.push(`<td xpath="${xpath}" data-field='${name}'>&nbsp;</td>`);
        });
        if (!columns.length) {
            header.push(`<th xpath="lookup" class="holder-item">
                <div class="title">
                    <span>&nbsp;</span>
                </div>
            </th>`);
            body.push(`<td xpath="lookup" class="holder-item">&nbsp;</td>`);
        }
        me.dom.find('.lookup-data').html(`<table class="table table-bordered table-hover dataTable">
            <thead><tr>${header.join('')}</tr></thead>
            <tbody>
                <tr class="odd">${body.join('')}</tr>
                <tr class="even">${body.join('')}</tr>
                <tr class="odd">${body.join('')}</tr>
                <tr class="even">${body.join('')}</tr>
                <tr class="odd">${body.join('')}</tr></tbody>
            </table>`);
        me.initDroppable(me.dom.find('.table [data-field],.table .holder-item'));
        me.moveDraggable(me.dom.find('.table th[data-field]'));
    },
});
