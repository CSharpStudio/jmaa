/**
 * 卡片控件
 * @example
 * <card cols="6">
 *     <toolbar buttons="default"></toolbar>
 *     <field name='code'/>
 *     <field name='status'/>
 *     <field name='type'/>
 *     <field name='related_code'/>
 *     <field name='warehouse_ids'/>
 *     <template>
 *         {@helper statusToCss}
 *              function(s){
 *                  let css = {draft:'warning',commit:'success',approve:'info',reject:'danger'}
 *                  return css[s] || 'secondary';
 *              }
 *         {@/helper}
 *         <div class="card-body">
 *             <div class="ribbon-wrapper">
 *                 <div class="ribbon bg-${status[0]|statusToCss}">
 *                     <small>${status[1]}</small>
 *                 </div>
 *             </div>
 *             <div class="text-title">${code}</div>
 *             <div class="text-content">${warehouse_ids|$tags}</div>
 *             <div class="text-content">
 *                 ${related_code}
 *                 <div class="float-right">
 *                     ${type|$index,1}
 *                 </div>
 *             </div>
 *         </div>
 *     </template>
 * </card>
 */
jmaa.component("JCard", {
    /**
     * 分页数
     */
    limit: 50,
    /**
     * ajax绑定数据
     * @param card
     * @param callback 数据绑定回调函数
     */
    ajax: jmaa.emptyFn,
    itemCols: 1,
    /**
     * 初始化控件
     */
    init() {
        let me = this;
        let dom = me.dom;
        me._fields = [];
        me.selected = [];
        if (me.arch) {
            let arch = jmaa.utils.parseXML(me.arch), card = arch.children('card');
            if (card.length > 0) {
                let tbar = card.children('toolbar');
                me.tbarArch = tbar.prop('outerHTML');
                tbar.remove();
                me.limit = me.nvl(eval(card.attr('limit')), me.limit);
                if (me.pager) {
                    me.pager.limit = me.limit;
                }
                me.onEvent('init', card.attr('on-init'));
                me.onEvent('load', card.attr('on-load'));
                me.onEvent('selected', card.attr('on-selected'));
                let dblclick = card.attr('on-dblclick');
                if (dblclick) {
                    dom.off('cardDblClick');
                    me.onEvent('cardDblClick', dblclick);
                }
                me.cols = eval(card.attr('cols') || 4);
                me.itemClass = card.attr('item-class') || me.itemClass || 'card-item';
                $.each(card[0].attributes, function (i, attr) {
                    if (attr.name === 'class') {
                        dom.addClass(attr.value);
                    } else {
                        const v = jmaa.utils.encode(attr.value);
                        dom.attr(attr.name, v);
                    }
                });
                me._presentFields = [];
                let items = [];
                card.children('field').each(function () {
                    let el = $(this),
                        name = el.attr('name'),
                        field = me.fields[name];
                    if (!field) {
                        throw new Error('模型' + me.model + '找不到字段' + name);
                    }
                    if (!field.deny) {
                        me._fields.push(name);
                        if (field.type === 'many2many' || field.type === 'one2many' || field.type === 'many2one') {
                            me._presentFields.push(name);
                        }
                        items.push(el);
                    }
                });
                let edit = card.children('edit');
                let editArch = $('<form></form>');
                if (edit.length > 0) {
                    editArch.html(edit.prop('innerHTML'));
                    let editAttrs = {};
                    $.each(edit[0].attributes, function (i, attr) {
                        editAttrs[attr.name] = attr.value;
                    });
                    editArch.attr(editAttrs);
                } else {
                    editArch.html(card.prop('innerHTML'));
                    editArch.children('template').remove();
                }
                me.editArch = editArch.prop('outerHTML');
                let tpl = card.children('template');
                if (tpl.length) {
                    me.tpl = juicer(tpl.html().replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">"));
                } else {
                    me.dataRoot = true;
                    me.itemCols = Math.min(3, me.nvl(eval(card.attr('item-cols')), me.itemCols));
                    for (let item of items) {
                        let colspan = Math.min(item.attr('colspan') || 1, me.itemCols);
                        let rowspan = item.attr('rowspan') || 1;
                        let css = ['card-item-field', `grid-colspan-${colspan}`, `grid-rowspan-${rowspan}`];
                        let name = item.attr('name');
                        let field = name == '$rownum' ? {
                            name: '$rownum',
                            label: '行号',
                            type: 'integer'
                        } : me.fields[name];
                        let render = item.attr('render');
                        if (!render) {
                            render = jmaa.renders[field.type] ? field.type : 'default';
                        }
                        let code = '$${data|$render,"' + render + '","' + name + '"}';
                        let nolabel = eval(item.attr('nolabel') || 0);
                        let label = nolabel ? "" : `<label>${(item.attr('label') || field.label || field.name).t()}：</label>`
                        item.replaceWith(`<div class="${css.join(' ')}">${label}${code}</div>`);
                    }
                    card.find('[colspan],[rowspan]').each(function () {
                        let el = $(this);
                        let colspan = el.attr('colspan') || 1;
                        let rowspan = el.attr('rowspan') || 1;
                        el.addClass(`grid-colspan-${colspan} grid-rowspan-${rowspan}`);
                    });
                    me.tpl = juicer(`<div class="d-grid grid-template-columns-${me.itemCols}"><div class="card-body"> ${card.html()}</div></div>`);
                }
            }
        }
        dom.on('click', '.card', function (e) {
            let card = $(this);
            dom.find('.card').removeClass('selected');
            card.addClass('selected');
            me.selected = [card.attr('data')];
            dom.triggerHandler('selected', [me, me.selected]);
        }).on("click", "[t-click]", function (e) {
            e.preventDefault();
            e.stopPropagation();
            let ele = $(this), card = ele.parents(".card"), click = ele.attr("t-click");
            let fn = new Function("return this." + click).call(me.view, e, card.attr("data"), me, me.view);
            if (fn instanceof Function) {
                fn.call(me.view, e, card.attr("data"), me, me.view);
            }
        }).on('dblclick', '.card', function () {
            dom.triggerHandler('cardDblClick', [me, $(this).attr('data')]);
        });
        dom.addClass('jui-card row mt-3 card-view').triggerHandler('init', [me]);
        me.load();
    },
    /**
     * 加载数据
     */
    load() {
        let me = this;
        let dom = me.dom;
        dom.html(`<div class="col-12" style="text-align:center;">${'数据加载中'.t()}</div>`);
        me.ajax(me, function (e) {
            me.data = e.data;
            if (e.data.length > 0) {
                dom.empty();
                $.each(e.data, function (i, d) {
                    for (let k of me._fields) {
                        let field = me.fields[k];
                        if (field.type === 'selection') {
                            let v = this[k];
                            this[k] = [v, field.options[v]];
                        }
                    }
                    let html = me.tpl.render(me.dataRoot ? {data: this} : this);
                    dom.append(`<div class="${me.itemClass} col-${12 / me.cols}">
                                    <div class="card${me.selected.includes(this.id) ? ' selected' : ''}" data="${this.id}">${html}</div>
                                </div>`);
                });
            } else {
                dom.html(`<div class="col-12" style="text-align:center;">${'没有数据'.t()}</div>`);
                me.selected = [];
            }
            dom.triggerHandler('load', [me]);
            dom.triggerHandler('selected', [me, me.selected]);
        });
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
                    dialog.dom.find('.buttons-right').append(`<button type="button" t-click="saveDialog" class="btn btn-flat btn-info">${'确定并关闭'.t()}</button>`);
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
        let v = $.extend(true, {}, values);
        me.renderDialogEdit(null, {}, function () {
            me.create(v);
        });
        me.editForm.create(values);
    },
    /**
     * 提交编辑数据
     */
    async save() {
        const me = this;
        await me.dom.triggerHandler('save', [me, me.editForm.getSubmitData(), $.extend({}, me.editForm.getData())]);
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
    select(id) {
        let me = this;
        me.dom.find('.card').removeClass('selected');
        if (typeof id === 'undefined') {
            me.selected = [];
        } else if (Array.isArray(id)) {
            for (let r of id) {
                me.dom.find('.card[data="${id}"]').addClass('selected');
            }
            me.selected = [...id];
        } else {
            me.dom.find('.card[data="${id}"]').addClass('selected');
            me.selected = [id];
        }
        me.dom.triggerHandler('selected', [me, me.selected]);
    },
    /**
     * 加载编辑的数据
     * @param grid 表格
     * @param id
     * @param callback
     */
    loadEdit(card, id, callback) {
        jmaa.rpc({
            model: card.model,
            module: card.module,
            method: 'read',
            args: {
                ids: [id],
                fields: card.editForm.getFields(),
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
     * 编辑行数据
     * @param id
     */
    async edit(id) {
        const me = this;
        id = id || me.selected[0];
        if (id) {
            me.renderDialogEdit(id, {
                ajax(form, callback) {
                    me.loadEdit(me, id, callback);
                },
            });
            me.editForm.load();
        }
        return me.editForm;
    },
    /**
     * 获取选中数据
     * @returns {[]|[*]|*}
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
    /**
     * 获取控件字段
     * @returns {[]}
     */
    getFields() {
        return this._fields;
    },
    /**
     * 获取使用present的字段
     * @returns {[]}
     */
    getUsePresent() {
        return this._presentFields;
    },
    /**
     * 获取排序, 不支持用户定义排序
     * @returns {string}
     */
    getSort() {
        return "";
    }
});
juicer.register('$image', function (data) {
    if (data?.id) {
        return `${jmaa.web.getTenantPath()}/attachment/${data.id}`;
    } else if ($.isArray(data) && data[0]?.id) {
        return `${jmaa.web.getTenantPath()}/attachment/${data[0].id}`;
    } else if (data) {
        return `data:image/png;base64,${data}`;
    }
    return `/web/org/jmaa/web/statics/img/placeholder.png`;
});
/**
 * 获取指定索引的值
 */
juicer.register('$index', function (data, idx) {
    if (data === null || data === undefined) {
        return '';
    }
    if (data) {
        return data[idx];
    }
    return data;
});
/**
 * *2many字段显示成tags
 */
juicer.register('$tags', function (data) {
    if (data) {
        let tags = []
        for (let d of data) {
            tags.push(d[1]);
        }
        return tags.join(',');
    }
    return data;
});
juicer.register('$render', function (data, render, field) {
    let fn = jmaa.renders[render];
    if (!fn) {
        throw new Error("找不到render:" + render);
    }
    return fn(data[field], data, field);
});
$.fn.JCard = function (opt) {
    let com = $(this).data(name);
    if (!com) {
        com = new JCard($.extend({dom: this}, opt));
        $(this).data(name, com);
    }
    return com;
};
