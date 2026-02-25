//@ sourceURL=route.js
jmaa.view({
    getTpl() {
        let me = this;
        let tpl = me.callSuper();
        tpl += '<div class="flow-panel view-panel"></div>';
        return tpl;
    },
    changeView(viewType) {
        let me = this;
        let m = viewType || me.urlHash.view;
        const changed = me.viewType !== m;
        if (changed && m == 'flow') {
            me.showFlow();
        }
        return me.callSuper(viewType);
    },
    showFlow() {
        let me = this;
        if (!me.flow) {
            me.flow = new RouteFlow({
                dom: me.dom.find('.flow-panel'),
                model: me.model,
                view: me
            });
        }
        me.flow.dataId = me.urlHash.id;
        me.flow.backView = me.viewType || 'grid';
        me.toolbar = null;
        me.curView = me.flow;
        me.dom.find('.view-panel').hide();
        me.dom.find('.flow-panel').show();
        me.flow.load();
        me.onViewChange();
    },
    editFlow() {
        let me = this;
        me.urlHash.id = me.getSelected()[0];
        me.urlHash.readonly = false;
        me.changeView('flow');
    },
    copyVersion(e, target) {
        let me = this;
        jmaa.showDialog({
            title: '复制'.t(),
            css: 'sm',
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="1">
                            <editor name="code" type="char" label="编码" required="1"></editor>
                            <editor name="version" type="char" label="版本" required="1"></editor>
                        </form>`
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getData();
                jmaa.rpc({
                    model: target.model,
                    module: target.module,
                    method: 'copyVersion',
                    dialog,
                    args: {
                        ids: target.getSelected(),
                        code: data.code,
                        version: data.version,
                    },
                    onsuccess() {
                        target.reload();
                        dialog.close();
                        jmaa.msg.show('操作成功'.t());
                    }
                })
            }
        })
    },
});
jmaa.editor('route-version', {
    buttonsTpl: {
        create: `<button name="create" class="btn-primary btn-edit-group" t-click="create">${'添加'.t()}</button>`,
        edit: `<button name="edit" auth="update" class="btn-blue btn-edit-group" t-enable="id" t-click="edit">${'编辑'.t()}</button>`,
        delete: `<button name="delete" t-enable="ids" class="btn-danger btn-edit-group" t-click="delete" confirm="${'确定删除?'.t()}">${'删除'.t()}</button>`,
        import: `<button name="import" auth="create|update" class="btn-default btn-edit-group" position="after" t-click="import">${'导入'.t()}</button>`,
        export: `<button name="export" auth="read" position="after" t-click="export">${'导出'.t()}</button>`,
    },
    getTpl() {
        return `<div class="toolbar version-toolbar border-bottom"></div>
                <div class="form-content d-flex">
                    <aside class="left-aside border-right">
                        <div class="route-version m-0"></div>
                    </aside>
                    <div class="right-content">
                        <div class="route-details"></div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.delete = [];
        me.create = [];
        me.update = [];
        me.data = [];
        me.dom.html(me.getTpl()).addClass("route-version d-grid");
        let arch = jmaa.utils.parseXML(me.arch);
        me.initVersion(arch.children('aside').html());
        me.initDetails(arch.children('details').html());
    },
    initVersion(arch) {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadFields',
            args: {
                model: me.field.comodel,
            },
            onsuccess(r) {
                me.versionCard = me.dom.find('.route-version').JCard({
                    model: me.field.comodel,
                    module: me.module,
                    arch: `<card cols="1">${arch}</card>`,
                    fields: r.data.fields,
                    view: me.view,
                    editable: true,
                    itemClass: 'version-item',
                    on: {
                        selected(e, card, ids) {
                            const selected = [];
                            $.each(ids, function (i, id) {
                                $.each(me.data, function () {
                                    if (this.id === id) {
                                        selected.push(this);
                                    }
                                });
                            });
                            if (me.toolbar) {
                                me.toolbar.update(selected);
                            }
                            if (me.detailsGrid && me.detailsGrid.dataId != ids[0]) {
                                me.detailsGrid.load();
                            }
                        },
                        cardDblClick(e, card, id) {
                            card.edit(id);
                        },
                        async save(e, card, dirty, data) {
                            await me.saveVersion(dirty, data);
                        },
                        async delete(e, card, ids) {
                            await me.deleteVersion(card, ids);
                        }
                    },
                    ajax(card, callback) {
                        me.searchVersion(card, callback);
                    },
                    reload() {
                        delete me.data;
                        me.versionCard.load();
                    },
                    loadEdit(grid, id, callback) {
                        if (!id) return;
                        if (id.startsWith('new')) {
                            let data = me.create.find(item => item.id === id);
                            callback({data});
                        } else {
                            jmaa.rpc({
                                model: me.model,
                                module: me.module,
                                method: 'searchByField',
                                args: {
                                    relatedField: me.field.name,
                                    criteria: [['id', '=', id]],
                                    fields: grid.editForm.getFields(),
                                },
                                context: {
                                    usePresent: true,
                                },
                                onsuccess(r) {
                                    let data = r.data.values[0];
                                    let dirty = me.update.find(item => item.id === id);
                                    if (dirty) {
                                        // 使用data数据更新一次，没有使用update的数据更新多次
                                        let item = me.data.find(item => item.id === id);
                                        $.extend(data, item);
                                    }
                                    callback({
                                        data: data,
                                    });
                                },
                            });
                        }
                    }
                });
                let auths = 'read';
                for (const auth of me.view.auths) {
                    if (auth[me.field.comodel]) {
                        auths = auth[me.field.comodel];
                        auths.push('read');
                        break;
                    }
                }
                me.toolbar = new  JToolbar({
                    dom: me.dom.find('.version-toolbar'),
                    arch: me.versionCard.tbarArch,
                    auths,
                    defaultButtons: 'create|edit|delete|import|reload',
                    buttonsTpl: me.buttonsTpl,
                    target: me.versionCard,
                    view: me.view,
                });
                me.versionCard.load();
            },
        });
    },
    initDetails(arch) {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadFields',
            args: {
                model: 'pr.route_node',
            },
            onsuccess(r) {
                me.detailsGrid = me.dom.find('.route-details').JGrid({
                    model: 'md.bom_details',
                    module: me.module,
                    arch: `<grid>${arch}</grid>`,
                    fields: r.data.fields,
                    view: me.view,
                    ajax(grid, callback) {
                        me.loadDetails(grid, callback);
                    },
                });
            },
        });
    },
    loadDetails(grid, callback) {
        let me = this;
        if (!me.versionCard || (me.versionCard.selected[0] || '').startsWith("new-")) {
            callback({data: []});
            return;
        }
        grid.dataId = me.versionCard.getSelected()[0];
        jmaa.rpc({
            model: 'pr.route_version',
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: 'node_ids',
                criteria: [["version_id", "=", grid.dataId], ['active', '=', true]],
                fields: grid.getFields(),
                order: grid.getSort(),
            },
            context: {
                usePresent: true,
                active_test: false,
                company_test: false,
            },
            onsuccess(r) {
                callback({
                    data: r.data.values,
                });
            },
        });
    },
    searchVersion(card, callback) {
        let me = this;
        if (me.data) {
            callback({data: me.data});
            return;
        }
        if (!me.owner.dataId) {
            me.data = [];
            callback({data: me.data});
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: me.field.name,
                criteria: [["route_id", "=", me.owner.dataId]],
                fields: card.getFields(),
                order: card.getSort(),
                limit: 1000,
            },
            context: {
                usePresent: true,
                active_test: false,
                company_test: false,
            },
            onsuccess(r) {
                me.data = r.data.values;
                if (card.getSelected().length == 0) {
                    let d = r.data.values.find(d => d.is_default);
                    d && card.selected.push(d.id);
                }
                callback({
                    data: r.data.values,
                });
            },
        });
    },
    async saveVersion(dirty, data) {
        let me = this;
        data = data || dirty;
        if (data.id) {
            let row = me.data.find(r => r.id == data.id);
            if (row) {
                $.extend(row, data);
            }
            if (data.id.startsWith('new')) {
                me.create.remove(item => item.id == data.id);
                me.create.push(dirty);
            } else {
                me.update.push(dirty);
            }
        } else {
            dirty.id = 'new-' + jmaa.nextId();
            data.id = dirty.id;
            me.create.push(dirty);
            me.data.push(data);
        }
        me.dom.triggerHandler('valueChange', [me]);
        me.versionCard.load();
    },
    async deleteVersion(card, ids) {
        let me = this;
        for (let id of ids) {
            if (id.startsWith('new')) {
                me.create.remove(item => item.id === id);
            } else {
                me.delete.push(id);
            }
            me.data.remove(item => item.id === id);
            me.dom.triggerHandler('valueChange', [me]);
        }
        ;
        me.versionCard.load();
    },
    /**
     * 注册值变更事件
     */
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        let toolbar = me.dom.find('.toolbar');
        if (readonly) {
            toolbar.find('.btn-group').hide();
            toolbar.find('[auth=read]').parent('.btn-group').show();
        } else {
            toolbar.find('.btn-group').show();
        }
    },
    setValue(values) {
        let me = this;
        me.values = values || [];
        delete me.data;
        me.delete = [];
        me.create = [];
        me.update = [];
        if (me.versionCard) {
            me.versionCard.selected = [];
            me.versionCard.load();
        }
    },
    getDirtyValue() {
        let me = this;
        let dirty = [];
        for (let i = 0; i < me.create.length; i++) {
            let values = {};
            $.extend(values, me.create[i]);
            delete values.id;
            dirty.push([0, 0, values]);
        }
        for (let i = 0; i < me.update.length; i++) {
            let values = {};
            $.extend(values, me.update[i]);
            let id = values.id;
            delete values.id;
            dirty.push([1, id, values]);
        }
        for (let i = 0; i < me.delete.length; i++) {
            dirty.push([2, me.delete[i], 0]);
        }
        return dirty;
    },
    /**
     * 获取数据
     */
    getValue() {
        let me = this;
        if (me.versionCard) {
            return me.versionCard.data;
        }
        return [];
    },
});
