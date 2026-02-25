//@ sourceURL=model-lookup.js
jmaa.editor('model-lookup', {
    extends: 'editors.many2one',
    searchRelated(callback) {
        let me = this;
        let criteria = ['|', ["model", "like", me.keyword], ["name", "like", me.keyword]];
        jmaa.rpc({
            model: 'ir.model',
            module: 'base',
            method: "search",
            args: {
                limit: me.limit,
                offset: me.offset,
                criteria,
                fields: ['model', 'name'],
                order: 'model asc',
                nextTest: true
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                if (r.data.values.length) {
                    for (let value of r.data.values) {
                        value.id = value.model;
                        value.present = value.model;
                        if (value.name != value.model) {
                            value.present += `(${value.name})`;
                        }
                    }
                }
                callback(r);
            }
        });
    },
    selectItem(item) {
        let me = this;
        me.selectValue(item.attr('value'));
    },
    setValue(value) {
        let me = this;
        let input = me.dom.find('input');
        if (value) {
            input.val(value).attr('placeholder', '');
            me.dom.attr('data-value', value).data('text', value).trigger('valueChange');
        } else {
            input.val('').attr('placeholder', me.placeholder);
            me.dom.attr('data-value', '').data('text', '').trigger('valueChange');
        }
    }
});
jmaa.searchEditor('model-lookup', {
    extends: "editors.model-lookup",
    getCriteria: function () {
        let val = this.getRawValue();
        if (val) {
            return [[this.name, '=', val]];
        }
        return [];
    },
    getText: function () {
        return this.getValue();
    },
});
