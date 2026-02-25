//@ sourceURL=EditView.js
jmaa.define("EditView", {
    getXPath(el) {
        let me = this;
        if (el.length === 0) return '';
        let path = [];
        let cur = el;
        while (cur.length) {
            let node = cur[0];
            let tagName = node.tagName.toLowerCase();
            let id = node.id;
            let name = cur.attr('name');
            let label = cur.attr('label');
            id = id ? `[id='${id}']` : '';
            name = name ? `[name='${name}']` : '';
            label = label ? `[label='${label}']` : '';
            path.unshift(`${tagName}${id}${name}${label}`);
            cur = cur.parent();
            if (tagName == me.type) {
                break;
            }
        }
        return `${path.join('>')}`;
    },
    combineViews(arch, views) {
        for (let ext of views) {
            let dom = jmaa.utils.parseXML(ext.arch);
            dom.children('xpath').each(function () {
                let xpath = $(this);
                let expr = xpath.attr('expr');
                let position = xpath.attr('position') || 'inside';
                let found = arch.find(expr);
                if (position == 'inside') {
                    found.append(xpath.html());
                } else if (position == 'after') {
                    found.after(xpath.html());
                } else if (position == 'before') {
                    found.before(xpath.html());
                } else if (position == 'replace') {
                    found.replaceWith(xpath.html());
                } else if (position == 'remove') {
                    found.remove();
                } else if (position == 'attribute') {
                    for (let attr of xpath.children()[0].attributes) {
                        found.attr(attr.name, attr.value);
                    }
                } else if (position == 'move') {
                    let move = xpath.children('xpath');
                    let movePosition = move.attr('position') || 'inside';
                    let moveExpr = move.attr('expr');
                    let moveTo = arch.find(moveExpr);
                    if (movePosition == 'inside') {
                        found.appendTo(moveTo);
                    } else if (movePosition == 'after') {
                        found.insertAfter(moveTo);
                    } else if (movePosition == 'before') {
                        found.insertBefore(moveTo);
                    }
                }
            })
        }
    },
    getViewFields() {
        let me = this;
        if (!me.studio.data.modelFields) {
            jmaa.rpc({
                async: false,
                method: 'loadModelFields',
                model: me.studio.model,
                module: me.studio.module,
                args: {
                    model: me.studio.data.model
                },
                onsuccess(r) {
                    me.studio.data.modelFields = r.data.fields;
                }
            })
        }
        return me.studio.data.modelFields;
    },
    loadViewData() {
        let me = this;
        me.fields = me.getViewFields();
        jmaa.rpc({
            model: me.studio.model,
            method: 'searchView',
            args: {
                criteria: [['model', '=', me.studio.data.model], ['type', '=', me.type], ['active', '=', true], ['key', '=', null]],
                fields: ["mode", "priority", "arch", "module"],
                limit: 1000,
                order: 'mode desc,priority'
            },
            context: {
                usePresent: true
            },
            onsuccess(r) {
                me.data = r.data.values;
                me.primaryView = me.data.find(item => item.mode == 'primary');
                me.extensionViews = [];
                for (let row of me.data.sort((x, y) => x.priority - y.priority)) {
                    if (row.module == 'studio' && row.mode == 'extension') {
                        me.studioView = row;
                    }
                    if (row.mode != 'primary') {
                        me.extensionViews.push(row);
                    }
                }
                if (!me.primaryView) {
                    throw new Error("没找到mode=primary的视图");
                }
                me.onViewLoaded();
            }
        });
    },
    onViewLoaded() {
    },
    filterFields() {
        let me = this;
        let keyword = me.dom.find('.sidebar_content input').val();
        let html = [];
        for (let key of Object.keys(me.fields)) {
            if (me._fields.includes(key)) {
                continue;
            }
            let f = me.fields[key];
            let title = f.help ? ` title="${f.help}"` : '';
            if (!keyword || f.label.includes(keyword) || f.name.includes(keyword)) {
                html.push(`<div field="${f.name}" class="s-field-${f.type} s-component"${title}>${f.label}
                    <div class="s-component-description">${f.name}</div>
                </div>`);
            }
        }
        me.dom.find('.s-fields .s-component').draggable('destroy');
        me.dom.find('.s-fields').html(html.join(''));
        me.initDraggable(me.dom.find('.s-fields .s-component'));
    },
    addField(field, target) {
        let me = this;
        let xpath = target.attr('xpath');
        let position = target.attr('position');
        let arch = `<xpath expr="${xpath}" position="${position}"><field name="${field}"></field></xpath>`;
        if (me.studioView) {
            let xml = jmaa.utils.parseXML(me.studioView.arch);
            xml.append(arch);
            me.saveView(xml.html());
        } else {
            me.saveView(arch);
        }
    },
    moveField(xpath, target) {
        let me = this;
        let toXpath = target.attr('xpath');
        let position = target.attr('position');
        if (me.studioView) {
            let xml = jmaa.utils.parseXML(me.studioView.arch);
            let old = xml.find(`[expr="${xpath}"][position=move]`);
            if (old.length) {
                old.html(`<xpath expr="${toXpath}" position="${position}"></xpath>`);
            } else {
                xml.append(`<xpath expr="${xpath}" position="move"><xpath expr="${toXpath}" position="${position}"></xpath></xpath>`);
            }
            me.saveView(xml.html());
        } else {
            me.saveView(`<xpath expr="${xpath}" position="move"><xpath expr="${toXpath}" position="${position}"></xpath></xpath>`);
        }
    },
    removeItem(xpath) {
        let me = this;
        delete me.studio.data.modelFields;
        if (me.studioView) {
            let xml = jmaa.utils.parseXML(me.studioView.arch);
            xml.append(`<xpath expr="${xpath}" position="remove"></xpath>`);
            me.saveView(xml.html());
        } else {
            me.saveView(`<xpath expr="${xpath}" position="remove"></xpath>`);
        }
    },
    _createView(arch) {
        let me = this;
        jmaa.rpc({
            model: me.studio.model,
            method: 'createView',
            args: {
                values: {
                    model: me.studio.data.model,
                    name: `${me.studio.data.model}-${me.type}-studio`,
                    type: me.type,
                    mode: 'extension',
                    module_id: me.studio.data.module[0],
                    arch,
                },
            },
            onsuccess(r) {
                me.loadViewData();
            }
        });
    },
    _updateView(viewId, arch) {
        let me = this;
        jmaa.rpc({
            model: me.studio.model,
            method: 'updateView',
            args: {
                viewId,
                values: {
                    arch,
                },
            },
            onsuccess(r) {
                me.loadViewData();
            }
        });
    },
    _deleteView(viewId) {
        let me = this;
        jmaa.rpc({
            model: me.studio.model,
            method: 'deleteView',
            args: {
                viewId
            },
            onsuccess(r) {
                me.loadViewData();
            }
        });
    },
    saveView(arch) {
        let me = this;
        if (me.studioView) {
            let oldView = me.studioView.arch;
            me.memo.add({
                undo() {
                    me._updateView(me.studioView.id, oldView);
                },
                redo() {
                    me._updateView(me.studioView.id, arch);
                }
            });
            me._updateView(me.studioView.id, arch);
        } else {
            me.memo.add({
                undo() {
                    me._deleteView(me.studioView.id);
                },
                redo() {
                    me._createView(arch);
                }
            });
            me._createView(arch);
        }
    },
    onDragging(e) {
        let me = this;
        let body = me.dom.find('.containment');
        let scrollOffset = body.offset();
        let scrollWidth = body.width();
        let scrollHeight = body.height();
        let mouseX = e.pageX;
        let mouseY = e.pageY;
        if (mouseX > scrollOffset.left - 30 && mouseX < scrollOffset.left + scrollWidth + 30) {
            if (mouseY < scrollOffset.top + 30) {
                body.scrollTop(body.scrollTop() - 5);
            }
            if (mouseY > scrollOffset.top + scrollHeight - 30) {
                body.scrollTop(body.scrollTop() + 5);
            }
        }
        if (mouseY > scrollOffset.top - 30 && mouseY < scrollOffset.top + scrollHeight + 30) {
            if (mouseX < scrollOffset.left + 50) {
                body.scrollLeft(body.scrollLeft() - 5);
            }
            if (mouseX > scrollOffset.left + scrollWidth - 50) {
                body.scrollLeft(body.scrollLeft() + 5);
            }
        }
    },
    onDragStop(e, ui) {
        let me = this;
        let target = me.dom.find(".drop-target");
        if (target.length) {
            let field = ui.helper.attr('field');
            me.addField(field, target);
        }
    },
    initDraggable(dom) {
        let me = this;
        dom.draggable({
            scroll: true,
            zIndex: 999,
            cursor: "none",
            cursorAt: {left: 5, top: 10},
            containment: 'window',
            helper: function (e) {
                return $(e.currentTarget).clone(true).css({
                    zIndex: "99999",
                    position: 'absolute',
                }).addClass('dragging');
            },
            drag: function (e) {
                me.onDragging(e);
            },
            stop: function (e, ui) {
                me.onDragStop(e, ui);
            }
        });
    },
    onMoveDragStop(e, ui) {
        let me = this;
        $(e.target).css("display", "block");
        let target = me.dom.find(".drop-target");
        if (target.length) {
            let xpath = ui.helper.attr('xpath');
            me.moveField(xpath, target);
        }
    },
    onMoveDragStart(e) {
        $(e.target).css("display", "none");
    },
    moveDraggable(dom) {
        let me = this;
        dom.draggable({
            scroll: true,
            zIndex: 999,
            cursor: "move",
            containment: 'window',
            cursorAt: {left: 0, top: 0},
            helper: function (e) {
                let helper = $(e.currentTarget).clone(true).css({
                    zIndex: "99999",
                    position: 'absolute',
                }).addClass('move-dragging');
                $("body").append(helper);
                return helper;
            },
            start: function (e) {
                me.onMoveDragStart(e);
            },
            drag: function (e) {
                me.onDragging(e);
            },
            stop: function (e, ui) {
                me.onMoveDragStop(e, ui);
            }
        });
    },
    nvl() {
        for (let value of arguments) {
            if (value != null && value != undefined) {
                return value;
            }
        }
        return null;
    },
    initMemo(onChange) {
        let me = this;
        me.memo = jmaa.create('CommandMemo');
        onChange && me.memo.onChange(onChange);
    },
});
