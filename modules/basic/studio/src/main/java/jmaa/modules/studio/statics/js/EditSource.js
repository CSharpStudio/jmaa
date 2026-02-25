//@ sourceURL=EditSource.js
jmaa.define("EditSource", {
    canCreate: true,
    getTpl() {
        let me = this;
        return `<div class="d-header">
                <div class="d-toolbar">
                    ${me.goBack ? `<button type="button" class="btn btn-flat btn-default btn-back">${'返回'}</button>` : ''}
                    ${me.canCreate ? `<button type="button" class="btn btn-flat btn-primary btn-create">${'创建'}</button>` : ''}
                    <button type="button" class="btn btn-flat btn-primary btn-save">${'保存'}</button>
                    <div class="btn btn-default btn-flat select-view" data-toggle="dropdown" aria-expanded="false">
                        <span class="select-result mr-2"><i>${'没有数据'.t()}</i></span>
                        <i class="fa fa-angle-down"></i>
                    </div>
                    <div class="dropdown-menu">
                        <ul class="select-items"></ul>
                    </div>
                </div>
            </div>
            <div class="d-body"></div>`;
    },
    __init__(opt) {
        let me = this;
        jmaa.utils.apply(true, me, opt);
        me.dom.unbind().addClass('edit-source').html(me.getTpl()).on('click', '.select-items .select-item', function () {
            let id = $(this).attr('data-id');
            me.dom.find('.select-view .select-result').html($(this).html());
            me.viewId = id;
            me.viewForm.load();
        }).on('click', '.btn-save', function () {
            me.saveView();
        }).on('click', '.btn-create', function () {
            me.viewId = null;
            let primary = me.dom.find('.select-item.primary').length;
            me.viewForm.create({module_id: me.studio.data.module, mode: primary ? 'extension' : 'primary'});
        }).on('click', '.btn-back', function () {
            me.goBack && me.goBack();
        });
        me.initViewForm();
    },
    saveView() {
        let me = this;
        let data = me.viewForm.getSubmitData();
        if (me.viewId) {
            jmaa.rpc({
                model: 'ir.model',
                method: 'updateView',
                args: {
                    viewId: me.viewId,
                    values: data,
                },
                onsuccess(r) {
                    jmaa.msg.show('操作成功'.t());
                    me.viewForm.load();
                }
            });
        } else {
            data.model = me.studio.data.model;
            data.type = me.type;
            jmaa.rpc({
                model: 'ir.model',
                method: 'createView',
                args: {
                    values: data,
                },
                onsuccess(r) {
                    jmaa.msg.show('操作成功'.t());
                    me.viewId = r.data;
                    me.viewForm.load();
                    me.loadViewSelect(function () {
                        let item = me.dom.find(`.select-items .select-item[data-id=${r.data}]`);
                        me.dom.find('.select-view .select-result').html(item.html());
                    });
                }
            });
        }
    },
    initViewForm() {
        let me = this;
        me.studio.loadView("ir.ui.view", "form", "studio-edit-view").then(v => {
            me.viewForm = me.dom.find('.d-body').JForm({
                model: v.model,
                fields: v.fields,
                arch: v.views.form.arch,
                ajax(form, callback) {
                    jmaa.rpc({
                        model: 'ir.model',
                        method: 'searchView',
                        args: {
                            criteria: [['id', '=', me.viewId]],
                            fields: form.getFields(),
                            limit: 1,
                        },
                        context: {
                            usePresent: form.getUsePresent(),
                        },
                        onsuccess(r) {
                            callback({data: r.data.values[0]})
                        }
                    })
                },
            });
            $('t').each(function () {
                let el = $(this);
                el.replaceWith(el.text().t());
            });
            me.loadViewData();
        });
    },
    loadViewData() {
        let me = this;
        me.loadViewSelect(function () {
            let item = me.dom.find('.select-items .select-item.studio');
            if (!item.length) {
                item = me.dom.find('.select-items .select-item.primary');
            }
            if (!item.length) {
                item = me.dom.find('.select-items .select-item:first');
            }
            item.click();
        });
    },
    loadViewSelect(callback) {
        let me = this;
        jmaa.rpc({
            model: 'ir.model',
            method: 'searchView',
            args: {
                criteria: [['model', '=', me.studio.data.model], ['type', '=', me.type]],
                fields: ["mode", "name", "module_id"],
                limit: 1000,
                order: 'mode desc,priority'
            },
            context: {
                usePresent: true
            },
            onsuccess(r) {
                let items = [];
                for (let row of r.data.values) {
                    items.push(`<li class="select-item${row.mode == "primary" ? ' primary' : ''}${row.module_id && row.module_id[1].includes('studio') ? ' studio' : ''}" data-id="${row.id}">${row.name}
                        <i class="ml-2 small text-muted">${row.module_id ? ` ${'来源：'.t()}${row.module_id[1]}` : ''}</i></li>`);
                }
                me.dom.find('.select-items').html(items.join(''));
                callback()
            }
        });
    }
});
