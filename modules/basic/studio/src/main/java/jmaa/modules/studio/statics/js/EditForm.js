//@ sourceURL=EditForm.js
jmaa.define("EditForm", {
    extends: 'EditView',
    cols: 4,
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
                    <div class="a-components">
                        <h6>${'组件'.t()}</h6>
                        <div class="s-components">
                            <div type="tabs" class="s-field-tabs s-component">${'页签'.t()}</div>
                            <div type="group" class="s-field-group s-component">${'分组'.t()}</div>
                        </div>
                        <h6>${'新字段'.t()}</h6>
                        <div class="s-new-fields">
                            <div type="char" class="s-field-char s-component">${'文本'.t()}</div>
                            <div type="text" class="s-field-text s-component">${'多行文本'.t()}</div>
                            <div type="integer" class="s-field-integer s-component">${'整数'.t()}</div>
                            <div type="float" class="s-field-float s-component">${'小数'.t()}</div>
                            <div type="date" class="s-field-date s-component">${'日期'.t()}</div>
                            <div type="datetime" class="s-field-datetime s-component">${'日期时间'.t()}</div>
                            <div type="boolean" class="s-field-boolean s-component">${'复选框'.t()}</div>
                            <div type="selection" class="s-field-selection s-component">${'下拉框'.t()}</div>
                            <div type="binary" class="s-field-binary s-component">${'文件'.t()}</div>
                            <div type="image" class="s-field-image s-component">${'图片'.t()}</div>
                            <div type="one2many" class="s-field-one2many s-component">${'一对多'.t()}</div>
                            <div type="many2one" class="s-field-many2one s-component">${'多对一'.t()}</div>
                            <div type="many2many" class="s-field-many2many s-component">${'多对多'.t()}</div>
                            <div type="many2many-tags" class="s-field-tags s-component">${'标签'.t()}</div>
                        </div>
                    </div>
                    <h6 class="s-collapse open">${'现有字段'.t()}<i class="s-field-icon fa fa-caret-down ml-2"></i></h6>
                    <div>
                        <h6 class="small">${'当前未使用的字段'.t()}</h6>
                        <input type="text" class="form-control mb-2"/>
                        <div class="s-fields"></div>
                    </div>
                </aside>
                <div class="m-body jui-form">
                    <div class="form-header">
                        <div part="form-toolbar" class="toolbar"></div>
                        <div class="bar-right"></div>
                    </div>
                    <div class="form-content"></div>
                </div>
            </div>`
    },
    getAsideTpl() {
        return `<aside class="left-aside border-right"><div class="m-1">
                    <div class="input-group input-group-sm">
                        <div class="input-group-prepend">
                            <button class="btn btn-default btn-sm tree-expand" title="${'展开所有节点'.t()}"><i class="fas fa-chevron-down"></i></button>
                            <button class="btn btn-default btn-sm tree-collapse" title="${'折叠所有节点'.t()}"><i class="fas fa-chevron-up"></i></button>
                        </div>
                        <input type="text" class="form-control tree-keyword"/>
                        <div class="input-group-append">
                            <button data-btn="view" class="btn btn-default tree-lookup">
                                <i class="fa fa-search"></i>
                            </button>
                        </div>
                    </div>
                </div><div id="treeview_${jmaa.nextId()}" class="treeview"></div></aside>`;
    },
    __init__(opt) {
        let me = this;
        jmaa.utils.apply(true, me, opt);
        me.view = {
            auths: '@all'
        }
        me.dom.addClass('edit-form').html(me.getTpl()).on('change', '.sidebar_content input', function () {
            me.filterFields();
        }).on('keyup', '.sidebar_content input', function () {
            clearTimeout(me.keyupTimer);
            me.keyupTimer = setTimeout(function () {
                me.filterFields();
            }, 100);
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
        }).on('click', '.tabs-add-tab', function () {
            let btn = $(this).clone();
            me.addContainer("tab", btn);
            return false;
        }).on('click', '.tabs-panel a[xpath].nav-link', function () {
            let link = $(this);
            let xpath = link.attr('xpath');
            me.activeTab = xpath;
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
        me.initDraggable(me.dom.find('.a-components .s-component'));
    },
    initDroppable(dom) {
        let me = this;
        dom.droppable({
            tolerance: "pointer",
            over: function (event, ui) {
                let target = $(this);
                let typeHelper = ui.helper.attr('type');
                if (typeHelper == 'tabs' && target.parents('.tabs-item,.group-item').length > 0) {
                    return;
                }
                if (typeHelper == 'group' && target.parents('.group-item').length > 0) {
                    return;
                }
                ui.helper.addClass("dragging-over");
                let colspan = ['tabs', 'group'].includes(typeHelper) ? ' grid-colspan-4' : '';
                if (target.hasClass('holder-item')) {
                    return target.html(`<div position="inside" xpath="${target.attr('xpath')}" class='drop-target${colspan}'></div>`);
                }
                target.siblings(".drop-target").remove();
                let targetWidth = target.width();
                let mouseX = event.pageX - target.offset().left;
                if (mouseX < targetWidth / 2) {
                    $(`<div position="before" xpath="${target.attr('xpath')}" class='drop-target${colspan}'></div>`).insertBefore(target);
                } else {
                    $(`<div position="after" xpath="${target.attr('xpath')}" class='drop-target${colspan}'></div>`).insertAfter(target);
                }
            },
            out: function (event, ui) {
                let target = $(this);
                ui.helper.removeClass("dragging-over");
                me.dom.find(".drop-target").remove();
                if (target.hasClass('holder-item')) {
                    target.text('表单设计区域');
                }
            }
        });
    },
    initContainerDroppable(dom, type) {
        let me = this;
        dom.droppable({
            tolerance: "pointer",
            over: function (event, ui) {
                let target = $(this);
                let typeHelper = ui.helper.attr('type');
                // group可以放进tab，不能放进group;tab不能放进tab和group
                if (type == 'group' && typeHelper == 'tabs' && target.closest('.tabs-item').length > 0) {
                    return;
                }
                if (type == 'group') {
                    ui.helper.addClass("dragging-over");
                    if (typeHelper == 'group' || typeHelper == 'tabs') {
                        let parent = target.closest('.group-item');
                        $(`<div position="after" xpath="${parent.attr('xpath')}" class='drop-target grid-colspan-4'></div>`).insertAfter(parent);
                    } else {
                        let parent = target.parent();
                        parent.find('.group-body .group-content').append(`<div position="inside" xpath="${parent.attr('xpath')}" class='drop-target'></div>`);
                    }
                } else if (type == 'tabs') {
                    if (!target.hasClass('active')) {
                        return;
                    }
                    ui.helper.addClass("dragging-over");
                    let colspan = ['tabs', 'group'].includes(typeHelper) ? ' grid-colspan-4' : '';
                    if (typeHelper == 'tabs') {
                        let parent = target.closest('.tabs-item');
                        $(`<div position="after" xpath="${parent.attr('xpath')}" class='drop-target${colspan}'></div>`).insertAfter(parent);
                    } else {
                        let link = target.attr('href');
                        me.dom.find(link + ' .tab-body').append(`<div position="inside" xpath="${target.attr('xpath')}" class='drop-target${colspan}'></div>`);
                    }
                }
            },
            out: function (event, ui) {
                ui.helper.removeClass("dragging-over");
                me.dom.find(".drop-target").remove();
            }
        });
    },
    onDragStop(e, ui) {
        let me = this;
        let target = me.dom.find(".drop-target");
        if (target.length) {
            let field = ui.helper.attr('field');
            if (field) {
                me.addField(field, target);
            } else {
                let type = ui.helper.attr('type');
                if (type == 'tabs' || type == 'group') {
                    me.addContainer(type, target);
                } else {
                    me.addNewField(type, target);
                }
            }
        }
    },
    onViewLoaded() {
        let me = this;
        me.renderForm();
        me.filterFields();
    },
    addNewField(type, target) {
        let me = this;
        jmaa.showDialog({
            title: '属性'.t(),
            css: 'modal-sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor name="name" type="char" label="名称" help="字段名称，小写字母+下划线" required="1" t-visible="!['one2many','many2many'].includes(type)"></editor>
                            <editor name="label" type="char" label="标题" required="1" t-visible="!['one2many','many2many'].includes(type)"></editor>
                            <editor name="model" type="model-lookup" label="关系" required="1" t-visible="['many2one','one2many','many2many'].includes(type)"></editor>
                            <editor name="options" label="选项" type="text" t-visible="type=='selection'" required="1" colspan="2"></editor>
                            <editor name="type" type="char" visible="0"></editor>
                        </form>`
                });
                dialog.form.create({type});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getRaw();
                let uiView = {};
                let position = target.attr('position');
                let xpath = target.attr('xpath');
                let arch = `<xpath expr="${xpath}" position="${position}"><field name="${data.name}"${type == 'selection' ? ` options="${data.options}"` : ''}></field></xpath>`;
                if (me.studioView) {
                    uiView.id = me.studioView.id;
                    let xml = jmaa.utils.parseXML(me.studioView.arch);
                    xml.append(arch);
                    uiView.arch = xml.html();
                } else {
                    uiView.model = me.studio.data.model;
                    uiView.name = me.studio.data.model + '-表单-stuido';
                    uiView.type = 'form';
                    uiView.mode = 'extension';
                    uiView.module_id = me.studio.data.module[0];
                    uiView.arch = arch;
                }
                jmaa.rpc({
                    model: me.studio.model,
                    method: 'addNewField',
                    args: {
                        model: me.studio.data.model,
                        field: {
                            field_type: type,
                            name: data.name,
                            label: data.label,
                            relation: data.model
                        },
                        uiView,
                    },
                    onsuccess(r) {
                        delete me.studio.data.modelFields;
                        me.loadViewData();
                    },
                    onerror(r) {
                        target.remove();
                        jmaa.msg.error(r);
                    }
                })
                dialog.close();
            },
            cancel() {
                target.remove();
            }
        })
    },
    addContainer(type, target) {
        let me = this;
        jmaa.showDialog({
            title: '属性'.t(),
            css: 'modal-sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor name="label" type="char" label="标题" required="1"></editor>
                            <editor name="columns" type="integer" label="跨列" required="1" t-visible="type!='tab'"></editor>
                            <editor name="type" type="char" visible="false"></editor>
                        </form>`
                });
                dialog.form.create({columns: 4, type});
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let xpath = target.attr('xpath');
                let position = target.attr('position');
                let getArch = function () {
                    let data = dialog.form.getData();
                    if (type == 'group') {
                        return `<group colspan="${data.columns}" label="${data.label}"></group>`;
                    } else if (type == 'tabs') {
                        return `<tabs colspan="${data.columns}"><tab label="${data.label}"></tab></tabs>`
                    } else if (type == 'tab') {
                        return `<tab label="${data.label}"></tab>`
                    }
                }
                let arch = `<xpath expr="${xpath}" position="${position}">${getArch()}</xpath>`;
                if (me.studioView) {
                    let xml = jmaa.utils.parseXML(me.studioView.arch);
                    xml.append(arch);
                    me.saveView(xml.html());
                } else {
                    me.saveView(arch);
                }
                dialog.close();
            },
            cancel() {
                target.remove();
            }
        });
    },
    editItem(el) {
        let me = this;
        let label = el.attr('label');
        let colspan = el.attr('colspan');
        let rowspan = el.attr('rowspan');
        let help = el.attr('help');
        let field = el.attr('form-editor');
        let xpath = el.attr('xpath');
        let options = el.find('.editor').attr('options');
        let f = me.fields[field] || {};
        jmaa.showDialog({
            title: '编辑'.t(),
            css: 'modal-md',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="2">
                        <editor name="label" type="char" label="标题" colspan="2"></editor>
                        <editor name="help" type="char" label="帮助" colspan="2" t-visible="type"></editor>
                        <editor name="options" label="选项" type="text" t-visible="type=='selection'" colspan="2"></editor>
                        <editor name="colspan" type="integer" label="跨列"></editor>
                        <editor name="rowspan" type="integer" label="跨行"></editor>
                        <editor name="type" type="char" visible="0"></editor>
                    </form>`,
                });
                dialog.form.setData({id: 'id', label, colspan, rowspan, help, type: f.type, options});
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
    renderForm() {
        let me = this;
        me._fields = [];
        me._editors = [];
        me.editors = {};
        let arch = jmaa.utils.parseXML(me.primaryView.arch);
        me.combineViews(arch, me.extensionViews);
        let form = arch.find('form');
        let toManyArchs = {};
        if (form.length > 0) {
            let dom = me.dom.find('.jui-form');
            let tbar = form.children('toolbar');
            me.tbarArch = tbar.prop('outerHTML');
            tbar.remove();
            let tree = form.children('tree');
            tree.remove();
            me.isTree = Boolean(tree.length);
            me.cols = me.nvl(eval(form.attr('cols')), me.cols);
            if (me.isTree) {
                me.cols -= 1;
            }
            if (me.cols < 1) {
                me.cols = 1;
            }
            me._processFields(form, toManyArchs);
            me._processEditors(form);
            me._processToolbar();
            me._processContainer(form);
            me._initLogAccess(form);
            form.addClass('d-grid e-form');
            form.attr('role', 'form-body');
            let html = arch.children().prop('outerHTML');
            if (dom.parents('form').length > 0) {
                html = html.replaceAll('<form', '<div').replaceAll('/form>', '/div>');
            }
            html = `<div class="form-body containment"><div class="form-card">${html}</div></div>`;
            if (me.isTree) {
                html = me.getAsideTpl() + html;
                dom.find('.form-content').css('display', 'flex');
            }
            let formBody = dom.find('.form-content').html(html).find('[role=form-body]');
            if (me.cols < 5) {
                formBody.addClass('grid-template-columns-' + me.cols);
            } else {
                formBody.css('grid-template-columns', 'repeat(' + me.cols + ', 1fr)');
            }
            if (me.isTree) {
                me._createTreeView(tree);
            }
            me._createEditors(toManyArchs);
            me._initEditorState();
        }
        if (!me.dom.find('.e-form [xpath]').length) {
            let holder = $('<div class="grid-colspan-4 holder-item" xpath="form">表单设计区域</div>');
            me.dom.find('.e-form').prepend(holder);
            me.initDroppable(holder);
        }
        me.initDroppable(me.dom.find('.e-form .form-item'));
        me.initContainerDroppable(me.dom.find('.e-form .group-item .group-header'), "group");
        me.initContainerDroppable(me.dom.find('.e-form .tabs-item [role=tab]'), "tabs");
        me.moveDraggable(me.dom.find('.e-form .form-item'));
        me.moveDraggable(me.dom.find('.e-form .group-item'));
        me.dom.find(".component-tools .btn-edit,.component-tools .btn-remove").on("mousedown", function (event) {
            event.stopPropagation();
        });
        if (me.activeTab) {
            me.dom.find(`[xpath="${me.activeTab}"]`).click();
        }
    },
    _processFields(form, toManyArchs) {
        let me = this;
        form.find('field').each(function () {
            let el = $(this);
            if (el.parents('field').length > 0) {
                return;
            }
            let name = el.attr('name');
            let field = me.fields[name];
            if (!field) {
                field = {type: '@none-field'};
            }
            field.name = field.name || name;
            me._fields.push(name);
            field.$defaultValue = el.attr('default') || el.attr('defaultValue') || field.defaultValue;
            if (field.type === 'many2many' || field.type === 'one2many') {
                toManyArchs[name] = el.html();
            }
            let html = me._getEditorHtml(el, field);
            el.replaceWith(html);
        });
    },
    _processEditors(form) {
        let me = this;
        form.find('editor').each(function () {
            let el = $(this);
            if (el.parents('field').length > 0) {
                return;
            }
            let name = el.attr('name');
            if (!name) {
                throw new Error('name属性不能为空' + el.html());
            }
            me._editors.push(name);
            let html = me._getEditorHtml(el, {name}, true);
            el.replaceWith(html);
        });
    },
    _processToolbar() {
        let me = this;
        if (!me.tbarArch) {
            return;
        }
        me.toolbar = new JToolbar({
            dom: me.dom.find('[part=form-toolbar]'),
            arch: me.tbarArch,
            auths: "@all",
            defaultButtons: me.isTree ? 'createChild|save|delete' : 'create|save|reload',
            target: me,
            view: me.view,
            design: true,
        });

        let html = [];
        jmaa.utils.parseXML(me.tbarArch).find('field').each(function () {
            let el = $(this);
            let name = el.attr('name');
            let field = me.fields[name];
            if (!field) {
                throw new Error('模型' + me.model + '找不到字段' + name);
            }
            field.name = field.name || name;
            if (!field.deny) {
                let attrs = [];
                let domAttrs = [];
                field.$defaultValue = el.attr('default') || el.attr('defaultValue') || field.defaultValue;
                $.each(this.attributes, function (i, attr) {
                    if (['name', 'label', 'colspan', 'rowspan'].indexOf(attr.name) > -1) {
                        return;
                    }
                    let v = jmaa.utils.encode(attr.value);
                    if (['t-readonly', 't-visible', 'readonly', 'style', 'class', 't-reset'].indexOf(attr.name) > -1) {
                        attrs.push(`${attr.name}="${v}"`);
                    } else {
                        domAttrs.push(`${attr.name}="${v}"`);
                    }
                });
                me._fields.push(name);
                if (el.attr('readonly') == undefined && field.readonly) {
                    attrs.push('readonly="1"');
                }
                let help = el.attr('help') || field.help;
                if (help) {
                    attrs.push(`title="${help}"`);
                }
                html.push(`<div form-editor="${name}" ${attrs.join(' ')}>
                            <div data-field="${name}">
                                <div class="editor" ${domAttrs.join(' ')}></div>
                            </div>
                        </div>`);
            }
            el.remove();
        });
        if (html.length) {
            me.dom.find('.form-header .bar-right').html(html.join(''));
        }
    },
    _processContainer(form) {
        let me = this;
        form.find('tabs,tab,group').each(function () {
            let container = $(this);
            container.attr('xpath', me.getXPath(container));
        });
        me._processTabs(form);
        me._processGroup(form);
    },
    _processGroup(form) {
        let me = this;
        form.find('group').each(function () {
            let group = $(this);
            let xpath = group.attr('xpath');
            let colspan = group.attr('colspan') || me.cols;
            let rowspan = group.attr('rowspan') || 1;
            let cols = group.attr('cols') || colspan;
            let attrs = [];
            let css = ['group-item field-group'];
            if (eval(group.attr('collapsed'))) {
                css.push('collapsed');
            }
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
                if (attr.name === 'class') {
                    css.push(attr.value);
                } else if (attr.name === 'style') {
                    style.push(attr.value);
                } else {
                    let v = jmaa.utils.encode(attr.value);
                    attrs.push(`${attr.name}="${v}"`);
                }
            });
            let label = (group.attr('label') || '').t();
            let position = group.attr('position');
            let collapsable = eval(group.attr('collapsable'));
            let expander = collapsable ? '<div class="group-expender"><span role="button" class="fa fa-chevron-up"></span></div>' : '';
            let header = `<div class="group-header${collapsable ? ' collapsable' : ''}">${label + expander}
                            <div class="component-tools">
                                <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                                <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                                <div class="btn btn-move"><i class="fa fa-arrows-alt"></i></div>
                            </div>
                        </div>`;
            let body = `<div class="group-body"><div class="group-content d-grid" style="grid-template-columns:repeat(${cols + ', 1fr'})">${group.prop('innerHTML')}</div></div>`;
            let html = `<div xpath="${xpath}" label="${label}" colspan="${colspan}" rowspan="${rowspan}" type="group" class="${css.join(' ')}"${style.length ? ` style="${style.join('')}"` : ''} ${attrs.join(' ')}>
                            ${position == 'bottom' ? body + header : header + body}
                        </div>`;
            group.replaceWith(html);
        });
    },
    _processTabs(form) {
        let me = this;
        form.find('tabs').each(function () {
            let tabs = $(this);
            let xpath = tabs.attr('xpath');
            let colspan = tabs.attr('colspan') || me.cols;
            let rowspan = tabs.attr('rowspan') || 1;
            let cols = tabs.attr('cols') || colspan;
            let nav = [];
            let content = [];
            let tabAttrs = [];
            let tabStyle = [];
            let tabCss = [];
            if (colspan < 5) {
                tabCss.push(`grid-colspan-${colspan}`);
            } else {
                tabStyle.push(`grid-column:span ${colspan};`);
            }
            if (rowspan < 5) {
                tabCss.push(`grid-rowspan-${rowspan}`);
            } else {
                tabStyle.push(`grid-row:span ${rowspan};`);
            }
            $.each(this.attributes, function (i, attr) {
                if (attr.name === 'class') {
                    tabCss.push(attr.value);
                } else if (attr.name === 'style') {
                    tabStyle.push(attr.value);
                } else {
                    let v = jmaa.utils.encode(attr.value);
                    tabAttrs.push(`${attr.name}="${v}"`);
                }
            });
            tabs.children('tab').each(function (i) {
                let tab = $(this);
                let xpathTab = tab.attr('xpath');
                let label = (tab.attr('label') || '').t();
                let id = 'tab-' + jmaa.nextId();
                let attrs = [];
                let linkCss = ['nav-link'];
                let paneCss = ['tab-pane fade'];
                if (!nav.length) {
                    linkCss.push('active');
                    paneCss.push('show active')
                }
                $.each(this.attributes, function (i, attr) {
                    if (attr.name === 'class') {
                        linkCss.push(attr.value);
                    } else {
                        let v = jmaa.utils.encode(attr.value);
                        if (attr.name != 'id') {
                            attrs.push(`${attr.name}="${v}"`);
                        }
                    }
                });
                nav.push(`<li class="nav-item tab-head" ${attrs.join(' ')}>
                                <a xpath="${xpathTab}" class="${linkCss.join(' ')}" id="${id}-tab" data-toggle="pill" href="#${id}" role="tab" aria-controls="${id}" aria-selected="true">
                                    ${label}<i class="fa fa-times btn-remove-tab"></i>
                                </a>
                            </li>`);
                content.push(`<div class="${paneCss.join(' ')}" id="${id}" role="tabpanel" aria-labelledby="${id}-tab">
                                    <div class="d-grid mt-2 tab-body" style="grid-template-columns:repeat(${cols + ', 1fr'})">${tab.prop('innerHTML')}</div>
                                </div>`);
            });
            nav.push(`<li class="nav-item tab-head">
                                <a xpath="${xpath}" position="inside" class="tabs-add-tab btn btn-icon" style="margin-top:2px" href="#"><i class="fa fa-plus-square"></i></a>
                            </li>`);
            let html = `<div class="tabs-panel tabs-item ${tabCss.join(' ')}"${tabStyle.length ? ` style="${tabStyle.join('')}"` : ''} ${tabAttrs.join(' ')}>
                                <ul class="nav nav-tabs" role="tablist">${nav.join('')}</ul>
                                <div class="tab-content">${content.join('')}</div>
                            </div>`;
            tabs.replaceWith(html);
        });
    },
    _initLogAccess(form) {
        let me = this;
        let logAccess = me.fields.create_uid && eval(form.attr('logAccess') || 1);
        if (logAccess) {
            form.append(`<div class="d-grid log-access" style="grid-template-columns: repeat(4, 1fr);grid-column:span ${me.cols}">
                        <div style="grid-column:span 1" class="form-group">
                            <label>${'创建人'.t()}</label>
                            <div data-field="create_uid">
                                <span class="editor" editor="span"></span>
                            </div>
                        </div>
                        <div style="grid-column:span 1" class="form-group">
                            <label>${'创建时间'.t()}</label>
                            <div data-field="create_date">
                                <span class="editor" editor="span"></span>
                            </div>
                        </div>
                        <div style="grid-column:span 1" class="form-group">
                            <label>${'修改人'.t()}</label>
                            <div data-field="update_uid">
                                <span class="editor" editor="span"></span>
                            </div>
                        </div>
                        <div style="grid-column:span 1" class="form-group">
                            <label>${'修改时间'.t()}</label>
                            <div data-field="update_date">
                                <span class="editor" editor="span"></span>
                            </div>
                        </div>
                    </div>`);
            me._fields.push('create_uid');
            me._fields.push('create_date');
            me._fields.push('update_uid');
            me._fields.push('update_date');
        }
    },
    _getEditorHtml(el, field, isEditor) {
        let me = this;
        let xpath = me.getXPath(el);
        let colspan = Math.min(el.attr('colspan') || 1, me.cols);
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
        let attrs = [];
        let domAttrs = [];
        $.each(el[0].attributes, function (i, attr) {
            if (['name', 'label', 'colspan', 'rowspan'].indexOf(attr.name) > -1) {
                return;
            }
            if (attr.name === 'class') {
                css.push(attr.value);
            } else if (attr.name === 'style') {
                style.push(attr.value);
            } else {
                let v = jmaa.utils.encode(attr.value);
                if (['t-readonly', 'readonly', 't-visible', 'visible', 't-reset'].indexOf(attr.name) > -1) {
                    attrs.push(`${attr.name}="${v}"`);
                } else {
                    domAttrs.push(`${attr.name}="${v}"`);
                }
            }
        });
        attrs.push(`${style.length ? `style="${style.join('')}" ` : ''}class="${css.join(' ')}"`);
        if (el.attr('readonly') == undefined && field.readonly) {
            attrs.push('readonly="1"');
        }
        let label = (el.attr('label') || field.label || field.name).t();
        let help = el.attr('help') || field.help || '';
        let getLabel = function () {
            let nolabel = el.attr('nolabel');
            if (!eval(nolabel)) {
                if (help) {
                    help = help.replaceAll('<', '&lt;').replaceAll('>', '&gt;');
                }
                let required = el[0].getAttribute('required');
                if (!required) {
                    required = field.required;
                }
                return `<label>${label}${help ? `<sup class="fa fa-question-circle btn-help p-1" data-role="tooltip" data-tooltip="${help}"></sup>` : ''}</label>
                                <span class="required text-danger">${required == 'required' || eval(required) ? '*' : ''}</span>`;
            }
            return '';
        }
        return `<div form-editor="${field.name}" ${attrs.join(' ')} xpath="${xpath}" label="${label}" help="${help}" colspan="${colspan}" rowspan="${rowspan}">
                    ${getLabel()}
                    <div data-${isEditor ? 'editor' : 'field'}="${field.name}">
                        <div class="editor" label="${label}" ${domAttrs.join(' ')}></div>
                    </div>
                    <div class="component-tools">
                        <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                        <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                        <div class="btn btn-move"><i class="fa fa-arrows-alt"></i></div>
                    </div>
                </div>`;
    },
    _createEditors(toManyArchs) {
        let me = this;
        let dom = me.dom;
        dom.find('[data-field],[data-editor]').each(function () {
            let item = $(this);
            let el = item.find('.editor');
            let fname = item.attr('data-field');
            let field = me.fields[fname] || {name: fname, label: fname, type: '@none-field'};
            let name = fname || item.attr('data-editor');
            let type = fname ? el.attr('editor') || field.type : el.attr('type');
            let editorClass = jmaa.editors[type];
            if (!editorClass) {
                editorClass = jmaa.editors['span'];
            }
            let opt = {
                field,
                name,
                model: me.model,
                module: me.module,
                owner: me,
                dom: el,
                view: me.view,
                design: true,
            };
            if (field.type === 'many2many' || field.type === 'one2many') {
                opt.arch = toManyArchs[name];
            }
            let editor = new editorClass(opt);
            me.editors[name] = editor;
        });
    },
    _createTreeView() {
        let me = this;
    },
    _initEditorState() {
        let me = this;
        me.dom.find('[visible]').each(function () {
            let e = $(this);
            let visible = eval(e.attr('visible'));
            if (visible) {
                e.show();
            } else {
                e.hide();
            }
        });
        me._updateReadonly();
    },
    _updateReadonly() {
        let me = this;
        me.dom.find('[readonly]').each(function () {
            let e = $(this);
            let name = e.attr('form-editor');
            let expr = this.attributes.readonly.value;
            if (name) {
                me.getEditor(name).readonly(eval(expr));
            }
        });
    },
    getEditor(name) {
        let e = this.editors[name];
        if (!e) {
            throw new Error('找不到name=[' + name + ']的editor');
        }
        return e;
    }
});
jmaa.editor('@none-field', {
    getTpl() {
        return `<div class="form-control"><span class="text-danger">**${'字段不存在'.t()}**</span></div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl());
    },
});
