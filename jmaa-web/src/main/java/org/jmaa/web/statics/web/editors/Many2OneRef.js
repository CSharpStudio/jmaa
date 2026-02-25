jmaa.editor('many2one_reference', {
    extends: 'editors.many2one',
    initLookupGrid(viewKey) {
        //未支持
    },
    showLinkModel(rec) {
        let me = this;
        me.linkDialog = jmaa.showDialog({
            id: 'm2o-' + me.id,
            submitText: '保存'.t(),
            title: '打开'.t() + ' ' + me.label.t(),
            init(dialog) {
                dialog.body.html(`<iframe style="width:100%;border:0;" src="${jmaa.web.getTenantPath()}/view#model=${me.comodel}&view=form&top=1${rec.id ? '&id=' + rec.id : '&present=' + rec.present}"></iframe>`);
                dialog.frame = dialog.body.find('iframe');
                dialog.frame.css('height', $(document.body).height() - 300);
            }
        });
    },
    searchRelated(callback, fields) {
        let me = this;
        let model = me.owner.getRawData(me.field.modelField);
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "searchByField",
            args: {
                relatedField: me.name,
                limit: me.limit,
                offset: me.offset,
                criteria: me.getFilter(),
                fields,
                order: me.order,
            },
            context: {
                comodel: model,
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    },
    loadPresent(id, input) {
        let me = this;
        let model = me.owner.getRawData(me.field.modelField);
        jmaa.rpc({
            model: me.owner.model,
            module: me.owner.module,
            method: "searchByField",
            args: {
                relatedField: me.name,
                criteria: [['id', '=', id]],
                fields: ["present"],
                limit: 1,
            },
            context: {
                comodel: model,
                active_test: false,
                company_test: false,
            },
            onsuccess(result) {
                let value = result.data.values;
                if (value.length > 0) {
                    input.val(value[0].present);
                    me.dom.find('.input-suffix').addClass('link');
                    me.dom.attr('data-value', value[0].id).data('text', value[0].present);
                }
            }
        });
    },
});
