//@ sourceURL=sampling_plan.js
jmaa.view({
    changType(target){
        let editForm = target.owner;
        let data = editForm.getRawData();
        let required = data.type && data.type !== 'dynamic';
        editForm.setEditorRequired("ac",required);
    }
});
jmaa.editor("level-letter-table", {
    getTpl() {
        let me = this;
        return `<table class="table table-bordered text-center">
                <thead>
                    <tr>
                        <th rowspan="2">批量</th>
                        <th colspan="4">特殊检验水平</th>
                        <th colspan="4">一般检验水平</th>
                    </tr>
                    <tr>
                        <th>S-1</th>
                        <th>S-2</th>
                        <th>S-3</th>
                        <th>S-4</th>
                        <th>Ⅰ</th>
                        <th>Ⅱ</th>
                        <th>Ⅲ</th>
                    </tr>
                </thead>
            </table>`
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl());
        me.loadTable();
    },
    loadTable() {
        let me = this;
        jmaa.rpc({
            module: me.owner.module,
            model: me.owner.model,
            method: 'getLevelLetterTable',
            onsuccess(r) {
                me.renderTable(r.data);
            }
        })
    },
    renderTable(data) {
        let me = this;
        let table = me.dom.find('table');
        let row = [];
        for (let d of data) {
            row.push(`<tr>
                        <td>${d.limit}</td>
                        <td>${d.s1}</td>
                        <td>${d.s2}</td>
                        <td>${d.s3}</td>
                        <td>${d.s4}</td>
                        <td>${d.g1}</td>
                        <td>${d.g2}</td>
                        <td>${d.g3}</td>
                    </tr>`);
        }
        table.append(`<tbody>${row}</tbody>`);
    }
});
jmaa.editor("aql-table", {
    getTpl() {
        let me = this;
        return `<table class="table table-bordered text-center">
                <thead>
                    <tr>
                        <th rowspan="2">字码</th>
                        <th rowspan="2">样本量</th>
                    </tr>
                </thead>
            </table>`
    },
    init() {
        let me = this;
        me.strictness = me.dom.attr("strictness") || 'normal';
        me.dom.html(me.getTpl()).css('overflow', 'auto');
    },
    setValue() {
        let me = this;
        jmaa.rpc({
            module: me.owner.module,
            model: me.owner.model,
            method: 'getAqlTable',
            args: {
                ids: [me.owner.dataId],
                strictness: me.strictness,
            },
            onsuccess(r) {
                me.renderTable(r.data);
            }
        })
    },
    renderTable(data) {
        let me = this;
        let table = me.dom.find('table');
        let th1 = [];
        let th2 = [];
        let body = [];
        let header = [];
        let aqls = {};
        for (let item of data.aql) {
            let aql = parseFloat(item.name);
            header.push(aql);
            aqls[aql] = item;
        }
        header.sort(function (a, b) {
            return a - b
        });
        for (let h of header) {
            th1.push(`<th class="p-1">${aqls[h].name}</th>`);
            th2.push(`<th class="p-1">Ac Re</th>`);
        }
        for (let sample of data.sample) {
            let td = [`<td>${sample.letter}</td><td>${sample.size}</td>`];
            let letter = sample.letter.toLowerCase();
            for (let h of header) {
                let aql = aqls[h];
                let value = aql[letter];
                if (value == -1) {
                    td.push(`<td>↓</td>`);
                } else if (value == -2) {
                    td.push(`<td>↑</td>`);
                } else if(value != null) {
                    td.push(`<td>${value} ${value + 1}</td>`);
                }else{
                    td.push('<td></td>');
                }
            }
            body.push(`<tr>${td.join('')}</tr>`);
        }
        table.html(`<thead>
                        <tr>
                            <th class="p-1" rowspan="2">字码</th>
                            <th class="p-1" rowspan="2">样本量</th>
                            ${th1}
                        </tr>
                        <tr>${th2}</tr>
                    </thead>
                    <tbody>${body}</tbody>`);
    }
});
