//@ sourceURL=pick_out.js
jmaa.view({
    openPickOutView() {
        let me = this;
        jmaa.showDialog({
            title: '挑选'.t(),
            css: '',
            init(dialog) {
                me.loadView('wms.pick_out_details', 'form', 'pick_out_dialog').then(v => {
                    dialog.form = dialog.body.JForm({
                        cols: 1,
                        model: v.model,
                        module: v.module,
                        fields: v.fields,
                        arch: v.views.form.arch,
                        view: me
                    });
                    dialog.dom.find('.buttons-right')
                        .html(`<button type="button" t-click="resetPick" class="btn btn-default btn-flat">${'重置'.t()}</button>
                            <button type="button" t-click="submitPick" class="btn btn-blue btn-flat">${'确定'.t()}</button>`);
                    dialog.resetPick(null, dialog);
                    dialog.form.create({"material_stock_rule": me.form.editors.material_stock_rule.getValue()});
                    dialog.form.editors.sn.dom.on('keyup', 'input', function (e) {
                        if (e.keyCode === 13 && $(this).val()) {
                            me.submitCode(dialog.form);
                        }
                    });
                    dialog.form.editors.qty.dom.on('keyup', 'input', function (e) {
                        if (e.keyCode === 13 && $(this).val()) {
                            me.submitCode(dialog.form);
                        }
                    });
                });
                dialog.body.css("min-height", "30px");
            },
            resetPick(e, dialog){
                dialog.form.create({"material_stock_rule": me.form.editors.material_stock_rule.getValue()});
            },
            submitPick(e, dialog){
                me.submitCode(dialog.form);
            },
            cancel() {
                me.form.editors.detail_ids.load()
            }
        });
    },
    submitCode(form) {
        let me = this;
        let materialStockRule = me.form.editors.material_stock_rule.getRawValue();
        let sn = form.editors.sn.getValue();
        if (!sn) {
            return jmaa.msg.error('请输入标签条码'.t());
        }
        if (materialStockRule !== 'sn') {
            if (!form.editors.qty.getValue()) {
                return jmaa.msg.error('请输入挑选数量'.t());
            }
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanMaterialCode',
            args: {
                ids: [me.form.dataId],
                code: form.editors.sn.getValue(),
                qty: form.editors.qty.getValue()
            }, onerror(r) {
                jmaa.msg.error(r);
            }, onsuccess: function (r) {
                form.editors.sn.dom.find('input').val('');
                form.editors.qty.dom.find('input').val('');
                me.form.editors.detail_ids.load()
                jmaa.msg.show("操作成功".t());
            }
        });
    },
    submitPick() {
        let me = this;
        let pickType = me.form.editors.pick_type.getRawValue()
        if (!pickType) {
            jmaa.msg.error("请选择挑选类型".t())
            return
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'submitPick',
            args: {
                ids: [me.form.dataId],
                pickType
            },
            onerror(r) {
                jmaa.msg.error(r);
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t());
                me.form.load();
            }
        });
    },
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        if ('draft' !== status && 'reject' !== status ) {
            me.form.setReadonly(true);
        }
    },
})
jmaa.editor('related_code_editor', {
    extends: "editors.many2one",
    searchRelated(callback, fields) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: "searchRelatedCode",
            args: {
                relatedField: me.field.name,
                limit: me.limit,
                offset: me.offset,
                criteria: me.getFilter(),
                fields,
                order: me.order,
                type: me.owner.editors['type'].getRawValue()
            },
            context: {
                usePresent: true,
                active_test: me.activeTest,
                company_test: me.companyTest,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    },
    loadPresent(id, el, link) {
        let me = this;
        me.setValue([id, id]);
    },
});
