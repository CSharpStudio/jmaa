//@ sourceURL=EditSearch.js
jmaa.define("EditSearch", {
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
                <div class="m-body containment">
                    <div class="jui-search">
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <button data-btn="submitSearch" type="button" class="btn btn-search">
                                    <i class="fa fa-search"></i>
                                </button>
                            </div>
                            <div class="jui-search-container">
                                <div class="input-group">
                                    <span class="jui-search-selection">
                                        <ul class="jui-search-selection-body"></ul>
                                    </span>
                                    <div class="input-group-append">
                                        <button type="button" class="btn btn-search dropdown-toggle" data-btn="dropdownSearch"></button>
                                    </div>
                                </div>
                                <div class="jui-search-fixed e-form"></div>
                            </div>
                        </div>
                    </div>
                    <div class="container-fluid search-dropdown" style="min-width:300px">
                        <div class="search-form e-form"></div>
                        <div class="card-footer">
                            <button data-btn="clearSearch" class="btn btn-flat btn-outline-secondary" style="margin-right:5px">${'清空'.t()}</button>
                            <button data-btn="resetSearch" class="btn btn-flat btn-outline-secondary" style="margin-right:5px">${'重置'.t()}</button>
                            <button data-btn="confirmSearch" class="btn btn-flat btn-primary float-right" style="min-width:100px">${'确定'.t()}</button>
                        </div>
                    </div>
                </div>
            </div>`
    },
    __init__(opt) {
        let me = this;
        jmaa.utils.apply(true, me, opt);
        me.dom.addClass('edit-search').html(me.getTpl()).on('change', '.sidebar_content input', function () {
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
                if (target.hasClass('holder-item')) {
                    let addFixed = target.hasClass('add-fixed');
                    return target.html(`<div position="inside" xpath="${target.attr('xpath')}" class='drop-target${addFixed ? ' add-fixed' : ''}'></div>`);
                }
                target.siblings(".drop-target").remove();
                let targetWidth = target.width();
                let mouseX = event.pageX - target.offset().left;
                if (mouseX < targetWidth / 2) {
                    $(`<div position="before" xpath="${target.attr('xpath')}" class='drop-target'></div>`).insertBefore(target);
                } else {
                    $(`<div position="after" xpath="${target.attr('xpath')}" class='drop-target'></div>`).insertAfter(target);
                }
            },
            out: function (event, ui) {
                let target = $(this);
                ui.helper.removeClass("dragging-over");
                me.dom.find(".drop-target").remove();
                if (target.hasClass('holder-item')) {
                    target.text(target.attr('text'));
                }
            }
        });
    },
    editItem(el) {
        let me = this;
        let xpath = el.attr('xpath');
        let field = el.find('[data-field]');
        let label = field.attr('data-label');
        let defaultValue = field.attr('data-default');
        jmaa.showDialog({
            title: '编辑'.t(),
            css: 'modal-sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                        <editor name="label" type="char" label="标题"></editor>
                        <editor name="default" type="char" label="默认值"></editor>
                    </form>`,
                });
                dialog.form.setData({id: 'id', label, default: defaultValue});
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
    onViewLoaded() {
        let me = this;
        me.renderSearch();
        me.filterFields();
    },
    renderAside(arch) {

    },
    renderFixed(arch) {
        let me = this, fixed = arch.children("fixed");
        if (fixed.length > 0) {
            let html = [];
            fixed.children("field").each(function (i, e) {
                let el = $(e);
                let name = el.attr('name');
                let field = me.fields[name] || {};
                let label = el.attr('label');
                let val = el.attr('default') || '';
                let op = el.attr('op');
                let criteria = el.attr('criteria');
                let attrs = "";
                let editor = el.attr('editor');
                let xpath = me.getXPath(el);
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
                me._fields.push(name);
                html.push(`<div class="form-item" xpath="${xpath}" position="fixed">
                        <label>${label}</label>
                        <div ${attrs} data-label="${label}" ${val ? ' data-default="' + val + '"' : ''}
                            ${op ? ' data-op="' + op + '"' : ''}
                            ${editor ? ' data-editor="' + editor + '"' : ''}
                            ${criteria ? ' data-criteria="' + jmaa.utils.encode(criteria) + '"' : ''}
                            data-field="${name}">
                        </div>
                        <div class="component-tools">
                            <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                            <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                            <div class="btn btn-move"><i class="fa fa-arrows-alt"></i></div>
                        </div>
                    </div>`);
            });
            me.dom.find('.jui-search-fixed').html(html);
        }
        if (!me.dom.find('.jui-search-fixed [xpath]').length) {
            let holder = $(`<div class="holder-item${fixed.length == 0 ? ' add-fixed' : ''}" xpath="search>fixed" text="固定条件区域">固定条件区域</div>`);
            me.dom.find('.jui-search-fixed').prepend(holder);
            me.initDroppable(holder);
        }
    },
    renderDropdown(arch) {
        let me = this;
        let fields = arch.children('field');
        let col = arch.col || (fields.length <= 3 ? 1 : fields.length <= 6 ? 2 : 3);
        me.dom.find('.search-dropdown').css('max-width', col == 3 ? '100%' : col == 2 ? '66.666667%' : '33.333333%');
        me.dom.find('.search-form').css('grid-template-columns', `repeat(${col},1fr)`);
        let html = [];
        fields.each(function (i, e) {
            let el = $(e);
            let name = el.attr('name');
            let field = me.fields[name] || {};
            let label = el.attr('label');
            let val = el.attr('default') || el.attr('defaultValue');
            let op = el.attr('op');
            let criteria = el.attr('criteria');
            let lookup = el.attr('lookup');
            let attrs = '';
            let editor = el.attr('editor');
            let xpath = me.getXPath(el);
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
            me._fields.push(name);
            html.push(`<div class="form-group form-item" xpath="${xpath}" position="dropdown"><label>${label}</label>
                <div data-label="${label}"
                ${val ? ` data-default="${val}"` : ''}
                ${op ? ` data-op="${op}"` : ''}
                ${editor ? ` data-editor="${editor}"` : ''}
                ${criteria ? ` data-criteria="${jmaa.utils.encode(criteria)}"` : ''}
                ${lookup ? ` lookup="${jmaa.utils.encode(lookup)}"` : ''}
                ${attrs} data-field="${name}"></div>
                <div class="component-tools">
                    <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                    <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                    <div class="btn btn-move"><i class="fa fa-arrows-alt"></i></div>
                </div>
            </div>`);
        });
        me.dom.find('.search-dropdown .search-form').html(html.join(''));
        if (!me.dom.find('.search-dropdown .search-form [xpath]').length) {
            let holder = $('<div class="holder-item" xpath="search" text="查询条件区域">查询条件区域</div>');
            me.dom.find('.search-form').prepend(holder);
            me.initDroppable(holder);
        }
    },
    renderSearch() {
        let me = this;
        me.editors = {};
        me._fields = [];
        let arch = jmaa.utils.parseXML(me.primaryView.arch);
        me.combineViews(arch, me.extensionViews);
        let search = arch.find('search');
        me.renderAside(search);
        me.renderFixed(search);
        me.renderDropdown(search);
        me.criteria = eval(search.attr('criteria'));
        me.dom.find('[data-field]').each(function () {
            let el = $(this),
                name = el.attr('data-field'),
                field = me.fields[name] || {},
                editor = el.attr('data-editor') || field.type,
                ctl = jmaa.searchEditors[editor];
            if (!ctl) {
                ctl = jmaa.searchEditors['@none-field'];
            }
            me.editors[name] = new ctl({
                dom: el,
                name,
                field: field,
                model: me.model,
                module: me.module,
                owner: me,
                allowNull: true,
                design: true,
                op: el.attr('data-op'),
                criteria: el.attr('data-criteria'),
                label: el.attr('data-label')
            });
        });
        me.initCriteria();
        me.initDroppable(me.dom.find('.form-item'));
        me.moveDraggable(me.dom.find('.form-item'));
        me.dom.find(".component-tools .btn-edit,.component-tools .btn-remove").on("mousedown", function (event) {
            event.stopPropagation();
        });
    },
    initCriteria() {
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
        me.updateCriteria();
    },
    updateCriteria() {
        let me = this;
        for (let field of me._fields) {
            me.dom.find('.jui-search-selection-body [data-field=' + field + ']').remove();
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
                    me.removeCriteria(field + '-col');
                    me.addCriteria(field, editor.label, editor.getText(), criteria);
                }
            } else {
                let criteria = editor.getCriteria();
                if (criteria.length > 0) {
                    me.removeCriteria(field + '-col');
                    me.addCriteria(field, editor.label, editor.getText(), criteria);
                }
            }
        });
    },
    addCriteria(field, label, text, expr) {
        let me = this;
        let svg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 512 512"><path d="M256 48C141.31 48 48 141.31 48 256s93.31 208 208 208s208-93.31 208-208S370.69 48 256 48zm75.31 260.69a16 16 0 1 1-22.62 22.62L256 278.63l-52.69 52.68a16 16 0 0 1-22.62-22.62L233.37 256l-52.68-52.69a16 16 0 0 1 22.62-22.62L256 233.37l52.69-52.68a16 16 0 0 1 22.62 22.62L278.63 256z" fill="currentColor"></path></svg>`
        let html = `
            <li class="jui-search-choice"  data-field="${field}">
                <p class="filter-name">${label}</p>
                <p class="filter-value"><span>${text}</span><span class="jui-search-choice-remove" role="presentation">${svg}</span></p>
            </li>
        `;
        me.dom.find('.jui-search-selection-body').append(html);
    },
    removeCriteria(field) {
        let me = this;
        me.editors[field]?.setValue('');
        me.dom.find('.jui-search-selection-body [data-field=' + field + ']').remove();
    },
    addField(field, target) {
        let me = this;
        let xpath = target.attr('xpath');
        let position = target.attr('position');
        let fixed = target.hasClass('add-fixed');
        let arch = `<xpath expr="${xpath}" position="${position}"><field name="${field}"></field></xpath>`;
        if (fixed) {
            arch = `<xpath expr="search" position="inside"><fixed></fixed></xpath>` + arch;
        }
        if (me.studioView) {
            let xml = jmaa.utils.parseXML(me.studioView.arch);
            xml.append(arch);
            me.saveView(xml.html());
        } else {
            me.saveView(arch);
        }
    },
    moveField(xpath, target) {
        let me = this;
        let toXpath = target.attr('xpath');
        let position = target.attr('position');
        let fixed = target.hasClass('add-fixed');
        if (me.studioView) {
            let xml = jmaa.utils.parseXML(me.studioView.arch);
            let old = xml.find(`[expr="${xpath}"][position=move]`);
            if (old.length) {
                let arch = `<xpath expr="${toXpath}" position="${position}"></xpath>`;
                if (fixed) {
                    arch = `<xpath expr="search" position="inside"><fixed></fixed></xpath>` + arch;
                }
                old.html(arch);
            } else {
                let arch = `<xpath expr="${xpath}" position="move"><xpath expr="${toXpath}" position="${position}"></xpath></xpath>`;
                if (fixed) {
                    arch = `<xpath expr="search" position="inside"><fixed></fixed></xpath>` + arch;
                }
                xml.append(arch);
            }
            me.saveView(xml.html());
        } else {
            let arch = `<xpath expr="${xpath}" position="move"><xpath expr="${toXpath}" position="${position}"></xpath></xpath>`;
            if (fixed) {
                arch = `<xpath expr="search" position="inside"><fixed></fixed></xpath>` + arch;
            }
            me.saveView(arch);
        }
    },
});
jmaa.searchEditor('@none-field', {
    getTpl() {
        return `<div class="form-control"><span class="text-danger">**${'不支持查询'.t()}**</span></div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl());
    },
    getCriteria() {
        return [];
    },
});
