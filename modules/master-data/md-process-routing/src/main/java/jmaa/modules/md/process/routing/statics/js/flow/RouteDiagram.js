//@ sourceURL=RouteDiagram.js
jmaa.define('RouteDiagram', {
    routeModel: 'pr.route_version',
    nodeModel: 'pr.route_node',
    initDiagram() {
        let me = this;
        me.flowDiagram = new JDiagram({
            dom: me.dom.find(".route-diagram"),
            toolItems: [{tooltip: '拖动添加工序', label: '工序', type: 'process'},
                {tooltip: '拖动添加工序组', label: '工序组', type: 'group'},
                /* {tooltip: '拖动添加网关', label: '网关', type: 'gateway'}*/],
            on: {
                memoChange(e, m) {
                    me.dom.find('.btn-redo').attr('disabled', !m.canRedo());
                    me.dom.find('.btn-undo').attr('disabled', !m.canUndo());
                },
            },
            createItem(data, undo) {
                return me.createNode(data, undo);
            },
            moveItems(param) {
                return me.moveNodes(param);
            },
            removeItems(items, undo) {
                if (undo) {
                    me.deleteNodes(items);
                } else {
                    return me.confirmDelete(items);
                }
            },
            updateConnectors(items) {
                me.updateConnectors(items);
            }
        });
        me.flowDiagram.dom.on('click', '.item-menu', function () {
            let menu = $(this);
            me.openMenu(menu);
        }).on('mousedown', function (e) {
            if (me.dropmenu && $(e.target).closest('.dropdown-menu').length == 0) {
                me.dropmenu.hide();
            }
        }).on('click', '.menu-delete', function () {
            let menu = $(this);
            me.deleteMenuClick(menu);
        }).on('click', '.menu-edit', function () {
            me.editNode($(this).parent().attr('id'));
            me.dropmenu.hide();
        }).on('click', '.btn-add', function () {
            let id = $(this).closest('.item-group').attr('id');
            me.addProcess(id);
        }).on('dblclick', '.group-item', function () {
            let id = $(this).attr('id');
            me.editNode(id);
        });
    },
    deleteMenuClick(menu) {
        let me = this;
        let id = menu.parent().attr('id');
        let parentId = menu.parent().attr('pid');
        if (!parentId) {
            me.flowDiagram.deleteItems(me.flowDiagram.items.get(id));
        } else {
            let item = me.flowDiagram.items.get(parentId);
            let msg = item.dom.find('#' + id).text();
            jmaa.msg.confirm({
                content: "确定删除{0}吗?".t().formatArgs(msg),
                submit() {
                    let data = item.data.children.find(i => i.id == id);
                    me.deleteChildProcess(data);
                },
            });
        }
        me.dropmenu.hide();
    },
    confirmDelete(items) {
        let me = this;
        return new Promise((resolve, reject) => {
            items = items.filter(item => item.id != 'start' && item.id != 'end');
            if (items.length) {
                let msg = items.length == 1 ? "[" + items[0].dom.find('.title').text() + "]"
                    : '选中的 {0} 个工序'.t().formatArgs(items.length);
                jmaa.msg.confirm({
                    content: "确定删除{0}吗?".t().formatArgs(msg),
                    submit() {
                        me.deleteNodes(items, function (values) {
                            resolve(values);
                        });
                    },
                });
            }
        });
    },
    openMenu(menu) {
        let me = this;
        let item = menu.closest('.diagram-item');
        let group = menu.closest('.group-item');
        let id = group.length ? group.attr('id') : item.attr('id');
        let z = me.flowDiagram.zoom;
        if (!me.dropmenu) {
            me.dropmenu = $(`<div class="dropdown-menu">
                    <span role="button" class="menu-edit dropdown-item">${'编辑'.t()}</span>
                    <span role="button" class="menu-delete dropdown-item">${'删除'.t()}</span>
                </div>`);
            me.flowDiagram.canvas.append(me.dropmenu);
        }
        let itemPosition = item.position();
        let menuPosition = menu.position();
        let groupPosition = group.position() || {top: 0};
        me.dropmenu.attr('id', id).attr('pid', group.attr('pid') || '').css({
            left: itemPosition.left + menuPosition.left,
            top: itemPosition.top + menuPosition.top + groupPosition.top + menu.height() * z / 100,
            zoom: 100 / z
        }).show();
    },
    addProcess(parentId) {
        let me = this;
        jmaa.showDialog({
            title: '添加工序'.t(),
            init(dialog) {
                me.view.loadView('pr.route_node', "form").then(v => {
                    dialog.form = dialog.body.JForm({
                        model: me.nodeModel,
                        module: v.module,
                        fields: v.fields,
                        arch: v.views.form.arch,
                        view: me
                    });
                    dialog.form.create();
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let values = dialog.form.getSubmitData();
                values.parent_id = parentId;
                me.view.rpc(me.routeModel, 'createNode', {
                    ids: [me.flowDiagram.dataId],
                    values,
                    fields: ["present", "process_type", "parent_id"]
                }).then(r => {
                    me.addChildProcess(r);
                    dialog.close();
                    me.flowDiagram.dom.focus();
                });
            },
            cancel() {
                me.flowDiagram.dom.focus();
            }
        });
    },
    addChildProcess(data, undo) {
        let me = this;
        let diagram = me.flowDiagram;
        let item = diagram.items.get(data.parent_id);
        item.data.children.push(data);
        item.dom.find('.process-items').html($(item.getTpl()).find('.process-items'));
        if (!undo) {
            diagram.cmdMemo.add({
                undo() {
                    me.deleteChildProcess(data, true);
                },
                redo() {
                    let values = [[1, data.id, {active: true}]];
                    me.updateNode({node_ids: values}, function () {
                        me.addChildProcess(data, true);
                    });
                }
            });
        }
    },
    deleteChildProcess(data, undo) {
        let me = this;
        let diagram = me.flowDiagram;
        let item = diagram.items.get(data.parent_id);
        let values = [[1, data.id, {active: false}]];
        me.updateNode({node_ids: values}, function () {
            item.data.children.remove(i => i.id == data.id);
            item.dom.find('.process-items').html($(item.getTpl()).find('.process-items'));
        });
        if (!undo) {
            diagram.cmdMemo.add({
                undo() {
                    let values = [[1, data.id, {active: true}]];
                    me.updateNode({node_ids: values}, function () {
                        me.addChildProcess(data, true);
                    });
                },
                redo() {
                    me.deleteChildProcess(data, true);
                }
            });
        }
    },
    updateConnectors(items) {
        let me = this;
        for (let item of items) {
            if (item.data.ok_id) {
                me.connect(item.id, item.data.ok_id, 'ok');
            }
            if (item.data.ng_id) {
                me.connect(item.id, item.data.ng_id, 'ng');
            }
            if (item.data.is_start) {
                me.connect('start', item.id, 'ok');
            }
            if (item.data.is_end) {
                me.connect(item.id, 'end', 'ok');
            }
        }
    },
    moveNodes(param) {
        let me = this;
        let nodes = [];
        let values = {node_ids: nodes};
        let items = [];
        for (let item of param.items) {
            let point = {x: Math.max(0, item.x + param.dx), y: Math.max(0, item.y + param.dy)};
            point.x = Math.round(point.x / 20) * 20;
            point.y = Math.round(point.y / 20) * 20;
            if (item.x != point.x || item.y != point.y) {
                items.push(item);
                item._to = point;
                if (item.id == 'start') {
                    values.start_x = point.x;
                    values.start_y = point.y;
                } else if (item.id == 'end') {
                    values.end_x = point.x;
                    values.end_y = point.y;
                } else {
                    nodes.push([1, item.id, point]);
                }

            }
        }
        if (!items.length) {
            return items;
        }
        return new Promise(resolve => {
            me.updateNode(values, function () {
                for (let item of param.items) {
                    item.moveTo(item._to);
                    delete item._to;
                    me.flowDiagram.aerialView.update(item.id);
                }
                resolve(items);
            });
        });
    },
    initNode(data) {
        let me = this;
        let item = jmaa.create('RouteNode', {
            id: data.id,
            data,
            css: me.itemCss,
            label: data.present,
            x: Math.round(data.x / 20) * 20,
            y: Math.round(data.y / 20) * 20,
        });
        let diagram = me.flowDiagram;
        item.dom.find('.output').draggable({
            scroll: false,
            opacity: 0.8,
            zIndex: 9999,
            containment: 'window',
            helper: function (e) {
                let el = $(e.currentTarget);
                return $(`<i item="${el.parent().attr('id')}" type="${el.attr('type')}" class="output dragging"/>`);
            },
            start: function (e, ui) {
                if (diagram.readonly()) {
                    return false;
                }
                let z = diagram.zoom;
                let p = ui.helper.offsetParent().position();
                let x = (ui.position.left + p.left) * 100 / z;
                let y = (ui.position.top + p.top) * 100 / z;
                let id = ui.helper.attr('item');
                let type = ui.helper.attr('type');
                let conn = diagram.items.get(id).connectors.find(c => c.id == id + '-' + type);
                conn && conn.svg.addClass("editing");
                diagram.canvas.append(`<svg class="connecting ${type}" style="z-index: 1000" width="100%" height="100%">
                        <marker id="arrow" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                            <path d="M 2 0 L 10 5 L 2 10" stroke-width="1" fill="none" />
                        </marker>
                        <line x1="${x + 2}" y1="${y + 10}" x2="100%" y2="100%" stroke-width="2" marker-end="url(#arrow)"/>
                    </svg>`);
            },
            drag: function (e, ui) {
                let parent = ui.helper.offsetParent().position();
                let p = ui.position;
                let z = diagram.zoom;
                p.left = p.left * 100 / z;
                p.top = p.top * 100 / z;
                let line = diagram.canvas.find('svg.connecting line');
                let x = parent.left * 100 / z + p.left;
                let y = parent.top * 100 / z + p.top;
                line.attr('x2', x + 2).attr('y2', y + 10);
            },
            stop: function (e, ui) {
                diagram.canvas.find('svg.connecting').remove();
                diagram.canvas.find('.diagram-connector.editing').removeClass("editing");
            }
        });
        item.dom.on('dblclick', function (e) {
            if (!$(e.target).closest('.process-items').length) {
                me.editNode(item.id);
            }
        }).droppable({
            accept: ".output",
            over(e, ui) {
                let from = ui.helper.attr('item');
                let type = ui.helper.attr('type');
                if (from == item.id || item.id == 'start' || from == 'start' && item.id == 'end' || item.id == 'end' && type != 'ok') {
                    item.dom.removeClass('ui-droppable-hover');
                }
            },
            drop: function (e, ui) {
                let id = ui.helper.attr('item');
                let type = ui.helper.attr('type');
                if (id == item.id || item.id == 'start' || id == 'start' && item.id == 'end' || item.id == 'end' && type != 'ok') {
                    return;
                }
                me.connectNode(id, type, item.id);
            }
        });
        return item;
    },
    editNode(id) {
        let me = this;
        if (id == 'start' || id == 'end') {
            return;
        }
        let item = me.flowDiagram.items.get(id);
        let type = item ? item.data.type : 'process';
        let css = type == 'process' ? "modal-xl" : "modal-sm";
        jmaa.showDialog({
            title: '编辑'.t(),
            css,
            submitText: '保存'.t(),
            init(dialog) {
                let render = view => {
                    let arch = view.views.form.arch;
                    if (type == 'group') {
                        arch = `<form cols="1" logAccess="0">
                                    <field name="label" required="1"></field>
                                    <field name="group_options" min="0"></field>
                                </form>`;
                    } else if (type == 'gateway') {
                        arch = `<form cols="1" logAccess="0">
                                    <field name="label" required="1"></field>
                                    <field name="gateway_expr" editor="text" required="1"></field>
                                </form>`;
                    }
                    dialog.form = dialog.body.JForm({
                        model: me.nodeModel,
                        module: view.module,
                        view: me,
                        fields: view.fields,
                        arch,
                    });
                    me.view.rpc(me.nodeModel, 'read', {
                        ids: [id],
                        fields: dialog.form.getFields(),
                    }, {usePresent: true}).then(r => {
                        dialog.form.setData(r[0]);
                        if (me.flowDiagram.readonly()) {
                            dialog.dom.find("[role=btn-submit]").hide();
                            dialog.form.setReadonly(true);
                        }
                    });
                };
                if (me.editNodeView) {
                    render(me.editNodeView);
                }
                me.view.loadView('pr.route_node', 'form').then(v => {
                    me.editNodeView = v;
                    render(v);
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                dialog.busy(true);
                let data = dialog.form.getSubmitData();
                me.updateNode({node_ids: [[1, id, data]]}, function () {
                    dialog.close();
                    me.loadFlow(me.flowDiagram.dataId, function () {
                        me.flowDiagram.items.get(id).select();
                    });
                }, function () {
                    dialog.busy(false);
                });
            },
            cancel(dialog) {
                me.flowDiagram.dom.focus();
            }
        });
    },
    connectNode(fromId, type, toId, undo) {
        let me = this;
        let diagram = me.flowDiagram;
        let from = diagram.items.get(fromId);
        let connId = fromId + '-' + type;
        let conn = from.connectors.find(c => c.id == connId);
        let oldTo = conn ? conn.to.attr('id') : null;
        if (oldTo == toId) {
            return;
        }
        let to = diagram.items.get(toId);
        let values = [];
        let undoValues = [];
        let undoAction = [];
        if (fromId == 'start') {
            values.push([1, toId, {is_start: true}]);
            undoValues.push([1, toId, {is_start: false}]);
            if (oldTo) {
                values.push([1, oldTo, {is_start: false}]);
                undoValues.push([1, oldTo, {is_start: true}]);
            }
            undoAction.push(() => {
                let item = diagram.items.get(fromId);
                let conn = item.connectors.find(c => c.id == connId);
                conn && conn.remove();
                if (oldTo) {
                    me.connect(fromId, oldTo, type);
                }
            });
        } else if (toId == 'end') {
            values.push([1, fromId, {is_end: true, ok_id: null}]);
            undoValues.push([1, fromId, {is_end: false}]);
            if (oldTo) {
                undoValues.push([1, fromId, {ok_id: oldTo}]);
            }
            undoAction.push(() => {
                let item = diagram.items.get(fromId);
                item.connectors.find(c => c.id == connId).remove();
                if (oldTo) {
                    me.connect(fromId, oldTo, type);
                }
            });
        } else {
            if (type == 'ok') {
                values.push([1, fromId, {ok_id: toId}]);
                if (oldTo == 'end') {
                    values.push([1, fromId, {is_end: false}]);
                    undoValues.push([1, fromId, {is_end: true}]);
                } else {
                    undoValues.push([1, fromId, {ok_id: oldTo}]);
                }
                undoAction.push(() => {
                    let item = diagram.items.get(fromId);
                    item.connectors.find(c => c.id == connId).remove();
                    if (oldTo == 'end') {
                        me.connect(fromId, 'end', type);
                    } else if (oldTo) {
                        me.connect(fromId, oldTo, type);
                    }
                });
            } else {
                values.push([1, fromId, {ng_id: toId}]);
                undoValues.push([1, fromId, {ng_id: oldTo}]);
                undoAction.push(() => {
                    let item = diagram.items.get(fromId);
                    item.connectors.find(c => c.id == connId).remove();
                    if (oldTo) {
                        me.connect(fromId, oldTo, type);
                    }
                });
            }
        }
        me.updateNode({node_ids: values}, function () {
            let conn = from.connectors.find(c => c.id == fromId + '-' + type);
            conn && conn.remove();
            me.connect(fromId, to.id, type);
        });
        if (!undo) {
            diagram.cmdMemo.add({
                undo() {
                    me.updateNode({node_ids: undoValues}, function () {
                        for (let action of undoAction) {
                            action();
                        }
                    });
                },
                redo() {
                    me.connectNode(fromId, type, toId, true);
                }
            });
        }
    },
    updateNode(values, callback, error) {
        let me = this;
        jmaa.rpc({
            model: me.routeModel,
            module: me.module,
            method: 'update',
            args: {
                ids: [me.flowDiagram.dataId],
                values,
            },
            onsuccess(r) {
                callback && callback(r);
            },
            onerror(r) {
                jmaa.msg.error(r);
                error && error();
            }
        });
    },
    createNode(data, undo) {
        let me = this;
        if (data.id) {
            if (undo) {
                return new Promise(resolve => {
                    let values = [[1, data.id, {active: true}]];
                    let toUpdate = [];
                    if (data.connectors) {
                        for (let c of data.connectors) {
                            if (c.to == data.id) {
                                let value = {};
                                value[c.type + "_id"] = data.id;
                                values.push([1, c.from, value]);
                                toUpdate.push(me.flowDiagram.items.get(c.from));
                            }
                        }
                    }
                    me.updateNode({node_ids: values}, function () {
                        let item = me.initNode(data);
                        item.related = toUpdate;
                        resolve(item);
                    });
                });
            }
            return me.initNode(data);
        }
        return new Promise((resolve, reject) => {
            if (data.type == 'group' || data.type == 'gateway') {
                let values = {
                    x: data.x,
                    y: data.y,
                    type: data.type,
                };
                if (data.type == 'gateway') {
                    values.label = '网关'.t();
                } else if (data.type == 'group') {
                    values.label = '工序组'.t();
                }
                me.view.rpc(me.routeModel, 'createNode', {
                    ids: [me.flowDiagram.dataId],
                    values,
                    fields: ["label", "type", "x", "y"]
                }).then(r => {
                    r.children = [];
                    resolve(me.initNode(r));
                    dialog.close();
                    me.flowDiagram.dom.focus();
                }).catch(r => reject(r));
            } else {
                jmaa.showDialog({
                    title: '添加工序'.t(),
                    init(dialog) {
                        me.view.loadView('pr.route_node', "form").then(v => {
                            dialog.form = dialog.body.JForm({
                                model: me.nodeModel,
                                module: v.module,
                                fields: v.fields,
                                arch: v.views.form.arch,
                                view: me
                            });
                            dialog.form.create();
                        });
                    },
                    submit(dialog) {
                        if (!dialog.form.valid()) {
                            return jmaa.msg.error(dialog.form.getErrors());
                        }
                        let values = dialog.form.getSubmitData();
                        values.x = data.x;
                        values.y = data.y;
                        me.view.rpc(me.routeModel, 'createNode', {
                            ids: [me.flowDiagram.dataId],
                            values,
                            fields: ["present", "process_type", "collection_result", "type", "x", "y"]
                        }).then(r => {
                            resolve(me.initNode(r));
                            dialog.close();
                            me.flowDiagram.dom.focus();
                        }).catch(r => reject(r));
                    },
                    cancel() {
                        me.flowDiagram.dom.focus();
                    }
                });
            }
        });
    },
    deleteNodes(items, callback) {
        let me = this;
        let values = [];
        let list = [];
        for (let item of items) {
            let data = item.getData();
            data.connectors = [];
            list.push(data);
            for (let c of item.connectors) {
                data.connectors.push(c.data);
                if (c.data.to == item.id && c.data.from != 'start') {
                    let value = c.data.type == 'ok' ? {'ok_id': null} : {'ng_id': null};
                    values.push([1, c.data.from, value]);
                }
            }
            values.push([1, item.id, {active: false}]);
        }
        me.updateNode({node_ids: values}, function () {
            for (let item of items) {
                me.flowDiagram.removeItem(item);
            }
            callback && callback(list);
        });
    },
    connect(fromId, toId, type) {
        let me = this;
        let diagram = me.flowDiagram;
        let to = diagram.items.get(toId);
        to && jmaa.create('Connector', {
            id: fromId + "-" + type,
            data: {from: fromId, to: toId, type},
            from: diagram.items.get(fromId).dom.find(`.output[type=${type}]`),
            to: to.dom,
            css: type,
            diagram,
        });
    }
});
