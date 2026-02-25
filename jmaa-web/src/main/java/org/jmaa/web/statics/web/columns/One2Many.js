jmaa.column('one2many', {
    render() {
        let me = this;
        return function (data, type, row) {
            let id = "o2m" + jmaa.nextId(), ids = [], present = '';
            const currentLabel = row[me.field.name + '@present'] || ''
            if (currentLabel) {
                return `<span row="${id}" o2m-field="${me.field.name}">${currentLabel}</span>`;
            }
            if (data && data.length > 0) {
                for (let row of data) {
                    if ($.isArray(row)) {
                        if (row.length == 3) {
                            ids.push(row[1]);
                        } else if (row.length == 2) {
                            if (present) {
                                present += ",";
                            }
                            present += row[1];
                        }
                    } else if (row.id) {
                        ids.push(row.id);
                    } else {
                        ids.push(row);
                    }
                }
            }
            if (present) {
                row[me.field.name + '@present'] = present;
                return `<span class="char" row="${id}" o2m-field="${me.field.name}">${row[me.field.name + '@present']}</span>`;
            }
            if (ids && ids.length > 0) {
                jmaa.rpc({
                    model: me.owner.model,
                    module: me.owner.module,
                    method: "searchByField",
                    args: {
                        relatedField: me.field.name,
                        criteria: [['id', 'in', ids]],
                        fields: ["present"],
                        limit: ids.length,
                        nextTest: false
                    },
                    context: {
                        active_test: false,
                        company_test: false,
                    },
                    onsuccess(r) {
                        let present = '', ids = '';
                        for (let item of r.data.values) {
                            if (ids) {
                                ids += ",";
                                present += ",";
                            }
                            present += item.present;
                            ids += item.id;
                        }
                        me.owner.dom.find(`[row=${id}]`).html(present).attr("data-id", ids);
                        row[me.field.name + '@present'] = present;
                    }
                });
            }
            return `<span class="char" row="${id}" o2m-field="${me.field.name}"></span>`;
        }
    }
});
