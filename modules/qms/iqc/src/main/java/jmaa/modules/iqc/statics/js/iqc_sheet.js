//@ sourceURL=iqc_sheet.js
jmaa.view({
    onSpecChange() {
        let me = this;
        if (me.form.dataId) {
            jmaa.msg.confirm({
                title: '提示'.t(),
                content: '检验标准已修改，需要手动删除检验项目才会自动生成新检验项目'.t()
            });
        }
    },
    commence(e, target) {
        let me = this;
        if (!me.form.valid()) {
            return jmaa.msg.error(me.form.getErrors());
        }
        jmaa.showDialog({
            title: "开始检验".t(),
            css: 'default',
            init: function (dialog) {
                dialog.form = dialog.body.JForm({
                    cols: 1,
                    arch: `<form><editor type="text" name="comment" label="备注"></editor></form>`
                });
            },
            submit: function (dialog) {
                if (!dialog.form.valid()) {
                    jmaa.msg.error(dialog.form.getErrors())
                    return;
                }
                let data = dialog.form.getData();
                let d = {...me.form.getSubmitData()};
                delete d.id;
                jmaa.rpc({
                    model: me.model,
                    method: "commence",
                    args: {
                        ids: target.getSelected(),
                        comment: data.comment,
                        values: d
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                        me.load();
                    }
                });
            }
        });
    },
    getItemChangeContext() {
        let me = this;
        let qty = me.form.getRawData("qty");
        return {inspectionQty: qty};
    },
    onFormLoad(e, form) {
        let me = this;
        me.callSuper(e, form);
        let data = form.getData();
        let editButtons = form.editors.inspection_item_ids.dom.find('button.btn-edit-group');
        let inspectButtons = form.editors.inspection_item_ids.dom.find('button.btn-inspect-group');
        if (data.status === 'to-inspect') {
            editButtons.show();
        } else {
            editButtons.hide();
        }
        if (data.status === 'inspecting') {
            inspectButtons.show();
        } else {
            inspectButtons.hide();
        }
        if (data.status === "done") {
            form.setReadonly(true);
        }
    },
    // 检查项目初始化数据
    onInspectionItemLoad(e, target) {
        let me = this;
        target.data.forEach(function (item) {
            return Object.assign(item, {
                need_test_qty: Number(item.sample_size) - me.readTestValues(item ? item.test_values : null).length
            });
        });
    },
    onInspectionEditLoad(e, target) {
        let editForm = target.editForm;
        let data = editForm.getRawData();
        let required = data.mark && data.mark === 'quan';
        editForm.setEditorRequired("limit_lower", required);
        editForm.setEditorRequired("limit_upper", required);
    },
    onRowDblClick(e, grid) {
        let me = this;
        let data = me.form.getData();
        if (data.status === 'to-inspect') {
            grid.edit();
        } else if (data.status === 'inspecting') {
            this.inspect(e, grid);
        }
    },
    setAllPass(e, target) {
        let me = this
        jmaa.rpc({
            model: target.model,
            module: target.module,
            method: 'allPass',
            args: {
                sheetId: me.form.dataId
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                target.owner.load();
            }
        })
    },
    inspect(e, target) {
        let formData = target.owner.owner.getData();
        if (["inspected", "exempted", "done"].includes(formData.status)) {
            jmaa.msg.confirm({
                title: '提示'.t(),
                content: '已提交的检验单无法修改'.t(),
            });
            return;
        }
        const me = this;
        jmaa.showDialog({
            title: "检验",
            init(dialog) {
                me.loadView('iqc.inspect_item', 'form', 'iqc_inspect_dialog').then(v => {
                    dialog.form = dialog.body.JForm({
                        arch: v.views.form.arch, fields: v.fields, module: v.module, model: v.model, view: me,
                    });
                    dialog.dom.find('.buttons-right')
                        .html(`<button type="button" t-click="prevItem" class="btn btn-default">${'上一个'.t()}</button>
                            <button type="button" t-click="nextItem" class="btn btn-default">${'下一个'.t()}</button>
                            <button type="button" t-click="saveItem" class="btn btn-blue btn-print">${'保存'.t()}</button>`);
                    me.handleInspectItemFormGridSelectData(dialog, 0, target.getSelectedData()[0]);
                });
            },
            prevItem(e, dialog) {
                me.inspectItemDetailChangeItem(dialog, -1);
            },
            nextItem(e, dialog) {
                me.inspectItemDetailChangeItem(dialog, 1);
            },
            saveItem(e, dialog) {
                me.inspectItemDetailChangeItem(dialog, null);
            }
        })
    },
    /**
     * 检验项目变更
     * @param dialog
     * @param stepType -1上一个,1下一个, null关闭
     */
    inspectItemDetailChangeItem(dialog, stepType = 0) {
        if (stepType === 0) {
            return;
        }
        let me = this;
        const currentData = dialog.form.getData();
        const getRes = me.changeInspectItemPageCheck(dialog.form);
        if (!getRes.status) {
            if (getRes.message) {
                jmaa.msg.error(getRes.message);
            }
            return;
        }
        me.handleInspectItemFormGridSelectRequest(dialog, getRes, stepType);
    },
    // 读取有效的测试值
    readTestValues: function (testValue) {
        if (!testValue) {
            return [];
        }
        return testValue.split(/[,;\n]/).map(v => v.trim()).filter(v => v);
    },
    // 检验当前数据并处理请求参数
    changeInspectItemPageCheck: function (dialogForm) {
        const currentData = dialogForm.getData();
        let result = null;
        if (currentData.ng_qty === 0) {
            result = 'ok'.t();
        } else if (currentData.ng_qty) {
            result = currentData.ng_qty >= Number(currentData.re) ? 'ng'.t() : 'ok'.t();
        }
        // 定性请求参数 处理
        if (currentData.mark[0] === 'qual') {
            const ngQtyEditors = dialogForm.editors.ng_qty;
            // 校验定性录入输入不良数
            const checkRes = this.checkQty(ngQtyEditors.field.label, currentData.ng_qty);
            if (checkRes.status) {
                if (currentData.ng_qty > currentData.sample_size) {
                    return {
                        status: false,
                        message: '不良数不能大于样本数量！'.t(),
                    };
                }
            }
            return {
                data: {
                    ids: [dialogForm.dataId],
                    values: {
                        remark: currentData.remark,
                        ng_qty: currentData.ng_qty,
                        result: result
                    },
                },
                status: checkRes.status,
                message: checkRes.message,
            };
        }
        // 剩下都是 定量录入 逻辑处理
        const testValueEditors = dialogForm.editors.test_values;
        const testValueArr = this.readTestValues(currentData.test_values);
        const realTestValueArr = [];
        let ng_qty = 0;
        for (const testValue of testValueArr) {
            if (testValue) {
                const checkRes = this.checkQty(testValueEditors.field.label, testValue, false);
                if (!checkRes.status) {
                    return {
                        status: false,
                        message: checkRes.message,
                    };
                }
                realTestValueArr.push(testValue);
                let testValueReal = parseFloat(testValue);
                if (testValueReal < currentData.limit_lower || testValueReal > currentData.limit_upper) {
                    ng_qty++;
                }
            }
        }
        currentData.ng_qty = ng_qty;
        currentData.need_test_qty = currentData.sample_size - realTestValueArr.length;
        if (realTestValueArr.length > currentData.sample_size) {
            return {
                status: false,
                message: '测试值数量大于样本数量！'.t(),
            };
        }
        const data = {
            ids: [dialogForm.dataId],
            values: {
                remark: currentData.remark,
                ng_qty: currentData.ng_qty,
                test_values: currentData.test_values,
                result: currentData.ng_qty >= currentData.re || realTestValueArr.length === currentData.sample_size ? result : null,
            },
        };
        return {
            status: true,
            message: '定量录入数据校验正确！'.t(),
            data,
        };
    },
    // 检验数据是否为大于0整数
    checkQty(label, qty, isDecimals = true) {
        const newQty = Number(qty);
        if (Number.isNaN(newQty)) {
            return {
                status: false,
                message: `${'当前'.t()}${label}[${qty}]${'不是合法的数字'.t()}`,
            };
        }
        if (isDecimals && Number(newQty) !== parseInt(newQty)) {
            return {
                status: false,
                message: `${'当前'.t()}${label}[${qty}]${'不能为小数'.t()}`,
            };
        }
        if (newQty < 0) {
            return {
                status: false,
                message: `${'当前'.t()}${label}[${qty}]${'必须为大于0的整数'.t()}`,
            };
        }
        return {
            status: true,
        };
    },
    // 更新检验项目结果
    updateResult(e, target) {
        this.changeInspectItemPageCheck(target.owner);
        // 重新计算结果，防止计算错误
        let currentData = target.owner.getRawData();
        let result = null;
        if (currentData.ng_qty === 0) {
            result = 'ok'.t();
        } else if (currentData.ng_qty) {
            result = currentData.ng_qty >= Number(currentData.re) ? 'ng'.t() : 'ok'.t();
        }
        if (currentData.mark === 'quan') {
            result = currentData.ng_qty >= currentData.re || currentData.need_test_qty === 0 ? result : null
        }
        target.owner.editors.result.setValue(result);
    },
    // 处理 上下切换 校验表格 请求, stepType 为null时只保存
    handleInspectItemFormGridSelectRequest(dialog, getRes, stepType) {
        const me = this;
        this.updateInspectItem({
            args: getRes.data,
            callback: function () {
                const formEditors = me.form.editors;
                const grid = formEditors.inspection_item_ids.grid;
                // 修改表格数据
                const currentSelectData = grid.data.find(item => item.id === dialog.form.dataId);
                if (currentSelectData) {
                    currentSelectData.ng_qty = this.args.values.ng_qty;
                    currentSelectData.remark = this.args.values.remark;
                    currentSelectData.result = this.args.values.result;
                    currentSelectData.test_values = dialog.form.editors.test_values.getValue();
                }
                grid.view.load();
                if (stepType == null) {
                    // dialog.close();
                } else {
                    // 上下切换 校验表格 选中数据
                    me.handleInspectItemFormGridSelectData(dialog, stepType, dialog.form.getData());
                }

            },
        });
    },
    // 上下切换检验表格选中数据
    handleInspectItemFormGridSelectData(dialog, stepType, currentData) {
        let me = this;
        me.readInspectItemDetail(me.form.dataId, currentData.id, stepType, function (data) {
            if (!data.id) {
                jmaa.msg.confirm({
                    title: '提示'.t(),
                    content: '全部项目已检验，是否提交结果?'.t(),
                    submit() {
                        me.commitOrder(null, me.form);
                    }
                });
                return;
            }
            const test_values = me.readTestValues(data.test_values);
            let dataValue = {
                ...data,
                need_test_qty: Number(data.sample_size) - test_values.length,
                test_values: data.test_values
            };
            // 设置表单参数
            dialog.form.setData(dataValue);
            // 数据设置进入后才能有数据进行验证
            dialog.form.editors.test_values.validate();
        });

    },
    // 检查记录更新
    updateInspectItem: function (data) {
        jmaa.rpc({
            model: 'iqc.inspect_item',
            module: 'iqc',
            method: 'update',
            args: data.args || {},
            content: {
                useParams: true,
            },
            onsuccess: function (r) {
                if (data.callback) {
                    data.callback();
                }
            },
        });
    },
    /**
     * 查询记录
     * @param sheetId   检验表单id
     * @param itemId    检验项id
     * @param stepType  检验类型 -1上一个，0当前，1下一个
     * @param callback  异步回调方法
     */
    readInspectItemDetail(sheetId, itemId, stepType, callback) {
        jmaa.rpc({
            model: "iqc.inspect_item",
            module: "iqc",
            method: 'readInspectItemDetail',
            args: {
                sheetId,
                itemId,
                stepType
            },
            onsuccess: function (r) {
                callback(r.data);
            }
        });
    },
});
