//@ sourceURL=Connector.js
jmaa.component("Connector", {
    css: '',
    getTpl() {
        let me = this;
        return `<svg id="${me.id}" pointer-events="none" version="1.1" xmlns="http://www.w3.org/2000/svg" class="diagram-connector ${me.css}">
                    <marker id="arrow-${me.id}" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                        <path d="M 5 0 L 10 5 L 5 10" stroke-width="1" fill="none" />
                    </marker>
                    <polyline stroke-width="2" fill="none" marker-end="url(#arrow-${me.id})"/>
                    <polyline class="line-bg" fill="none"/>
                </svg>`;
    },
    init() {
        let me = this;
        me.svg = $(me.getTpl());
        me.diagram.canvas.append(me.svg);
        me.line = me.svg.find('polyline');
        me.line.on('mouseover', function () {
            me.svg.addClass('hover');
        });
        me.line.on('mouseout', function () {
            me.svg.removeClass('hover');
        });
        me.diagram.items.get(me.data.from).connectors.push(me);
        me.diagram.items.get(me.data.to).connectors.push(me);
        me.update();
    },
    remove() {
        let me = this;
        me.diagram.items.get(me.data.from).connectors.remove(me);
        me.diagram.items.get(me.data.to).connectors.remove(me);
        me.svg.remove();
    },
    update() {
        let me = this;
        let c = me.diagram.canvas.offset();
        let f = me.from.offset();
        let t = me.to.offset();
        let m = {
            x: (f.left - c.left) * 100 / me.diagram.zoom,
            y: (f.top - c.top) * 100 / me.diagram.zoom,
            h: me.from.height(),
            w: me.from.width()
        };
        let n = {
            x: (t.left - c.left) * 100 / me.diagram.zoom,
            y: (t.top - c.top) * 100 / me.diagram.zoom,
            h: me.to.height(),
            w: me.to.width()
        };
        m.p = m.y + m.h / 2;
        n.p = n.y + n.h / 2;
        let points = [];
        if (m.p > n.p) {
            me.svg.css('top', (n.p - 10) + 'px');
        } else {
            me.svg.css('top', (m.p - 10) + 'px');
        }
        me.svg.height(Math.abs((m.y + m.h / 2) - (n.y + n.h / 2)) + 20);
        if (n.x - m.x < 50) {
            if (m.x + m.w < n.x) {
                me.svg.css('left', m.x + 'px');
                me.svg.width(Math.max(40, n.x - m.x));
            } else {
                me.svg.css('left', (n.x - 20) + 'px');
                me.svg.width(m.x - n.x + 60);
            }
            if (Math.abs(m.p - n.p) < 60) {
                let height = Math.abs(m.p - n.p) + 50;
                me.svg.height(height);
                points.push((m.x + m.w < n.x ? 20 : me.svg.width() - 20) + "," + (m.p > n.p ? m.p - n.p + 10 : 10));
                points.push((m.x + m.w < n.x ? 30 : me.svg.width() - 10) + "," + (m.p > n.p ? m.p - n.p + 10 : 10));
                points.push((m.x + m.w < n.x ? 30 : me.svg.width() - 10) + "," + (height - 5));
                points.push("5," + (height - 5));
                points.push("5," + (m.p > n.p ? 10 : n.p - m.p + 10));
                points.push((m.x + m.w < n.x ? n.x - m.x : 20) + "," + (m.p > n.p ? 10 : n.p - m.p + 10));
            } else {
                points.push((m.x + m.w < n.x ? 20 : me.svg.width() - 20) + "," + (m.p > n.p ? me.svg.height() - 10 : 10));
                points.push((m.x + m.w < n.x ? 35 : me.svg.width() - 5) + "," + (m.p > n.p ? me.svg.height() - 10 : 10));
                points.push((m.x + m.w < n.x ? 35 : me.svg.width() - 5) + "," + (me.svg.height() / 2));
                points.push((m.x + m.w < n.x ? n.x - m.x - 15 : 5) + "," + (me.svg.height() / 2));
                points.push((m.x + m.w < n.x ? n.x - m.x - 15 : 5) + "," + (m.p > n.p ? 10 : me.svg.height() - 10));
                points.push((m.x + m.w < n.x ? n.x - m.x : 20) + "," + (m.p > n.p ? 10 : me.svg.height() - 10));
            }
        } else {
            me.svg.css('left', (m.x + m.w - 10) + 'px');
            me.svg.width(n.x - m.x - m.w + 20);
            points.push("10," + (m.p > n.p ? me.svg.height() - 10 : 10));
            let ac = me.to.attr('id') == 'end' || me.to.hasClass('gateway');
            if (Math.abs(m.p - n.p) < 3 || (!ac && Math.abs(m.p - n.p) < 25)) {
                points.push((me.svg.width() - 10) + "," + (m.p > n.p ? me.svg.height() - 10 : 10));
            } else {
                points.push((me.svg.width() / 2) + "," + (m.p > n.p ? me.svg.height() - 10 : 10));
                points.push((me.svg.width() / 2) + "," + (m.p > n.p ? 10 : me.svg.height() - 10));
                points.push((me.svg.width() - 10) + "," + (m.p > n.p ? 10 : me.svg.height() - 10));
            }
        }
        me.line.attr('points', points.join(' '));
    }
});
