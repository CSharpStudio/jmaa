//@ sourceURL=EditCard.js
jmaa.define("EditCard", {
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
                <div class="m-body">
                    <div class="content-header p-2">
                        <div class="btn-row">
                            <div part="toolbar" class="toolbar"></div>
                        </div>
                    </div>
                    <div class="card-content containment">
                        <div class="jui-card row mt-3 card-view">
                            <div class="card-item col-3"><div class="card card-panel"></div></div>
                            <div class="card-item col-3"><div class="card bg-light"></div></div>
                            <div class="card-item col-3"><div class="card bg-light"></div></div>
                            <div class="card-item col-3"><div class="card bg-light"></div></div>
                            <div class="card-item col-3"><div class="card bg-light"></div></div>
                            <div class="card-item col-3"><div class="card bg-light"></div></div>
                            <div class="card-item col-3"><div class="card bg-light"></div></div>
                        </div>
                    </div>
                </div>
            </div>`
    },
    __init__(opt) {
        let me = this;
        jmaa.utils.apply(true, me, opt);
        me.dom.addClass('edit-card').html(me.getTpl()).on('change', '.sidebar_content input', function () {
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
                    target.text('卡片设计区域');
                }
            }
        });
    },
    onViewLoaded() {
        let me = this;
        me.renderCard();
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
    renderCard() {
        let me = this;
        me._fields = [];
        let arch = jmaa.utils.parseXML(me.primaryView.arch);
        me.combineViews(arch, me.extensionViews);
        let card = arch.find('card');
        let tbar = card.children('toolbar');
        me.toolbar = new JToolbar({
            dom: me.dom.find('[part=toolbar]'),
            arch: tbar.prop('outerHTML'),
            defaultButtons: 'query|create|edit|delete|export|import',
            target: me.grid,
            view: me.view,
            auths: "@all",
            design: true,
        });
        tbar.remove();
        let template = card.find('template');
        if (template.length) {
            me.renderTemplate(card, template);
        } else {
            me.renderFields(card);
        }
    },
    renderTemplate(card, template) {
        let me = this;
        let fields = card.find('field');
        fields.each(function () {
            let el = $(this);
            let field = el.attr('name');
            me._fields.push(field);
        });
        let tpl = juicer(template.html().replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">"));
        jmaa.rpc({
            model: 'dev.studio',
            method: 'searchModelDemo',
            module: me.studio.module,
            args: {
                model: me.studio.data.model,
                limit: 1,
                fields: me._fields,
            },
            onsuccess(r) {
                let content = tpl.render(r.data[0] || {});
                me.dom.find('.card-panel').html(content);
            }
        })
    },
    renderFields(card) {
        let me = this;
        me.itemCols = Math.min(3, me.nvl(eval(card.attr('item-cols')), me.itemCols));
        card.find('field').each(function () {
            let item = $(this);
            let xpath = me.getXPath(item);
            let colspan = Math.min(item.attr('colspan') || 1, me.itemCols);
            let rowspan = item.attr('rowspan') || 1;
            let css = ['card-item-field', `grid-colspan-${colspan}`, `grid-rowspan-${rowspan}`];
            let name = item.attr('name');
            let field = name == '$rownum' ? {
                name: '$rownum',
                label: '行号',
                type: 'integer'
            } : me.fields[name];
            let nolabel = eval(item.attr('nolabel') || 0);
            let label = (item.attr('label') || field.label || field.name).t();
            item.replaceWith(`<div xpath="${xpath}" class="${css.join(' ')}" data-label="${label}">${nolabel ? "" : `<label>${label}：</label>`}
                <div class="component-tools">
                    <div class="btn btn-edit"><i class="fa fa-edit"></i></div>
                    <div class="btn btn-remove"><i class="fa fa-times"></i></div>
                    <div class="btn btn-move"><i class="fa fa-arrows-alt"></i></div>
                </div>
            </div>`);
            me._fields.push(name);
        });
        let content = `<div class="d-grid grid-template-columns-${me.itemCols}">
            <div class="card-body"> ${card.html()}</div>
        </div>`;
        me.dom.find('.card-panel').html(content);
        if (!me.dom.find('.card-panel [xpath]').length) {
            let holder = $('<div class="holder-item" xpath="card">卡片设计区域</div>');
            me.dom.find('.card-panel .card-body').prepend(holder);
            me.initDroppable(holder);
        }
        me.initDroppable(me.dom.find('.jui-card .card-item-field'));
        me.moveDraggable(me.dom.find('.jui-card .card-item-field'));
    }
});
