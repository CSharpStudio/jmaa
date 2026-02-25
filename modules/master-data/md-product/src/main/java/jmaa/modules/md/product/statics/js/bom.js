//@ sourceURL=bom.js
jmaa.view({});
jmaa.editor('bom-version', {
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
                        <div class="bom-version m-0"></div>
                    </aside>
                    <div class="bom-details"></div>
                </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).addClass("bom-version d-grid");
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
                me.versionCard = me.dom.find('.bom-version').JCard({
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
                            me.detailsGrid && me.detailsGrid.load();
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
                });
                let auths = 'read';
                for (const auth of me.view.auths) {
                    if (auth[me.field.comodel]) {
                        auths = auth[me.field.comodel];
                        auths.push('read');
                        break;
                    }
                }
                me.toolbar = new JToolbar({
                    dom: me.dom.find('.version-toolbar'),
                    arch: me.versionCard.tbarArch,
                    auths,
                    defaultButtons: 'create|edit|delete|import',
                    buttonsTpl: me.buttonsTpl,
                    target: me.versionCard,
                    view: me.view,
                });
            },
        });
    },
    searchVersion(card, callback) {
        let me = this;
        if (!me.owner.dataId) {
            callback({data: []});
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: me.field.name,
                criteria: [["bom_id", "=", me.owner.dataId]],
                fields: card.getFields(),
                limit: 1000,
                order: card.getSort(),
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
        let values = [];
        data = data || dirty;
        if (data.id) {
            values.push([1, data.id, dirty]);
        } else {
            values.push([0, 0, dirty]);
        }
        await me.view.rpc(me.model, "update", {
            ids: [me.owner.dataId],
            values: {version_ids: values}
        });
        me.versionCard.load();
    },
    async deleteVersion(card, ids) {
        let me = this;
        let values = [];
        for (let id of ids) {
            values.push([2, id, 0]);
        }
        await me.view.rpc(me.model, "update", {
            ids: [me.owner.dataId],
            values: {version_ids: values}
        });
        me.versionCard.load();
    },
    loadDetails(grid, callback) {
        let me = this;
        if (!me.versionCard) {
            callback({data: []});
            return;
        }
        jmaa.rpc({
            model: 'md.bom_version',
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: 'details_ids',
                criteria: [["version_id", "=", me.versionCard.getSelected()[0]]],
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
    initDetails(arch) {
        let me = this;
        jmaa.rpc({
            model: 'ir.ui.view',
            method: 'loadFields',
            args: {
                model: 'md.bom_details',
            },
            onsuccess(r) {
                me.detailsGrid = me.dom.find('.bom-details').JGrid({
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
        if (me.versionCard) {
            me.versionCard.selected = [];
            me.versionCard.load();
        }
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
