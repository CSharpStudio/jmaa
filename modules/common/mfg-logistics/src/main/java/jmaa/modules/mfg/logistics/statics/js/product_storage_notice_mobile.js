//@ sourceURL=product_storage_notice_mobile.js
jmaa.view({
    init() {
        let me = this;
        me.mediaScan = new Audio('/web/jmaa/modules/md/enterprise/statics/media/scan.mp3');
        me.mediaSubmit = new Audio('/web/jmaa/modules/md/enterprise/statics/media/submit.mp3');
        me.mediaError = new Audio('/web/jmaa/modules/md/enterprise/statics/media/error.mp3');
        me.dom.find('[name=scope]').on('change', function () {
            me.detailsList.load();
        });
        me.orderList.load();
    },
    onOrderFormLoad(e, form) {
        let me = this;
        // 控制按钮
    },
    searchOrder() {
        let me = this;
        me.orderList.load();
    },
    resetSearchOrder() {
        let me = this;
        me.dom.find('.search-product-input').val('');
        me.orderList.load();
    },
    loadOrderList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-product-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchProductOrder',
            args: {
                keyword,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                order: 'code asc',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data});
            }
        });
    },
    createOrder() {
        let me = this;
        jmaa.showDialog({
            title: '创建成品入库通知单'.t(),
            init(dialog) {
                me.loadView(me.model, 'form').then(v => {
                    dialog.form = dialog.body.JForm({
                        model: me.model,
                        module: me.module,
                        view: me,
                        fields: v.fields,
                        arch: v.views.form.arch,
                    });
                    dialog.form.dom.enhanceWithin();
                    dialog.form.create();
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'create',
                    args: data,
                    onsuccess(r) {
                        dialog.close();
                        me.orderList.load();
                    }
                })
            }
        });
    },
    openDetails() {
        let me = this;
        let data = me.orderList.getSelectedData()[0];
        me.orderForm.dataId = data.id;
        me.orderForm.setData(data);
        me.orderForm.editors.result.reset();
        me.dom.find('.label-input').val('')
        me.changePage("details");
    },
    scanCode() {
        let me = this;
        let form = me.orderForm;
        let input = me.dom.find('.label-input');
        let code = input.val();
        let submit = me.dom.find('#check-auto').is(":checked");
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'scanCode',
            args: {
                ids: [me.orderForm.dataId],
                code,
                submit,
            },
            onerror(r) {
                me.mediaError.play();
                input.val('');
                jmaa.msg.error(r);
                if (r.code == 1000) {
                    form.getData().result = {error: true, msg: r.message};
                }
            },
            onsuccess: function (r) {
                form.getData().result = {msg: r.data.message};
                if (submit) {
                    input.val('');
                    form.editors.scan_qty.setValue(r.data.scan_qty)
                    me.mediaSubmit.play();
                } else {
                    // 手动确认  弹框
                    me.mediaScan.play();
                    me.confirmSubmit(code, r.data.data);
                }
            }
        });
    },
    confirmSubmit(code, data) {
        let me = this;
        jmaa.showDialog({
            title: '确认入库'.t(),
            init(dialog) {
                dialog.form = dialog.body.JForm({
                    fields: me.fields,
                    arch: `<form>
                        <div class="card-view">
                            <editor type="card-item" name="sn" label="条码" colspan="2"></editor>
                            <editor type="card-item" name="material_id" label="物料编码" colspan="2"></editor>
                            <editor type="card-item" name="material_name_spec" label="规格名称" colspan="2"></editor>
                            <editor type="card-item" name="qty" label="数量" colspan="2"></editor>
                        </div>
                    </form>`,
                    model: me.model
                });
                dialog.form.dom.enhanceWithin();
                dialog.form.setData(data);
            },
            submit(dialog) {
                let form = me.orderForm;
                let result = form.editors.result;
                let data = dialog.form.getRaw();
                let input = me.dom.find('.label-input');
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'scanCode',
                    args: {
                        ids: [form.dataId],
                        code,
                        submit: true,
                    },
                    onerror(r) {
                        me.mediaError.play();
                        if (r.code == 1000) {
                            jmaa.msg.error(r);
                            result.setValue({error: true, msg: r.message});
                        } else {
                            jmaa.msg.error(r);
                        }
                    },
                    onsuccess: function (r) {
                        result.setValue({msg: r.data.message});
                        dialog.close();
                        input.val('');
                        form.editors.scan_qty.setValue(r.data.scan_qty);
                        me.mediaSubmit.play();
                    }
                });
            }
        });
    },
    showDetailsList() {
        let me = this;
        me.detailsList.load();
    },
    loadDetailsList(list, callback) {
        let me = this;
        let criteria = [['product_storage_notice_id', '=', me.orderList.getSelected()[0]]];
        let scope = me.dom.find('[name=scope]:checked').val();
        if (scope === 'undone') {
            criteria.push(['status', '=', 'new']);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                criteria,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields(),
                relatedField: 'details_ids',
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values})
                if (scope === 'undone') {
                    me.dom.find('.delete-class').parent().show();
                } else {
                    me.dom.find('.delete-class').parent().hide();
                }
            }
        });
    },
    deleteDetails(e) {
        let me = this;
        let id = $(e.target).closest('[data-id]').attr('data-id')
        jmaa.msg.confirm({
            title: '删除'.t(),
            content: '确认删除此标签?'.t(),
            submit() {
                jmaa.rpc({
                    model: "mfg.product_storage_notice_details",
                    module: me.module,
                    method: 'deleteDetails',
                    args: {
                        ids: [id],
                    },
                    onerror(r) {
                        jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        me.detailsList.load();
                        me.orderForm.editors.scan_qty.setValue(r.data)
                    }
                });
            },
        });
    },
    submitOrder() {
        let me = this;
        let form = me.orderForm
        jmaa.showDialog({
            title: "生成入库单".t(),
            init: function (dialog) {
                dialog.form = dialog.body.JForm({
                    arch: `<form cols="2">
                                <editor label="备注" type="text" name="comment" colspan="2" />
                            </form>`
                });
                dialog.form.dom.enhanceWithin();
            },
            submit: function (dialog) {
                let data = dialog.form.getData();
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'commit',
                    args: {
                        ids: [form.dataId],
                        comment: data.comment,
                    },
                    onerror(r) {
                        return jmaa.msg.error(r);
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t())
                        me.detailsList.load();
                        me.orderForm.editors.status.setValue('done')
                        dialog.close()
                    }
                });
            },
        })
    },
})
