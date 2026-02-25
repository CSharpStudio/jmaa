//@ sourceURL=RouteNode.js
jmaa.component("RouteNode", {
    extends: 'DiagramItem',
    getTpl() {
        let me = this;
        if (me.id == 'start') {
            return `<div id="${me.id}" class="${me.css} start" style="top:${me.y}px;left:${me.x}px;">
                        <div class="title">${me.present}</div>
                        <div class="output" type="ok"></div>
                    </div>`;
        } else if (me.id == 'end') {
            return `<div id="${me.id}" class="${me.css} end" style="top:${me.y}px;left:${me.x}px;">
                        <div class="title">${me.present}</div>
                    </div>`;
        } else {
            let out = '<div class="output" type="ok"></div>';
            return `<div id="${me.id}" class="${me.css}" style="top:${me.y}px;left:${me.x}px;">
                        <div class="w-100">
                            <div class="title">${me.present}</div>
                            <div class="b-bar">
                                <div class="item-menu"><i class="fa fa-bars"></i></div>
                                <div class="params">${(me.data.next_relationship || '').toUpperCase()}</div>
                            </div>
                        </div>
                        ${out}
                    </div>`;
        }
    },
});
