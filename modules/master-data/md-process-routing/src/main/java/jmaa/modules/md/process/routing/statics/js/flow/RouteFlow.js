//@ sourceURL=RouteFlow.js
jmaa.component('RouteFlow', {
    extends: ['JComponent', 'RouteDiagram'],
    getTpl() {
        return `<div class="content-header">
                    <div class="toolbar main-toolbar">
                        <div class="btn-group">
                            <button class="btn btn-back" type="button" >${'返回'.t()}</button>
                        </div>
                    </div>
                    <div class="title"></div>
                </div>
                <div class="content">
                    <aside class="left-aside border-right">
                        <div class="toolbar version-toolbar"></div>
                        <div class="route-version m-0"></div>
                    </aside>
                    <div class="route-details">
                        <div class="toolbar details-toolbar">
                            <button class="btn btn-default btn-flat btn-reload">${"刷新".t()}</button>
                            <button disabled class="btn btn-default btn-flat btn-undo">${"撤销".t()}</button>
                            <button disabled class="btn btn-default btn-flat btn-redo">${"恢复".t()}</button>
                            <button class="btn btn-blue btn-flat btn-release">${"发布".t()}</button>
                        </div>
                        <div class="route-diagram"></div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).addClass('route-flow').on('click', '.btn-back', function () {
            me.view.changeView(me.backView);
        }).on('click', '.btn-undo', function (e) {
            me.flowDiagram.undo();
        }).on('click', '.btn-reload', function (e) {
            me.loadFlow(me.flowDiagram.dataId);
        }).on('click', '.btn-redo', function (e) {
            me.flowDiagram.redo();
        }).on('click', '.btn-release', function (e) {
            me.releaseVersion();
        });
        me.initVersion();
        me.initDiagram();
    },
    initVersion() {
        let me = this;
        me.view.loadView("pr.route_version", "card").then(v => {
            me.versionCard = me.dom.find('.route-version').JCard({
                model: v.model,
                module: v.module,
                arch: v.views.card.arch,
                fields: v.fields,
                itemClass: 'version-item',
                editable: true,
                view: me.view,
                on: {
                    load() {
                        me.flowDiagram.dataId = null;
                        me.flowDiagram.unload();
                        me.dom.find('.details-toolbar .btn').hide();
                    },
                    editFormInit(e, card, form, dialog) {
                        let btn = dialog.dom.find('[role=btn-submit-close]');
                        if (btn.length) {
                            btn.html('保存'.t());
                            dialog.dom.find('[role=btn-submit]').remove();
                        } else {
                            dialog.dom.find('[role=btn-submit]').html('保存'.t());
                        }
                    },
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
                        if (me.flowDiagram.dataId != ids[0]) {
                            me.loadFlow(ids[0]);
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
                    me.versionCard.load();
                }
            });
            me.toolbar = new JToolbar({
                dom: me.dom.find('.version-toolbar'),
                arch: me.versionCard.tbarArch,
                auths: v.auths,
                defaultButtons: 'create|edit|delete',
                target: me.versionCard,
                view: me.view,
            });
            if (!v.auths['publish']) {
                me.dom.find('btn-release').remove();
            }
        });
    },
    loadFlow(versionId, callback) {
        if (!versionId) {
            return;
        }
        let me = this;
        me.dropmenu = null;
        me.flowDiagram.readonly(false);
        me.flowDiagram.dataId = versionId;
        me.view.rpc('pr.route_version', 'loadFlow', {
            ids: [me.flowDiagram.dataId],
            fields: ['type', 'label', 'parent_id', 'child_ids', 'present', 'process_type', 'collection_result',
                'is_optional', 'is_output', 'create_task', 'enable_move_in', 'is_repeatable', 'is_deduction', 'to_fqc',
                'x', 'y', 'ok_id', 'ng_id', 'is_start', 'is_end'],
        }, {
            active_test: true,
        }).then(r => {
            if (r.is_publish) {
                me.dom.find('.details-toolbar .btn:not(.btn-reload)').hide();
                me.dom.find('.details-toolbar .btn-reload').show();
            } else {
                me.dom.find('.details-toolbar .btn').show();
            }
            me.flowDiagram.updateSize({width: r.canvas_width, height: r.canvas_height});
            let nodes = [];
            let parents = {};
            let children = [];
            for (let n of r.nodes) {
                if (!n.parent_id) {
                    n.children = [];
                    nodes.push(n);
                    parents[n.id] = n;
                } else {
                    children.push(n);
                }
            }
            for (let n of children) {
                let p = parents[n.parent_id];
                p && p.children.push(n);
            }
            me.flowDiagram.load(nodes);
            me.flowDiagram.readonly(r.is_publish);
            callback && callback();
        });
    },
    searchVersion(card, callback) {
        let me = this;
        if (!me.dataId) {
            callback({data: []});
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                relatedField: 'version_ids',
                criteria: [["route_id", "=", me.dataId]],
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
                card.selected = [];
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
    async saveVersion(data) {
        let me = this;
        let id = data.id;
        delete data.id;
        let result;
        if (id) {
            result = await me.view.rpc('pr.route_version', 'update', {
                ids: [id],
                values: data,
            });
        } else {
            data.route_id = me.dataId;
            result = await me.view.rpc('pr.route_version', 'create', data);
        }
        if (!id) {
            me.versionCard.selected = [result];
        }
        me.versionCard.load();
    },
    async deleteVersion(card, ids) {
        let me = this;
        await me.view.rpc('pr.route_version', "delete", {
            ids: ids
        });
        me.versionCard.selected = [];
        me.versionCard.load();
    },
    releaseVersion() {
        let me = this;
        if (me.flowDiagram.dataId) {
            jmaa.msg.confirm({
                content: '发布后不能再修改，确定发布？'.t(),
                submit() {
                    me.view.rpc('pr.route_version', 'publish', {
                        ids: [me.flowDiagram.dataId]
                    }).then(r => {
                        me.versionCard.load();
                    });
                }
            });
        }
    },
    processFilter() {
        let me = this;
        return ["|", ['product_family_id', '=', me.family_id], ['product_family_id', '=', null]];
    },
    load() {
        let me = this;
        me.versionCard && me.versionCard.load();
        me.view.rpc(me.model, 'read', {
            ids: [me.dataId],
            fields: ['present', 'family_id'],
        }).then(r => {
            me.family_id = r[0].family_id;
            me.dom.find('.title').html(r[0].present);
        });
    }
})
