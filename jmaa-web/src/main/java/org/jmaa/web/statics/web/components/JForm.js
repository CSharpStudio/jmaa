/**
 * 表单控件
 * 使用xml声明结构，支持模型的field
 * <pre>
 *     new JForm({
 *          dom,
 *          arch: `
 *              <form>
 *                   <toolbar>
 *                       <button label="按钮"></button>
 *                   </toolbar>
 *                   <field name="code"></field>
 *                   <editor name="message" type="msg_editor" label="消息"></editor>
 *                   <tabs>
 *                       <tab label="页签">
 *                       </tab>
 *                   </tabs>
 *                   <div>原生HTML</div>
 *              </form>
 *          `
 *     });
 * </pre>
 */
jmaa.component('JForm', {
    /**
     * ajax绑定数据
     * @param form
     * @param callback 数据绑定回调函数
     * @example
     * new JForm({
     *   ajax(form, callback) {
     *       if (me.urlHash.id) {
     *           jmaa.rpc({
     *                 model: me.model,
     *                 module: me.module,
     *                 method: 'read',
     *                 args: {
     *                     ids: [me.urlHash.id],
     *                     fields: form.getFields(),
     *                 },
     *                 context: {
     *                     usePresent: true,
     *                 },
     *                 onsuccess(r) {
     *                     callback({data: r.data[0]});
     *                 },
     *             });
     *       } else {
     *           let data = {}
     *           if (me.urlHash.present) {
     *               data[me.present[0]] = me.urlHash.present;
     *           }
     *           me.form.create(data);
     *       }
     *   },
     * })
     */
    ajax(form, callback) {
    },
    /**
     * 获取渲染模板
     * @returns {string}
     */
    getTpl() {
        return `<div class="form-header"></div>
                <div class="form-content"></div>`;
    },
    /**
     * 表单列数
     */
    cols: 4,
    /**
     * 显示更新者信息
     */
    logAccess: true,
    /**
     * 左侧树形模板
     * @returns
     */
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
    /**
     * 初始化控件
     */
    init() {
        let me = this;
        let dom = me.dom;
        dom.html(me.getTpl()).addClass('jui-form');
        me._fields = [];
        me._editors = [];
        me.selected = [];
        me.usePresent = [];
        me.editors = {};
        if (me.arch) {
            let arch = jmaa.utils.parseXML(me.arch);
            let form = arch.find('form');
            let toManyArchs = {};
            if (form.length > 0) {
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
                me.onEvent('init', form.attr('on-init'));
                me.onEvent('select', form.attr('on-select'));
                me.onEvent('create', form.attr('on-create'));
                me.onEvent('save', form.attr('on-save'));
                me.onEvent('load', form.attr('on-load'));
                me.onEvent('valid', form.attr('on-valid'));
                me.loadActionHandler(form.attr('on-load-action'));
                if (!me.fields) {
                    me.fields = {};
                }
                me._processFields(form, toManyArchs);
                me._processEditors(form);
                me._processToolbar();
                me._processTabs(form);
                me._processGroup(form);
                me._initLogAccess(form);
                form.addClass('d-grid e-form');
                form.attr('role', 'form-body');
                let html = arch.children().prop('outerHTML').replaceAll('<form', '<div').replaceAll('/form>', '/div>');
                html = `<div class="form-body"><div class="form-card">${html}</div></div>`;
                if (me.isTree) {
                    html = me.getAsideTpl() + html;
                    dom.find('.form-content').css('display', 'flex');
                }
                let formBody = dom.find('.form-content').html(html).find('[role=form-body]')
                    .on('click', '.field-group .group-header.collapsable', function () {
                        let header = $(this);
                        header.parent().toggleClass('collapsed');
                    });
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
        }
        me.dom.on('click', '[data-role=tooltip]', function () {
            me._showTooltip($(this));
        }).triggerHandler('init', [me]);
        if (me.data) {
            me.setData(me.data);
        }
        me.initData();
    },
    initData() {
        let me = this;
        me.$data = {};
        me.$rawdata = {};
        $.each([...me._fields, ...me._editors], function (i, name) {
            try {
                Object.defineProperty(me.$data, name, {
                    get() {
                        return me.getEditor(name).getValue();
                    },
                    set(value) {
                        me.getEditor(name).setValue(value);
                    },
                    enumerable: true
                });
                Object.defineProperty(me.$rawdata, name, {
                    get() {
                        return me.getEditor(name).getRawValue();
                    },
                    set(value) {
                        me.getEditor(name).setValue(value);
                    },
                    enumerable: true
                });
            } catch (e) {
                console.log(e);
            }
        });
        Object.defineProperty(me.$data, 'id', {
            get() {
                return me.dataId;
            },
            enumerable: true
        });
        Object.defineProperty(me.$rawdata, 'id', {
            get() {
                return me.dataId;
            },
            enumerable: true
        });
    },
    loadActionHandler(action) {
        let me = this;
        if (action) {
            me.dom.on('load', function () {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'action',
                    args: {
                        ids: [me.dataId || '@newid'],
                        values: me.getRawData(),
                        action: action,
                    },
                    onsuccess(r) {
                        me.loading = true;
                        me.clearInvalid();
                        me.handleAction(r.data);
                        me.clean();
                        me.loading = false;
                    },
                });
            });
        }
    },
    _showTooltip(item) {
        let help = item.attr('data-tooltip');
        let offset = item.offset();
        let tooltip = $(`<div class="tooltip form-tooltip fade show" style="position:absolute;top:0;left:0;will-change:transform;">
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
    /**
     * 创建编辑器
     */
    _createEditors(toManyArchs) {
        let me = this;
        let dom = me.dom;
        dom.find('[data-field],[data-editor]').each(function () {
            let item = $(this);
            let el = item.find('.editor');
            let fname = item.attr('data-field');
            let field = fname ? me.fields[fname] : {};
            let name = fname || item.attr('data-editor');
            let type = fname ? el.attr('editor') || field.type : el.attr('type');
            let editorClass = jmaa.editors[type];
            if (!editorClass) {
                window.postError && window.postError(name + '找不到编辑器:' + editor);
                return console.error(name + '找不到编辑器:' + el.prop("outerHTML"));
            }
            let opt = {
                field,
                name,
                model: me.model,
                module: me.module,
                owner: me,
                dom: el,
                view: me.view,
            };
            if (field.type === 'many2many' || field.type === 'one2many') {
                opt.arch = toManyArchs[name];
            }
            let editor = new editorClass(opt);
            if (editor.usePresent) {
                me.usePresent.push(name);
            }
            if (editor.onValueChange) {
                editor.onValueChange(function (e, target) {
                    target.dirty = true;
                    me.dirty = true;
                    if (!me.loading && !target.loading) {
                        me._updateEditorState();
                        me.valid(target.name);
                        me._onFieldChange(target);
                    }
                    delete target.loading;
                });
            }
            me.editors[name] = editor;
        });
        dom.find('[t-reset]').each(function () {
            let e = $(this);
            let name = e.attr('form-editor');
            let reset = e.attr('t-reset');
            if (name && reset) {
                $.each(reset.split(','), function () {
                    let editor = me.editors[this];
                    if (editor.onValueChange) {
                        editor.onValueChange(function () {
                            if (!me.loading) {
                                me.getEditor(name).resetValue();
                            }
                        });
                    }
                });
            }
        });
    },
    /**
     * 初始化树控件
     * @param form
     * @private
     */
    _createTreeView(arch) {
        let me = this;
        let dom = me.dom;
        let sortField = arch.attr('sort-field');
        let allowSort = Boolean(sortField);
        let dragInner = eval(arch.attr('drag-inner') || 1);
        let beforeDrop = arch.attr('before-drop');
        me.treeview = new JTree({
            dom: dom.find('.treeview'),
            model: me.model,
            module: me.module,
            fields: me.fields,
            form: me,
            presentField: arch.attr('present') || 'present',
            parentField: arch.attr('parent') || 'parent_id',
            sortField,
            ajax(tree, callback) {
                me.loadTreeData(tree, callback);
            },
            on: {
                selected(e, tree, selected) {
                    selected.remove(s => s.id == 'root');
                    dom.triggerHandler('treeSelect', [me.treeview, selected]);
                },
                load(e, tree) {
                    let root = tree.ztree.getNodeByParam("id", null, null);
                    if (root) {
                        tree.ztree.expandNode(root, true, false, true);
                    } else {
                        tree.ztree.expandAll(true);
                    }
                },
                init(e, tree) {
                    tree.ztreeSetting.edit.drag.prev = allowSort;
                    tree.ztreeSetting.edit.drag.next = allowSort;
                    tree.ztreeSetting.edit.drag.inner = dragInner;
                    tree.ztreeSetting.view.dblClickExpand = false;
                    tree.ztreeSetting.view.addDiyDom = function (treeId, treeNode) {
                        if (treeNode.id == 'root') {
                            me.treeview.dom.find('#' + treeNode.tId).addClass('root');
                        }
                    }
                    tree.ztreeSetting.callback.beforeDrop = function (treeId, treeNodes, targetNode, moveType) {
                        if (beforeDrop) {
                            return me.getFunction(beforeDrop).call(me, treeId, treeNodes, targetNode, moveType);
                        }
                    };
                    tree.ztreeSetting.callback.onDrop = function (event, treeId, treeNodes, targetNode, moveType) {
                        if (moveType != null) {
                            me.onTreeNodeDrop(event, treeId, treeNodes, targetNode, moveType);
                        }
                    };
                },
            },
        });
        me.treeview.load();
        dom.on('click', '.tree-expand', function () {
            me.treeview.expandAll();
        }).on('click', '.tree-collapse', function () {
            let root = me.treeview.ztree.getNodeByParam("id", null, null);
            if (root) {
                for (let node of root.children) {
                    me.treeview.ztree.expandNode(node, false, true, true);
                }
            } else {
                me.treeview.collapseAll();
            }
        }).on('click', '.tree-lookup', function () {
            me.treeview.load();
        }).on('keydown', '.tree-keyword', function (e) {
            if (e.key == 'Enter') {
                me.treeview.load();
            }
        });
        me.onEvent('treeSelect', arch.attr('on-select'))
    },
    loadTreeData(tree, callback) {
        let me = this;
        let kw = me.dom.find('.tree-keyword').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'presentSearch',
            args: {
                keyword: kw,
                offset: 0,
                limit: 0,
                order: '',
                fields: tree.getFields(),
            },
            onsuccess(r) {
                let showRoot = !r.data.length;
                if (r.data.length) {
                    for (let item of r.data) {
                        if (item[tree.parentField] == null) {
                            item[tree.parentField] = 'root';
                            showRoot = true;
                        }
                    }
                }
                if (showRoot) {
                    let root = {id: 'root'};
                    root[tree.presentField] = '根节点'.t();
                    r.data.unshift(root);
                }
                callback(r.data);
            },
        });
    },
    onTreeNodeDrop(event, treeId, treeNodes, targetNode, moveType) {
        let me = this;
        let ids = [];
        let values = {};
        let pId = me.treeview.parentField;
        let id = me.treeview.idField;
        let sort = me.treeview.sortField;
        let getId = function (field) {
            let targetId = targetNode ? targetNode[field] : null;
            if (targetId == 'root') {
                targetId = null;
            }
            return targetId;
        }
        if (moveType === 'inner') {
            values[pId] = getId(id);
        } else if (moveType === 'next') {
            values[pId] = getId(pId);
            values[sort] = targetNode ? targetNode[sort] + 1 : 0;
        } else if (moveType === 'prev') {
            values[pId] = getId(pId);
            values[sort] = targetNode ? targetNode[sort] - 1 : 0;
        } else {
            return;
        }
        $.each(treeNodes, function () {
            ids.push(this[id]);
        });
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'update',
            args: {
                ids,
                values,
            },
            onsuccess(r) {
                $.each(treeNodes, function () {
                    $.extend(this, values);
                });
                me.dom.triggerHandler('treeSelect', [me.treeview, treeNodes]);
                jmaa.msg.show('操作成功'.t());
            },
        });
    },
    /**
     * 初始化工具条上的
     * @private
     */
    _processToolbar() {
        let me = this;
        if (!me.tbarArch) {
            return;
        }
        me.dom.find('.form-header').append('<div part="form-toolbar" class="toolbar"></div>');
        let auths = me.view ? me.view.auths : '';
        me.toolbar = new JToolbar({
            dom: me.dom.find('[part=form-toolbar]'),
            arch: me.tbarArch,
            auths,
            defaultButtons: me.isTree ? 'createChild|save|delete' : 'create|save|reload',
            target: me,
            view: me.view,
        });

        let html = [];
        jmaa.utils.parseXML(me.tbarArch).find('field').each(function () {
            let el = $(this);
            let name = el.attr('name');
            let field = me.fields[name];
            if (!field) {
                window.postError && window.postError('模型' + me.model + '找不到字段' + name);
                return console.error('模型' + me.model + '找不到字段' + name);
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
            me.dom.find('.form-header').append(`<div class="bar-right">${html.join('')}</div>`);
        }
    },
    /**
     * 把field转换成DOM
     * <pre>
     *     <field name="code"></field>
     *     转换成以下结构
     *     <div form-editor="code" ...>
     *         <label>...</label>
     *         <div data-field="code" ...></div>
     *         <span class="invalid-feedback" style="display: none;"></span>
     *     </div>
     * </pre>
     */
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
                window.postError && window.postError('模型' + me.model + '找不到字段' + name);
                return console.error('模型' + me.model + '找不到字段' + name);
            }
            field.name = field.name || name;
            if (field.deny) {
                el.remove();
            } else {
                me._fields.push(name);
                field.$defaultValue = el.attr('default') || el.attr('defaultValue') || field.defaultValue;
                if (field.type === 'many2many' || field.type === 'one2many') {
                    toManyArchs[name] = el.html();
                }
                let html = me._getEditorHtml(el, field);
                el.replaceWith(html);
            }
        });
    },
    /**
     * 把editor转换成DOM
     * <pre>
     *     <editor name="code" type="char" label="编码"></editor>
     *     转换成以下结构
     *     <div form-editor="code" ...>
     *         <label>编码</label>
     *         <div data-editor="code" ...></div>
     *         <span class="invalid-feedback" style="display: none;"></span>
     *     </div>
     * </pre>
     */
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
    _getEditorHtml(el, field, isEditor) {
        let me = this;
        let colspan = Math.min(el.attr('colspan') || 1, me.cols);
        let rowspan = el.attr('rowspan') || 1;
        let css = ['form-group'];
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
        let getLabel = function () {
            let nolabel = el.attr('nolabel');
            if (!eval(nolabel)) {
                let help = el.attr('help') || field.help;
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
        return `<div form-editor="${field.name}" ${attrs.join(' ')}>
                    ${getLabel()}
                    <div data-${isEditor ? 'editor' : 'field'}="${field.name}">
                        <div class="editor" label="${label}" ${domAttrs.join(' ')}></div>
                        <span class="invalid-feedback"></span>
                    </div>
                </div>`;
    },
    /**
     * 初始化分组布局
     * @param form
     * @private
     */
    _processGroup(form) {
        let me = this;
        form.find('group').each(function () {
            let group = $(this);
            let colspan = group.attr('colspan') || me.cols;
            let rowspan = group.attr('rowspan') || 1;
            let cols = group.attr('cols') || colspan;
            let attrs = [];
            let css = ['field-group'];
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
            let header = `<div class="group-header${collapsable ? ' collapsable' : ''}">${label + expander}</div>`;
            let body = `<div class="group-body"><div class="group-content d-grid" style="grid-template-columns:repeat(${cols + ', 1fr'})">${group.prop('innerHTML')}</div></div>`;
            let html = `<div class="${css.join(' ')}"${style.length ? ` style="${style.join('')}"` : ''} ${attrs.join(' ')}>
                            ${position == 'bottom' ? body + header : header + body}
                        </div>`;
            group.replaceWith(html);
        });
    },
    /**
     * 初始化Tabs布局
     * @param form
     * @private
     */
    _processTabs(form) {
        let me = this;
        form.find('tabs').each(function () {
            let tabs = $(this);
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
                if (tab.children().length > 0) {
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
                                <a class="${linkCss.join(' ')}" id="${id}-tab" data-toggle="pill" href="#${id}" role="tab" aria-controls="${id}" aria-selected="true">${label}</a>
                            </li>`);
                    content.push(`<div class="${paneCss.join(' ')}" id="${id}" role="tabpanel" aria-labelledby="${id}-tab">
                                    <div class="d-grid mt-2" style="grid-template-columns:repeat(${cols + ', 1fr'})">${tab.prop('innerHTML')}</div>
                                </div>`);
                }
            });
            if (nav.length) {
                let html = `<div class="tabs-panel ${tabCss.join(' ')}"${tabStyle.length ? ` style="${tabStyle.join('')}"` : ''} ${tabAttrs.join(' ')}>
                                <ul class="nav nav-tabs" role="tablist">${nav.join('')}</ul>
                                <div class="tab-content">${content.join('')}</div>
                            </div>`;
                tabs.replaceWith(html);
            } else {
                tabs.remove();
            }
        });
    },
    /**
     * 初始化访问字段
     * @param form
     * @private
     */
    _initLogAccess(form) {
        let me = this;
        let logAccess = me.fields.create_uid && eval(form.attr('logAccess') || me.logAccess);
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
    /**
     * 根据t-visible,t-readonly计算结果更新DOM状态
     * @private
     */
    _updateEditorState() {
        let me = this;
        let toUpdate = me.dom.find('[t-visible],[t-readonly]');
        if (toUpdate.length > 0) {
            let data = me.getRawData();
            toUpdate.each(function () {
                let e = $(this);
                let visible = e.attr('t-visible');
                let readonly = e.attr('t-readonly');
                if (visible) {
                    visible = visible.replace(/ and /gi, " && ");
                    data.__test_visible = new Function('with(this) return ' + jmaa.utils.decode(visible));
                    let result = data.__test_visible();
                    if (result) {
                        e.removeClass('d-none');
                    } else {
                        e.addClass('d-none');
                        if (e.hasClass('tab-head')) {
                            let tabId = e.find('a.nav-link').removeClass('active').attr('aria-controls');
                            me.dom.find('#' + tabId).removeClass('active');
                        }
                    }
                    if (e.hasClass('tab-head') && !e.parent().find('a.nav-link.active').length) {
                        // requestAnimationFrame 这里需要等待下一帧执行
                        requestAnimationFrame(() => {
                            e.parent().find('.tab-head:not(.d-none):first a').trigger('click');
                        });
                    }
                }
                if (readonly) {
                    let name = e.attr('form-editor');
                    let editor = me.getEditor(name);
                    readonly = readonly.replace(/ and /gi, " && ");
                    data.__test_visible = new Function('with(this) return ' + jmaa.utils.decode(readonly));
                    let result = data.__test_visible();
                    editor.readonly(result);
                }
            });
        }
    },
    /**
     * 根据visible,readonly初始化DOM状态
     */
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
    /**
     * 根据readonly更新控件状态
     * @private
     */
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
    triggerChange(editor) {
        let me = this;
        me._onFieldChange(me.editors[editor]);
    },
    /**
     * 字段变更
     * @param editor
     * @private
     */
    _onFieldChange(editor) {
        let me = this;
        if (editor.dom.attr('onchange')) {
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'onchange',
                args: {
                    ids: [me.dataId || '@newid'],
                    values: me.getRawData(),
                    field: editor.field.name,
                },
                onsuccess(r) {
                    me.loading = true;
                    me.clearInvalid();
                    for (let k in r.data) {
                        let ed = me.editors[k];
                        if (ed) {
                            ed.setValue(r.data[k]);
                            ed.dirty = true;
                        }
                    }
                    me.loading = false;
                    me._updateEditorState();
                },
            });
        }
        let change = editor.dom.attr('t-change');
        if (change) {
            let fn = new Function('return this.' + change).call(me.view, editor);
            if (fn instanceof Function) {
                fn.call(me.view, editor);
            }
        }
        let action = editor.dom.attr('on-change-action');
        if (action) {
            let context = editor.dom.attr('action-context');
            if (context) {
                context = me.getFunction(context).call(me);
            }
            let values = {};
            $.each([...me._fields], function (i, name) {
                values[name] = me.editors[name].getRawValue();
            });
            jmaa.rpc({
                model: me.model,
                module: me.module,
                method: 'action',
                args: {
                    ids: [me.dataId || '@newid'],
                    values,
                    action,
                },
                context,
                onsuccess(r) {
                    me.loading = true;
                    me.clearInvalid();
                    me.handleAction(r.data);
                    me.loading = false;
                    me._updateEditorState();
                },
            });
        }
    },
    /**
     * 处理Action结果
     */
    handleAction(data) {
        let me = this;
        if (data.action == 'attr') {
            me.handleAttrAction(data);
        } else if (data.action == 'dialog') {
        } else if (data.action == 'js') {
        }
    },
    /**
     * 处理AttrAction
     * @param data
     */
    handleAttrAction(data) {
        let me = this;
        for (let name in data.attrs) {
            let editor = me.editors[name];
            if (editor) {
                let d = data.attrs[name];
                for (let attr in d) {
                    if (attr == 'value') {
                        editor.setValue(d.value);
                        editor.dirty = true;
                    } else if (attr == 'readonly') {
                        editor.readonly(d.readonly);
                    } else if (attr == 'visible') {
                        me.setEditorVisible(name, d.visible);
                    } else if (attr == 'required') {
                        me.setEditorRequired(name, d.required);
                    } else {
                        editor.setAttr(attr, d[attr]);
                    }
                }
            }
        }
    },
    /**
     * 设置编辑器显示/隐藏
     * @param field
     * @param visible
     */
    setEditorVisible(name, visible) {
        let me = this;
        if (visible) {
            me.dom.find(`[form-editor=${name}]`).show();
        } else {
            me.dom.find(`[form-editor=${name}]`).hide();
        }
    },
    /**
     * 设置编辑器必填
     * @param field
     * @param required
     */
    setEditorRequired(name, required) {
        let me = this;
        if (required) {
            me.dom.find(`[form-editor=${name}] .required`).html('*');
            me.dom.find(`[form-editor=${name}] .editor`).attr('required', true);
        } else {
            me.dom.find(`[form-editor=${name}] .required`).html('');
            me.dom.find(`[form-editor=${name}] .editor`).attr('required', false);
        }
    },
    /**
     * 重置只读状态
     */
    resetReadonly() {
        let me = this;
        me.setReadonly(false);
        me._updateReadonly();
        me._updateEditorState();
    },
    /**
     * 设置只读
     * @param readonly
     */
    setReadonly(readonly) {
        let me = this;
        for (let editor in me.editors) {
            me.editors[editor].readonly(readonly);
        }
    },
    focus() {
        let me = this;
        me.dom.find('.form-body [focusable]:not([disabled]):not(.disabled):not([readonly]):not(.readonly):first').focus();
    },
    /**
     * 获取表单的字段
     * @returns {[]}
     */
    getFields() {
        return this._fields;
    },
    /**
     * 获取使用present的字段，使用默认值true
     * @returns {boolean}
     */
    getUsePresent() {
        return this.usePresent;
    },
    /**
     * 获取当前选中 [id]
     * @returns {[]|[*]|*}
     */
    getSelected() {
        return this.selected;
    },
    /**
     * 设置值
     * @param data
     */
    setData(data) {
        let me = this;
        me.dataId = data.id || '';
        if (data.id) {
            me.selected = [data.id];
            if (data.id.startsWith('new')) {
                me.dom.find('.log-access').hide();
            } else {
                me.dom.find('.log-access').show();
            }
        } else {
            me.selected = [];
            me.dom.find('.log-access').hide();
        }
        me.loading = true;
        $.each([...me._fields, ...me._editors], function (i, name) {
            me.getEditor(name).setValue(data[name]);
        });
        me.loading = false;
        if (window.view && window.view.urlHash && eval(window.view.urlHash.readonly)) {
            me.setReadonly(true);
            me._updateEditorState();
        } else {
            me.resetReadonly();
        }
        me.clearInvalid();
        me.focus();
        me.dom.triggerHandler('selected', [me, me.selected]);
    },
    /**
     * 设置字段的错误提示
     * @param field
     * @param error
     */
    setInvalid(name, error) {
        let me = this;
        let editor = me.dom.find(`[form-editor=${name}]`).addClass('is-invalid');
        editor.find('.invalid-feedback').html(error).show();
        editor.find('.form-control').addClass('is-invalid');
    },
    /**
     * 获取所有控件的错误信息
     * @returns {string}
     */
    getErrors() {
        let me = this;
        let error = [];
        let _valid = function (name) {
            let dom = me.dom.find('[form-editor=' + name + ']');
            if (dom.hasClass('d-none') || dom.css('display') == 'none') {
                return;
            }
            let editor = me.getEditor(name);
            let addError = function (err) {
                if (err) {
                    error.push(editor.dom.attr('label') + ':' + err);
                }
            };
            addError(me._requiredValid(editor));
            if (editor.valid) {
                addError(editor.valid());
            }
        };
        $.each([...me._fields, ...me._editors], function (i, name) {
            _valid(name);
        });
        let err = me.dom.triggerHandler('valid', [me]);
        if (typeof err != 'undefined') {
            error.push(err)
        }
        return error.join('<br/>');
    },
    /**
     * 验证指定name的编辑器，或者验证整个表单
     * @example
     *     if(!form.valid()){
     *         jmaa.msg.error(form.getErrors());
     *     }
     * @returns {boolean}
     */
    valid(name) {
        let me = this;
        let result = true;
        let _valid = function (name) {
            let dom = me.dom.find('[form-editor=' + name + ']');
            if (dom.hasClass('d-none') || dom.css('display') == 'none') {
                return;
            }
            let editor = me.getEditor(name);
            let error = [];
            let addError = function (err) {
                if (err) {
                    error.push(err);
                }
            };
            addError(me._requiredValid(editor));
            if (editor.valid) {
                addError(editor.valid());
            }
            if (error.length > 0) {
                me.setInvalid(name, error.join(';'));
                result = false;
            }
        };
        me.clearInvalid(name);
        if (name) {
            _valid(name);
        } else {
            $.each([...me._fields, ...me._editors], function (i, name) {
                _valid(name);
            });
            let err = me.dom.triggerHandler('valid', [me]);
            if (typeof err != 'undefined') {
                result = false;
            }
        }
        return result;
    },
    /**
     * 必填验证
     */
    _requiredValid(editor) {
        let required = editor.dom[0].getAttribute('required');
        if (!required) {
            required = editor.field.required;
        }
        if (required == 'required' || eval(required)) {
            let val = editor.getValue();
            if (val === '' || val == null || val == undefined) {
                return '不能为空'.t();
            }
            if ($.isArray(val) && val.length == 0) {
                return '不能为空'.t();
            }
        }
    },
    /**
     * 清空验证失败的提示
     */
    clearInvalid(name) {
        let me = this;
        if (name) {
            let editor = me.dom.find(`[form-editor=${name}]`).removeClass('is-invalid');
            editor.find('.invalid-feedback').empty().hide();
            editor.find('.form-control').removeClass('is-invalid');
        } else {
            me.dom.find('.invalid-feedback').empty().hide();
            me.dom.find('.is-invalid').removeClass('is-invalid');
        }
    },
    /**
     * 获取加载的数据，对字段的读写调用editor的getValue(getRawValue)/setValue方法。
     * @param raw 是否使用raw值
     * @returns {*|{}}
     */
    getData(raw) {
        if (raw) {
            return this.$rawdata;
        }
        return this.$data;
    },
    getRaw() {
        return this.$rawdata;
    },
    /**
     * 获取提交的数据
     * @returns {{id: (*|string)}}
     */
    getSubmitData() {
        let me = this;
        let data = {id: me.dataId};
        let all = !me.dataId || me.dataId.startsWith('new');
        $.each([...me._fields, ...me._editors], function (i, name) {
            let editor = me.getEditor(name);
            if (!editor.noEdit && (all || editor.dirty)) {
                data[name] = editor.getDirtyValue();
            }
        });
        return data;
    },
    /**
     * 清空所有字段的dirty状态
     */
    clean() {
        let me = this;
        me.dirty = false;
        $.each([...me._fields, ...me._editors], function (i, name) {
            me.getEditor(name).dirty = false;
        });
    },
    /**
     * 获取存储格式的数据
     * @returns {{}}
     */
    getRawData(name) {
        let me = this;
        let data = {id: me.dataId};
        let get = function (name) {
            let editor = me.getEditor(name);
            return editor.getRawValue();
        }
        if (name) {
            return get(name);
        }
        $.each([...me._fields, ...me._editors], function (i, name) {
            data[name] = get(name);
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
            throw new Error('找不到name=[' + name + ']的editor');
        }
        return e;
    },
    /**
     * 创建子数据
     */
    async createChild() {
        let me = this;
        if (me.treeview) {
            delete view.urlHash.id;
            let data = {};
            let selected = me.treeview.ztree.getSelectedNodes()[0];
            if (selected && selected.id != 'root') {
                data[me.treeview.parentField] = [selected.id, selected[me.treeview.presentField]];
            }
            await me.create(data);
        }
    },
    /**
     * 创建数据，触发create事件。
     * @param values
     */
    async create(values) {
        let me = this;
        let result = await me.dom.triggerHandler('create', [me, values]);
        let data = me.getDefaultValue();
        if (typeof result !== 'undefined') {
            $.extend(true, data, result);
        } else {
            $.extend(true, data, values);
        }
        me.setData(data);
        me.dom.triggerHandler('load', [me]);
    },
    /**
     * 获取字段默认值
     * @returns {{}}
     */
    getDefaultValue() {
        let me = this, data = {};
        $.each(me.getFields(), function () {
            let field = me.fields[this];
            let val = field.$defaultValue;
            if (val == 'env.user') {
                val = [env.user.id, env.user.name];
            } else if (val == 'env.company') {
                val = [env.company.id, env.company.name];
            } else if (val == 'env.user.id' || val == 'env.company.id') {
                val = eval(val);
            } else if (val == '[env.company]' || val == '[env.company.id]') {
                val = [[4, env.company.id, 0]];
            }
            data[this] = val;
        });
        return data;
    },
    /**
     * 保存，表单验证成功后触发save事件
     */
    save() {
        let me = this;
        if (!me.valid()) {
            let errors = me.getErrors();
            jmaa.msg.error(errors);
            return;
        }
        me.dom.triggerHandler('save', [me]);
        me.dirty = false;
    },
    /**
     * 删除，触发delete事件
     */
    delete() {
        let me = this;
        me.dom.triggerHandler('delete', [me, me.dataId]);
    },
    onLoad() {
        let me = this;
        me.dom.triggerHandler('load', [me]);
    },
    loadData(data) {
        let me = this;
        me.setData(data);
        me.clean();
        me.dom.triggerHandler('load', [me]);
    },
    reload() {
        let me = this;
        if (me.dirty) {
            jmaa.msg.confirm({
                title: '提示'.t(),
                content: '数据未保存，是否继续？'.t(),
                submit() {
                    me.load();
                }
            })
        } else {
            me.load();
        }
    },
    /**
     * 加载数据
     */
    load() {
        let me = this;
        if (me.treeview) {
            me.treeview.load();
        }
        me.ajax(me, function (r) {
            me.setData(r.data);
            me.clean();
            me.dom.triggerHandler('load', [me]);
        });
    },
});
$(function () {
    $(document).on('mousedown', function (e) {
        if ($(e.target).closest('.form-tooltip').length == 0) {
            $('.form-tooltip').remove();
        }
    });
});
$.fn.JForm = function (opt) {
    let com = $(this).data(name);
    if (!com) {
        com = new JForm($.extend({dom: this}, opt));
        $(this).data(name, com);
    }
    return com;
};
