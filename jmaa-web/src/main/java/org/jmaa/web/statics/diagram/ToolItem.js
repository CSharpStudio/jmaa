//@ sourceURL=ToolItem.js
jmaa.component("ToolItem", {
    createItem(point) {
    },
    init() {
        let me = this;
        me.dom.draggable({
            scroll: false,
            opacity: 0.8,
            zIndex: 999,
            cursor: "none",
            containment: me.containment || 'window',
            cursorAt: {top: 0, left: 0},
            helper: function (e) {
                return $(e.currentTarget).clone(true).css({
                    zIndex: "99999",
                    position: 'absolute',
                }).addClass('dragging');
            },
            start: function (e) {
            },
            drag: function (e) {
            },
            stop: function (e, item) {
                let canvas = me.diagram.canvas.offset();
                let zoom = 100 / me.diagram.zoom;
                let point = {
                    x: Math.max(0, (e.pageX - canvas.left) * zoom),
                    y: Math.max(0, (e.pageY - canvas.top) * zoom),
                    type: item.helper.attr('type'),
                    label: item.helper.text(),
                };
                me.createItem(point);
            }
        });
    }
});
