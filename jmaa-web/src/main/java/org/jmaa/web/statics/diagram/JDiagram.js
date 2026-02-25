//@ sourceURL=JDiagram.js
jmaa.component("JDiagram", {
    width: 2000,
    height: 2000,
    toolItems: [],
    itemDragOptions: {},
    itemCss: 'diagram-item',
    ajax(diagram, callback) {
    },
    getTpl: function () {
        let me = this;
        let items = [];
        for (let item of this.toolItems) {
            items.push(me.createToolItem(item));
        }
        return `<div class="jui-diagram readonly" tabindex="-1">
                    <div class="tool-panel">
                        ${items.join('')}
                    </div >
                    <div title="${"缩放".t()}" class="zoom-slider">
                        <div></div>
                    </div>
                    <div class="aerial-view"></div>
                    <div class="drag-container">
                        <div class="diagram-panel">
                            <div class="diagram-canvas container-fluid">
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    init: function () {
        let me = this;
        let dom = me.dom.append(me.getTpl());
        me.items = jmaa.create("KeyValue");
        me.canvas = dom.find('.diagram-canvas').height(me.height).width(me.width);
        me.cmdMemo = jmaa.create('CommandMemo', {
            change: function (memo) {
                dom.triggerHandler('memoChange', [memo]);
            }
        });
        me.initZoom();
        me.initSelect();
        me.initTools();
        me.aerialView = new AerialView({dom: dom.find('.aerial-view'), diagram: me});
    },
    initZoom() {
        let me = this;
        me.zoom = 100;
        me.zoomSlider = me.dom.find('.zoom-slider div').bootstrapSlider({
            formatter: function (value) {
                if (value >= 5) {
                    return value / 5 * 100 + '%';
                }
                return value * 15 + 25 + '%';
            },
            tooltip_position: 'left',
        });
        me.zoomSlider.on("change", function (e) {
            let value = e.value.newValue;
            if (value >= 5) {
                me.zoom = value / 5 * 100;
            }
            me.zoom = value * 15 + 25;
            me.canvas.css("zoom", me.zoom + "%");
            me.dom.triggerHandler('zoom', [me]);
        });
    },
    initSelect() {
        let me = this;
        me.dom.on('click', `.${me.itemCss}`, function (e) {
            let id = $(this).attr('id');
            let item = me.items.get(id);
            if (!e.ctrlKey) {
                me.items.each(function () {
                    this.unselect();
                });
                item.select();
            } else {
                if (item.selected) {
                    item.unselect();
                } else {
                    item.select();
                }
            }
            me.dom.find('.jui-diagram').focus();
        }).on('keydown', function (e) {
            if (e.ctrlKey && e.keyCode == 65) {
                me.selectItems();
                e.stopPropagation();
                e.cancelBubble = true;
                e.preventDefault();
            } else if (e.keyCode == 46) {
                let items = me.items.find(i => i.selected);
                me.deleteItems(items);
            }
        });
    },
    initTools() {
        let me = this;
        me.tools = new ToolItem({
            dom: me.dom.find('.tool-item'),
            diagram: me,
            containment: me.dom.find('.diagram-canvas'),
            createItem(point) {
                me._createItem(point);
            },
        });
    },
    createToolItem(item) {
        return `<div class="tool-item" type="${item.type || ''}">
                    ${item.label.t()}
                    <div class="tooltip  bs-tooltip-right">
                        <div class="arrow"></div>
                        <div class="tooltip-inner">${item.tooltip || item.label.t()}</div>
                    </div>
                </div>`
    },
    undo() {
        this.cmdMemo.undo();
    },
    redo() {
        this.cmdMemo.redo();
    },
    updateSize: function (size) {
        let me = this;
        me.canvas.width(size.width).height(size.height);
        me.aerialView.updateView();
    },
    onZoom: function (handler) {
        this.dom.on('zoom', handler);
    },
    selectItems: function () {
        this.items.each(function () {
            this.select();
        });
    },
    unselectItems: function () {
        this.items.each(function () {
            this.unselect();
        });
    },
    _createItem(data, undo) {
        let me = this;
        let item = me.createItem(data, undo);
        let callback = function (item) {
            if (!undo) {
                let clone = item.getData();
                me.cmdMemo.add({
                    undo() {
                        me.deleteItems(me.items.get(clone.id), true);
                    },
                    redo() {
                        me._createItem(clone, true);
                    }
                });
            }
            me.drawItem(item);
            me.unselectItems();
            item.select();
            if (undo) {
                let toUpdate = item.related || [];
                toUpdate.push(item);
                me.updateConnectors(toUpdate);
            }
        }
        if (item instanceof Promise) {
            item.then(r => callback(r));
        } else {
            callback(item);
        }
    },
    createItem(data) {
        let me = this;
        return jmaa.create('DiagramItem', {
            id: data.id || jmaa.nextId(),
            data,
            css: me.itemCss,
            label: data.label,
            x: data.x,
            y: data.y
        });
    },
    deleteItems(items, undo) {
        let me = this;
        if (!Array.isArray(items)) {
            items = [items];
        }
        let values = me.removeItems(items, undo);
        if (!undo) {
            let callback = function (values) {
                if (values.length) {
                    me.cmdMemo.add({
                        undo() {
                            for (let value of values) {
                                me._createItem(value, true);
                            }
                        },
                        redo() {
                            for (let value of values) {
                                let item = me.items.get(value.id);
                                me.deleteItems(item, true);
                            }
                        }
                    });
                }
            }
            if (values instanceof Promise) {
                values.then(r => callback(r));
            } else {
                callback(values);
            }
        }
    },
    removeItems(items, undo) {
        let me = this;
        let values = [];
        for (let item of items) {
            let value = item.getData();
            value.connectors = [];
            for (let c of item.connectors) {
                value.connectors.push(c.data);
            }
            values.push(value);
            me.removeItem(item);
        }
        return values;
    },
    removeItem(item) {
        let me = this;
        while (item.connectors.length) {
            item.connectors[0].remove();
        }
        item.dom.remove();
        me.items.remove(item.id);
        me.aerialView.remove(item.id);
    },
    moveItems(param) {
        let me = this;
        for (let item of param.items) {
            item.moveTo({x: Math.max(0, item.x + param.dx), y: Math.max(0, item.y + param.dy)});
            me.aerialView.update(item.id);
        }
        return param.items;
    },
    _moveItems(param, undo) {
        let me = this;
        let items = me.moveItems(param);
        if (!undo) {
            let callback = function (items) {
                if (items.length) {
                    let ids = [];
                    for (let item of items) {
                        ids.push(item.id);
                    }
                    let p = {dx: param.dx, dy: param.dy, ids};
                    me.cmdMemo.add({
                        undo() {
                            let items = [];
                            for (let id of ids) {
                                let item = me.items.get(id);
                                item.select();
                                items.push(item);
                            }
                            me._moveItems({items, dx: -p.dx, dy: -p.dy}, true);
                        },
                        redo() {
                            let items = [];
                            for (let id of ids) {
                                let item = me.items.get(id);
                                item.select();
                                items.push(item);
                            }
                            me._moveItems({items, dx: p.dx, dy: p.dy}, true);
                        }
                    });
                }
            };
            if (items instanceof Promise) {
                items.then(r => callback(r));
            } else {
                callback(items);
            }
        }
    },
    createDragHandler(e) {
        let me = this;
        let current = $(e.currentTarget);
        let id = current.attr('id');
        let handler = $(`<div style="z-index: 9999" data-id="${id}"></div>`);
        let position = current.position();
        if (!current.hasClass('selected')) {
            me.unselectItems();
            let item = me.items.get(id);
            item.select();
        }
        let selected = $(`.${me.itemCss}.selected`);
        let cloned = selected.clone(true).addClass('dragging');
        handler.append(cloned);
        let z = me.zoom;
        cloned.each(function () {
            let el = $(this);
            el.css({
                left: el.css('left').replace('px', '') - position.left * 100 / z,
                top: el.css('top').replace('px', '') - position.top * 100 / z
            });
        });
        return handler;
    },
    drawItem(item) {
        let me = this;
        me.canvas.append(item.dom);
        item.dom.draggable($.extend({
            scroll: true,
            opacity: 0.8,
            zIndex: 999,
            containment: 'parent',
            stack: ".diagram-item",
            delay: 100,
            helper: function (e) {
                return me.createDragHandler(e);
            },
            start: function (e, ui) {
                if (me.readonly()) {
                    return false;
                }
            },
            drag: function (e, ui) {
                let p = ui.position;
                let z = me.zoom;
                p.left = p.left * 100 / z;
                p.top = p.top * 100 / z;
            },
            stop: function (e, ui) {
                let p = ui.position;
                let id = ui.helper.attr('data-id');
                let item = me.items.get(id);
                let dx = p.left - item.x;
                let dy = p.top - item.y;
                let items = me.items.find(i => i.selected);
                me._moveItems({items, dx, dy});
            }
        }, me.itemDragOptions));
        me.items.add(item.id, item);
        me.aerialView.add(item.id);
        return item;
    },
    load(data) {
        let me = this;
        me.canvas.empty();
        me.items.clear();
        me.aerialView.clear();
        me.cmdMemo.clear();
        if (data.length > 0) {
            for (let row of data) {
                let item = me.createItem(row);
                me.drawItem(item);
            }
            me.updateConnectors(me.items.values);
        }
        me.dom.triggerHandler('load', [me]);
        me.dom.find('.jui-diagram').focus();
    },
    unload() {
        let me = this;
        me.canvas.empty();
        me.items.clear();
        me.aerialView.clear();
        me.cmdMemo.clear();
        me.readonly(true);
    },
    readonly(value) {
        let me = this;
        let dom = me.dom.find('.jui-diagram');
        if (value == undefined) {
            return dom.hasClass('readonly');
        }
        if (value) {
            dom.addClass('readonly');
        } else {
            dom.removeClass('readonly');
        }
    },
    updateConnectors(items) {
    },
});
