//@ sourceURL=route_version.js
jmaa.editor('route-editor', {
    extends: ['editors.Editor', 'RouteDiagram'],
    routeModel: 'pr.route',
    nodeModel: 'pr.route_node',
    getTpl() {
        let me = this;
        return `<div class="content route-flow">
                    <div class="route-details">
                        <div class="toolbar details-toolbar">
                            <button type="button" class="btn btn-default btn-flat btn-reload">${"刷新".t()}</button>
                            <button disabled class="btn btn-default btn-flat btn-undo">${"撤销".t()}</button>
                            <button disabled class="btn btn-default btn-flat btn-redo">${"恢复".t()}</button>
                        </div>
                        <div class="route-diagram"></div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl()).css('height', '600px').on('click', '.btn-undo', function (e) {
            me.flowDiagram.undo();
        }).on('click', '.btn-reload', function (e) {
            me.loadFlow();
        }).on('click', '.btn-redo', function (e) {
            me.flowDiagram.redo();
        });
        me.initDiagram();
    },
    loadFlow(id, callback) {
        let me = this;
        if (!me.owner.dataId) {
            return;
        }
        me.dropmenu = null;
        me.flowDiagram.readonly(false);
        me.view.rpc(me.owner.model, 'loadFlow', {
            ids: [me.owner.dataId],
            fields: ['type', 'label', 'parent_id', 'child_ids', 'present', 'process_type', 'collection_result', 'x', 'y', 'ok_id', 'ng_id', 'is_start', 'is_end'],
        }, {
            active_test: true,
        }).then(r => {
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
            me.flowDiagram.dataId = r.id;
            if (!r.editable) {
                me.dom.find('.details-toolbar .btn:not(.btn-reload)').hide();
                me.dom.find('.details-toolbar .btn-reload').show();
            } else {
                me.dom.find('.details-toolbar .btn').show();
            }
            me.flowDiagram.readonly(!r.editable);
            me.family_id = r.family_id;
            callback && callback();
        });
    },
    updateFlow() {
        let me = this;
        me.flowDiagram.items.each(function () {
            let item = this;
            for (let c of item.connectors) {
                c.update();
            }
        });
        me.flowDiagram.aerialView.updateView();
        me.flowDiagram.aerialView.load();
    },
    setValue(value) {
        let me = this;
        if (value) {
            me.loadFlow();
        } else {
            me.flowDiagram.dataId = null;
            me.flowDiagram.unload();
            me.dom.find('.details-toolbar .btn').hide();
        }
    },
    processFilter() {
        let me = this;
        return ["|", ['product_family_id', '=', me.family_id], ['product_family_id', '=', null]];
    },
});
