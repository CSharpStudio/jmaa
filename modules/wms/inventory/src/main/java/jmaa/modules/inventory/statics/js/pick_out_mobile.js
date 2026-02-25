//@ sourceURL=pick_out_mobile.js
jmaa.view({
    searchPickList() {
        let me = this;
        me.pickList.load();
        me.tabs.open("pickTab")
    },
    loadPickList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-pick-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchPickOutList',
            args: {
                keyword,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data});
            }
        });
    },
    showDetails() {
        let me = this;
        let hide = me.dom.find('.field-group').hasClass('collapsed');
        me.dom.find('.list-panel').css('height', hide ? 'calc(100% - 166px)' : 'calc(100% - 288px)');
    },
    backSearchList() {
        let me = this;
        me.pickList.load()
    },
    openPick() {
        let me = this;
        me.changePage("pick");
        let pickId = me.pickList.getSelected()[0];
        me.pickForm.dataId = pickId;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'read',
            args: {
                ids: [pickId],
                fields: me.pickForm.getFields()
            },
            context: {
                usePresent: me.pickForm.getUsePresent(),
            },
            onsuccess: function (r) {
                me.pickForm.loadData(r.data[0]);
                me.pickForm.setReadonly(true);
                me.pickForm.editors.pick_type.setReadonly(false)
                //me.detail_ids.load();
            }
        });
        me.tabs.open("pickTab");
    },
    scanPickDetail() {
        let me = this;
        let form = me.pickForm;
        if ('done' === form.editors.status.getRawValue()) {
            jmaa.msg.error('单据已提交,不能继续扫码'.t())
            return;
        }
        let detailList = me.detail_ids;
        let input = me.dom.find('.pick-detail-input');
        let code = input.val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanMaterialCode',
            args: {
                ids: [form.dataId],
                code,
            },
            onerror(r) {
                jmaa.msg.error(r)
            },
            onsuccess: function (r) {
                if (r.data) {
                    if ('lot' === r.data.material_stock_rule || 'num' === r.data.material_stock_rule) {
                        // 弹框
                        let data = r.data
                        jmaa.showDialog({
                            title: "请输入挑选数量".t(),
                            init(dialog) {
                                me.loadView("wms.pick_out_details", 'form', "pick_outlot_num_mobile").then(v => {
                                    dialog.form = dialog.body.JForm({
                                        arch: v.views.form.arch,
                                        fields: v.fields,
                                        module: v.module,
                                        model: v.model,
                                        view: me,
                                    });
                                    dialog.form.dom.enhanceWithin();
                                    dialog.form.create(data);
                                    // 挑选完以后必须打印新标签,这里控制没意义
                                    //dialog.form.editors.qty.setReadonly(data.lot_in_qty)
                                });
                            },
                            submit(dialog) {
                                // 先保存
                                if (!dialog.form.valid()) {
                                    return jmaa.msg.error(dialog.form.getErrors());
                                }
                                let qty = dialog.form.editors.qty.getRawValue();
                                jmaa.rpc({
                                    model: me.model,
                                    module: me.module,
                                    method: 'scanMaterialCode',
                                    args: {
                                        ids: [form.dataId],
                                        code,
                                        qty
                                    },
                                    onerror(r) {
                                        jmaa.msg.error(r)
                                    },
                                    onsuccess: function (r) {
                                        jmaa.msg.show("操作成功".t())
                                        dialog.close();
                                        detailList.load();
                                        input.val('').focus();
                                    }
                                });
                            }
                        });
                    } else {
                        jmaa.msg.show('操作成功'.t());
                        detailList.load();
                        input.val('').focus();
                    }
                } else {
                    jmaa.msg.error('标签不存在'.t());
                }
            }
        });
    },
    loadDetailMethod(list, callback) {
        let me = this;
        let criteria = [];
        let fields = me.detail_ids.getFields()
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                ids: [me.pickForm.dataId],
                criteria,
                fields,
                limit: list.limit,
                offset: list.offset,
                relatedField: "detail_ids"
            },
            onerror(r) {
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
    deleteDetail(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'deleteDetail',
            args: {
                detailId: id
            },
            onerror(r) {
            },
            onsuccess: function (r) {
                me.detail_ids.load()
                jmaa.msg.show('操作成功'.t());
            }
        });
    },
    submitOrder() {
        let me = this;
        let pickTypeCheck = me.dom.find('[name=pickType]:checked').val();
        if (!pickTypeCheck) {
            jmaa.msg.error("请选择挑选类型".t())
            return
        }
        let pickType = "pick_stock";
        if ("false" == pickTypeCheck){
            pickType = "pick_return";
        }
        jmaa.msg.confirm({
            title: '确认提交?'.t(),
            content: '提交后,当前单据将不能再扫码,确认提交?'.t(),
            submit() {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'commit',
                    args: {
                        ids: [me.pickForm.dataId],
                        values:{"pick_type":pickType},
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t());
                        me.detail_ids.load()
                        // 控制按钮
                        me.pickForm.editors.status.setValue("done");
                        me.dom.find(".btn-submit-pick").attr("disabled", true);
                        setTimeout(function () {
                            me.dom.find('.delete-detail').hide();
                        }, 500);
                    }
                });
            },
        });
    },
    showDetailList() {
        let me = this;
        me.detail_ids.load();
    },
    onPickFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        if ('done' === status) {
            form.setReadonly(true);
        } else{
            me.dom.find(".btn-submit-pick").attr("disabled", false);
        }
    }
})
