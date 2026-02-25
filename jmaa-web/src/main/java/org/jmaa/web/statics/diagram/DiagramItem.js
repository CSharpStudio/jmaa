//@ sourceURL=DiagramItem.js
jmaa.component("DiagramItem", {
    css: 'diagram-item',
    getTpl() {
        let me = this;
        return `<div id="${me.id}" class="${me.css}" style="top:${me.y}px;left:${me.x}px;"><div class="title">${me.label}</div></div>`;
    },
    init() {
        let me = this;
        me.connectors = [];
        me.dom = $(me.getTpl());
    },
    moveTo: function (point) {
        let me = this;
        me.x = point.x;
        me.y = point.y;
        me.dom.css('left', me.x + 'px').css('top', me.y + 'px');
        $.each(me.connectors, function () {
            this.update();
        });
    },
    select() {
        let me = this;
        me.selected = true;
        me.dom.addClass('selected');
        let idx = parseInt(me.dom.css('z-index'));
        if (idx < 200) {
            me.dom.css('z-index', idx + 100);
        }
    },
    unselect() {
        let me = this;
        me.selected = false;
        me.dom.removeClass('selected');
        let idx = parseInt(me.dom.css('z-index'));
        if (idx > 200) {
            me.dom.css('z-index', idx - 100);
        }
    },
    getData() {
        let me = this;
        let data = $.extend({}, me.data);
        data.id = me.id;
        data.x = me.x;
        data.y = me.y;
        return data;
    }
});
