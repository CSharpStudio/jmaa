//@ sourceURL=iqc_sheet_mobile.js
jmaa.view({
    commence() {
        let me = this;
        if (!me.sheetForm.valid()) {
            jmaa.msg.error(me.sheetForm.getErrors());
            return;
        }
        jmaa.showDialog({
            title: '开始检验'.t(),
            css: 'default',
            init: function (dialog) {
                dialog.form = me.createCommentForm(dialog);
                dialog.form.dom.enhanceWithin();
            },
            submit: function (dialog) {
                if (!dialog.form.valid()) {
                    jmaa.msg.error(dialog.form.getErrors())
                    return;
                }
                let data = dialog.form.getData();
                jmaa.rpc({
                    model: me.model,
                    method: "commence",
                    args: {
                        ids: [me.sheetForm.dataId],
                        comment: data.comment,
                    },
                    onsuccess: function (r) {
                        dialog.close();
                        me.loadSheet(me.sheetForm.dataId, () => {
                            me.itemList.load();
                            me.tabs.open('inspectItemTab');
                        });
                    }
                });
            }
        });
    },
    init() {
        let me = this;
        me.dirty = {};
        // 检验项目 查询条件切换
        me.dom.find('[name=inspectItemScope]').YiSelect();
        me.dom.find('[name=inspectItemScope],[name=inspectItemCategory]').change(() => {
            me.itemList.load();
        });
    },
    onItemListInit(e, list) {
        let me = this;
        let opts = list.fields.category.options;
        let html = [];
        for (let value of Object.keys(opts)) {
            html.push(`<option value='${value}'>${opts[value]}</option>`)
        }
        me.dom.find('[name=inspectItemCategory]').append(html.join('')).YiSelect();
    },
    searchList() {
        let me = this;
        me.iqcSheetList.load();
    },
    loadList(list, callback) {
        let me = this;
        let keyword = me.dom.find('.search-iqc-input').val();
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchList',
            args: {
                keyword,
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data});
            }
        });
    },
    openSheet() {
        let me = this;
        me.changePage("sheet");
        me.tabs.open('orderInfoTab');
        let iqcSheetId = me.iqcSheetList.getSelected()[0];
        me.loadSheet(iqcSheetId, () => {
            me.itemList.load();
            // wms送检明细
            if (me.iqcDetailsList) {
                me.iqcDetailsList.load();
            }
        });
    },
    loadSheet(iqcSheetId, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'read',
            args: {
                ids: [iqcSheetId],
                fields: me.sheetForm.getFields()
            },
            context: {
                usePresent: me.sheetForm.getUsePresent(),
            },
            onsuccess: function (r) {
                me.sheetForm.setData(r.data[0]);
                me.sheetForm.setReadonly(true);
                if (callback) {
                    callback();
                }
                // 更新按钮
                let buttons = me.dom.find("div[name='formButtons']");
                // 一键定性合格
                let setAllPassBtn = me.dom.find("button[name='setAllPass']");
                // 开始检验按钮
                let toInspectButton = me.dom.find("button[name='to-inspect']");
                if (!me.isCanUpdate()) {
                    toInspectButton.hide();
                    buttons.show().find("button").attr("disabled", true);
                    setAllPassBtn.attr("disabled", true);
                    me.sheetForm.setReadonly(true);
                } else if (me.isToInspect()) {
                    // 未开始检验状态
                    toInspectButton.show();
                    buttons.hide();
                    setAllPassBtn.hide();
                } else {
                    // 检验中
                    toInspectButton.hide();
                    buttons.show().find("button").attr("disabled", false);
                    setAllPassBtn.show().attr("disabled", false);
                    me.sheetForm.editors.attachments.setReadonly(false)
                }
            }
        });
    },
    loadItemList(list, callback) {
        let me = this;
        let criteria = [['sheet_id', '=', me.sheetForm.dataId]];
        let scope = me.tabs.dom.find('[name=inspectItemScope] option:selected').val();
        if (scope == 'undone') {
            criteria.push(['result', '=', null]);
        }
        let category = me.tabs.dom.find('[name=inspectItemCategory] option:selected').val();
        if (category != 'all') {
            criteria.push(['category', '=', category]);
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                id: [me.sheetForm.dataId],
                criteria: criteria,
                relatedField: 'inspection_item_ids',
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
                // 更新定性显示内容
                r.data.values.forEach(item => {
                    if (item.mark[0] === 'qual') {
                        let markIsQuanItems = me.itemList.dom.find("[class='markIsQuan-" + item.id + "']").parent();
                        markIsQuanItems.hide();
                    }
                })
                // 更新检查按钮显示
                if (!me.isCanUpdate() || me.isToInspect()) {
                    me.itemList.dom.find(".checkItemBtn").hide();
                }

            }
        });
    },
    // 一键定性合格
    setAllPass(e) {
        let me = this
        if (!me.canUpdate()) {
            return;
        }
        jmaa.rpc({
            model: "iqc.inspect_item_mobile",
            module: "iqc",
            method: 'allPass',
            args: {
                sheetId: me.sheetForm.dataId
            },
            onsuccess: function (r) {
                jmaa.msg.show('操作成功'.t())
                me.loadSheet(me.sheetForm.dataId, () => {
                    me.itemList.load();
                });
            }
        })
    },
    isCanUpdate() {
        let formData = this.sheetForm.getData();
        if (["inspected", "exempted", "done", "close"].includes(formData.status[0])) {
            return false;
        }
        return true;
    },
    // 当前是否为待检验
    isToInspect() {
        return "to-inspect" === this.sheetForm.getData().status[0];
    },
    //是否可以更新
    canUpdate() {
        if (!this.isCanUpdate()) {
            jmaa.msg.confirm({
                title: '提示'.t(),
                content: '已提交的检验单无法修改'.t(),
            });
            return fales;
        }
        return true;
    },
    inspect(e) {
        const me = this;
        if (!me.canUpdate()) {
            return;
        }
        // 父card 设置选中
        let btn = $(e.target);
        let id = btn.closest("[data-id]").attr('data-id');
        this.itemList.setSelected([id]);
        me.changePage('checkItem');
        me.handleInspectItemFormGridSelectData(me.waitCheckFrom, 0, me.itemList.getSelectedData()[0]);
    },
    // 上下切换检验表格选中数据
    handleInspectItemFormGridSelectData(waitCheckFrom, stepType, currentData) {
        let me = this;
        me.readInspectItemDetail(me.sheetForm.dataId, currentData.id, stepType, function (data) {
            if (!data.id) {
                jmaa.msg.confirm({
                    title: '提示'.t(),
                    content: '全部项目已检验，是否提交结果?'.t(),
                    submit() {
                        me.dom.find("#checkItemBackBtn").click();
                        me.tabs.open('orderInfoTab');
                        me.submit();
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
            waitCheckFrom.setData(dataValue);
            // 数据设置进入后才能有数据进行验证
            waitCheckFrom.editors.test_values.validate();
        });
    },
    // 上一个
    handleInspectItemFormGridPrev() {
        this.inspectItemDetailChangeItem(this.waitCheckFrom, -1);
    },
    // 下一个
    handleInspectItemFormGridNext() {
        this.inspectItemDetailChangeItem(this.waitCheckFrom, 1);
    },
    // 提交检验表格
    handleInspectItemFormGridConfirmed() {
        this.inspectItemDetailChangeItem(this.waitCheckFrom, null);
    },
    /**
     * 检验项目变更
     * @param waitCheckFrom
     * @param stepType -1上一个,1下一个 , null关闭
     */
    inspectItemDetailChangeItem(waitCheckFrom, stepType = 0) {
        if (stepType === 0) {
            return;
        }
        let me = this;
        const currentData = waitCheckFrom.getData();
        const getRes = me.changeInspectItemPageCheck(waitCheckFrom);
        if (!getRes.status) {
            if (getRes.message) {
                jmaa.msg.error(getRes.message);
            }
            return;
        }
        me.handleInspectItemFormGridSelectRequest(waitCheckFrom, getRes, stepType);
    },
    // 读取有效的测试值
    readTestValues: function (testValue) {
        if (!testValue) {
            return [];
        }
        return testValue.split(/[,;\n]/).map(v => v.trim()).filter(v => v);
    },
    // 检验当前数据并处理请求参数
    changeInspectItemPageCheck: function (waitCheckFrom) {
        const currentData = waitCheckFrom.getData();
        let result = null;
        if (currentData.ng_qty === 0) {
            result = 'ok'.t();
        } else if (currentData.ng_qty) {
            result = currentData.ng_qty >= Number(currentData.re) ? 'ng'.t() : 'ok'.t();
        }
        // 定性请求参数 处理
        if (currentData.mark[0] === 'qual') {
            const ngQtyEditors = waitCheckFrom.editors.ng_qty;
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
                    ids: [waitCheckFrom.dataId],
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
        const testValueEditors = waitCheckFrom.editors.test_values;
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
            ids: [waitCheckFrom.dataId],
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
    handleInspectItemFormGridSelectRequest(waitCheckFrom, getRes, stepType, callBackFun) {
        const me = this;
        this.updateInspectItem({
            args: getRes.data,
            callback: function () {
                const grid = me.itemList;
                // 修改表格数据
                const currentSelectData = grid.data.find(item => item.id === waitCheckFrom.dataId);
                if (currentSelectData) {
                    currentSelectData.ng_qty = this.args.values.ng_qty;
                    currentSelectData.remark = this.args.values.remark;
                    currentSelectData.result = this.args.values.result;
                    currentSelectData.test_values = waitCheckFrom.editors.test_values.getValue();
                    let card = grid.dom.find("[data-id='" + currentSelectData.id + "']");
                    card.find("#ngQty-" + currentSelectData.id).text(currentSelectData.ng_qty);
                    card.find("#remark-" + currentSelectData.id).text(currentSelectData.remark);
                    card.find("#testValues-" + currentSelectData.id).text(currentSelectData.test_values);
                    let radios = card.find("[result-radio='" + currentSelectData.id + "']").find("input:radio");
                    radios.removeAttr('checked');
                    if (currentSelectData.result) {
                        card.find("[result-radio='" + currentSelectData.id + "'] > [value='" + currentSelectData.result + "']").prop('checked', 'true');
                    }
                }
                // 步法值为null就是关闭检查窗口
                if (stepType == null) {
                    if (callBackFun) {
                        callBackFun();
                    }
                } else {
                    // 上下切换 校验表格 选中数据
                    me.handleInspectItemFormGridSelectData(waitCheckFrom, stepType, waitCheckFrom.getData());
                }

            },
        });
    },
    // 返回检查项目
    backCheckInspectItemTab() {
        let me = this;
        me.loadSheet(me.sheetForm.dataId);
        me.tabs.open('inspectItemTab');
    },
    // 检查记录更新
    updateInspectItem: function (data) {
        jmaa.rpc({
            model: 'iqc.inspect_item_mobile',
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
            model: "iqc.inspect_item_mobile",
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
    save() {
        let me = this;
        if (!me.canUpdate()) {
            return;
        }
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'update',
            args: {
                ids: [me.sheetForm.dataId],
                values: me.dirty,
            },
            onsuccess: function (r) {
                me.itemList.load();
                jmaa.msg.show("操作成功".t());
            }
        });
    },
    submit() {
        let me = this;
        if (!me.canUpdate()) {
            return;
        }
        jmaa.showDialog({
            title: '提交'.t(),
            css: 'default',
            init: function (dialog) {
                dialog.form = me.createCommentForm(dialog);
                dialog.form.dom.enhanceWithin();
            },
            submit: function (dialog) {
                let data = dialog.form.getData();
                dialog.busy();
                jmaa.rpc({
                    model: "iqc.sheet_mobile",
                    module: "iqc",
                    method: 'commit',
                    args: {
                        ids: [me.sheetForm.dataId],
                        comment: data.comment,
                    },
                    onerror: function (e) {
                        dialog.busy(false);
                        jmaa.msg.error(e);
                    },
                    onsuccess: function (r) {
                        jmaa.msg.show('操作成功'.t());
                        dialog.close();
                        me.loadSheet(me.sheetForm.dataId, () => {
                            me.itemList.load();
                        });
                    }
                });
            }
        });

    },
    // 创建提交提示输入框
    createCommentForm(dialog, required) {
        return dialog.body.JForm({
            cols: 1,
            fields: {
                comment: {name: 'comment', type: 'text', label: '备注', required}
            },
            arch: `<form><field name="comment"></field></form>`
        });
    },
    // 加载送检明细
    loadDetailsList(list, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'searchByField',
            args: {
                id: [me.sheetForm.dataId],
                criteria: [['iqc_id', '=', me.sheetForm.dataId]],
                relatedField: 'details_ids',
                limit: list.limit,
                offset: list.offset,
                fields: list.getFields()
            },
            context: {
                usePresent: list.getUsePresent(),
            },
            onsuccess: function (r) {
                callback({data: r.data.values});
            }
        });
    },
});
jmaa.render('markIsQuan', function (data, row) {
    return `<div class="markIsQuan-${row.id}">${data}</div>`;
});
jmaa.render('result', function (data, row) {
    let id = jmaa.nextId();
    return `<div id="result-${id}" class="yi-radio" result-radio="${row.id}">
                <input id="result-${id}-ok" item-id="${row.id}" field="result" type="radio" name="result-${id}" value="ok"${data[0] == 'ok' ? ' checked="checked"' : ''} disabled="disabled">
                <label for="result-${id}-ok" class="ok">${'合格'.t()}</label>
                <input id="result-${id}-ng" item-id="${row.id}" field="result" type="radio" name="result-${id}" value="ng"${data[0] == 'ng' ? ' checked="checked"' : ''} disabled="disabled">
                <label for="result-${id}-ng" class="ng">${'不合格'.t()}</label>
           </div>`
});

jmaa.render('testValues', function (data, row) {
    return `<div id="testValues-${row.id}" class="markIsQuan-${row.id}">${data == null ? "" : data}</div>`
});
jmaa.render('ngQty', function (data, row) {
    return `<div id="ngQty-${row.id}">${data == null ? "" : data}</div>`
});
jmaa.render('remark', function (data, row) {
    return `<div id="remark-${row.id}">${data == null ? "" : data}</div>`
});
