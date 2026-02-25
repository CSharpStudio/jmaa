//@ sourceURL=RouteNode.js
jmaa.component("RouteNode", {
    extends: 'DiagramItem',
    getTpl() {
        let me = this;
        let getParams = function (data) {
            let params = [];
            if (data.is_optional) {
                params.push(`<span title="${'可选工序'.t()}">可选</span>`);
            }
            if (data.is_repeatable) {
                params.push(`<span title="${'允许重复过站'.t()}">重复</span>`);
            }
            if (data.enable_move_in) {
                params.push(`<span title="${'进出站控制'.t()}">进出</span>`);
            }
            if (data.create_task) {
                params.push(`<span title="${'生成任务单'.t()}">任务</span>`);
            }
            if (data.is_deduction) {
                params.push(`<span title="${'按工序BOM扣料'.t()}">扣料</span>`);
            }
            if (data.is_output) {
                params.push(`<span title="${'计算工单产量'.t()}">计产</span>`);
            }
            if (data.to_fqc) {
                params.push(`<span title="${'生成FQC检验单'.t()}">终检</span>`);
            }
            return params.join('/');
        }
        if (me.data.type == 'gateway') {
            return `<div id="${me.id}" class="${me.css} gateway" style="top:${me.y}px;left:${me.x}px;">
                        <div class="bg-border"></div>
                        <div class="title">${me.data.label}</div>
                        <div class="item-menu"><i class="fa fa-bars"></i></div>
                        <div class="output ok" type="ok"></div>
                        <div class="output ng" type="ng"></div>
                    </div>`;
        } else if (me.data.type == 'group') {
            let children = [];
            for (let n of me.data.children) {
                children.push(`<div class="group-item" id="${n.id}" pid="${n.parent_id}">
                                    <div class="type ${n.process_type}"></div>
                                    <div class="w-100">
                                        <div class="title">${n.present}</div>
                                        <div class="b-bar">
                                            <div class="item-menu"><i class="fa fa-bars"></i></div>
                                            <div class="params">${getParams(n)}</div>
                                        </div>
                                    </div>
                                </div>`);
            }
            return `<div id="${me.id}" class="${me.css} item-group" style="top:${me.y}px;left:${me.x}px;">
                        <div class="title">${me.data.label}</div>
                        <div class="process-items">
                            ${children.join('')}
                        </div>
                        <div class="item-bar">
                            <div class="btn-add"><i class="fa fa-plus-circle"></i></div>
                            <div class="item-menu group"><i class="fa fa-bars"></i></div>
                        </div>
                        <div class="output" type="ok"></div>
                    </div>`;
        } else if (me.id == 'start') {
            return `<div id="${me.id}" class="${me.css} start" style="top:${me.y}px;left:${me.x}px;">
                        <div class="title">${me.label}</div>
                        <div class="output" type="ok"></div>
                    </div>`;
        } else if (me.id == 'end') {
            return `<div id="${me.id}" class="${me.css} end" style="top:${me.y}px;left:${me.x}px;">
                        <div class="title">${me.label}</div>
                    </div>`;
        } else {
            let out = '';
            if (me.data.collection_result == 'ok') {
                out = '<div class="output" type="ok"></div>';
            } else {
                out = '<div class="output ok" type="ok"></div><div class="output ng" type="ng"></div>';
            }
            return `<div id="${me.id}" class="${me.css}" style="top:${me.y}px;left:${me.x}px;">
                        <div class="type ${me.data.process_type}"></div>
                        <div class="w-100">
                            <div class="title">${me.label}</div>
                            <div class="b-bar">
                                <div class="item-menu"><i class="fa fa-bars"></i></div>
                                <div class="params">${getParams(me.data)}</div>
                            </div>
                        </div>
                        ${out}
                    </div>`;
        }
    },
});
