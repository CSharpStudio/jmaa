jmaa.component('JImportXls', {
    previewMaxRows: 100,
    sheet2JSONOpts: {
        defval: '',
        raw: true,
        header: 'A'
    },
    parents: true, // 是否支持配置父id列
    getTpl() {
        return `<div class="import-xls m-3">
                    <div class="import-file">
                        <input type="file" class="import-file-input" id="customFile"/>
                        <label class="import-file-label" for="customFile"><span class="import-file-name">${'选择文件'.t()}</span><div class="import-file-btn">选择</div></label>
                    </div>
                    <div class="justify-content-between d-flex m-1 mb-3">
                        <div class="d-flex align-items-center">
                            <div class="e-check">
                                <input id="hasHeader" type="checkbox" checked="checked">
                                <label class="right" for="hasHeader">包含表头</label>
                            </div>
                            <div class="head-rows-panel e-form">
                                <input type="number" class="header-rows form-control" min="1" max="10" value="1"/>
                                <span>行</span>
                            </div>
                        </div>
                        <div><a href="#" onclick="return false;" class="btn-down-tpl">下载模板</a></div>
                    </div>
                    <ul class="nav nav-tabs" id="sheet-tab" role="tablist">
                    </ul>
                    <div class="data-preview"></div>
                </div>`;
    },
    init() {
        const me = this;
        me.parentField = me.target?.owner?.field.inverseName;
        me.parentId = me.target?.owner?.owner?.dataId;
        me.wizard = jmaa.showDialog({
            id: 'import-dialog',
            title: '导入'.t(),
            init: function (dialog) {
                me.initWizard(dialog);
            },
            submit: function (dialog) {
                dialog.busy(true);
                me.uploadData();
            },
        });
    },
    getFieldMap() {
        const me = this;
        const map = {};
        const select = me.wizard.body.find('.x-field select');
        for (let i = 0; i < select.length; i++) {
            if (select[i].value) {
                map[select[i].value] = i;
            }
        }
        return map;
    },
    checkFieldMap() {
        const me = this;
        const map = [];
        const duplicate = [];
        const select = me.wizard.body.find('.x-field select');
        for (let i = 0; i < select.length; i++) {
            const item = select[i];
            const value = item.value;
            if (value) {
                if (map.includes(value)) {
                    const text = item.options[item.selectedIndex].text;
                    if (!duplicate.includes(text)) {
                        duplicate.push(text);
                    }
                } else {
                    map.push(value);
                }
            }
        }
        if (map.length == 0) {
            jmaa.msg.error('至少配置一列字段映射，请检查配置'.t());
            return false;
        }
        if (duplicate.length > 0) {
            jmaa.msg.error(duplicate.join('，').toString() + '重复，请检查导入配置'.t());
            return false;
        }
        return true;
    },
    getUploadData() {
        const me = this;
        const formData = new FormData();
        const hasHeader = me.wizard.body.find('#hasHeader').is(':checked');
        const opt = {
            model: me.model,
            sheetIndex: me.sheetIndex,
            headRow: hasHeader ? me.wizard.body.find('.header-rows').val() : 0,
            fields: me.getFieldMap(),
            parentField: me.parentField,
            parentId: me.parentId,
        };
        if (me.importer) {
            opt.importer = me.importer;
        }
        formData.set('options', JSON.stringify(opt));
        formData.set('file', me.file);
        return formData;
    },
    uploadData() {
        const me = this;
        if (!me.checkFieldMap()) {
            me.wizard.busy(false);
            return;
        }
        const formData = me.getUploadData();
        if (!formData) {
            return;
        }
        $.ajax({
            url: jmaa.web.getTenantPath() + '/importXls',
            type: 'POST',
            data: formData,
            contentType: false,
            processData: false,
            success: function (data) {
                me.wizard.busy(false);
                if (data.success) {
                    if (me.callback) {
                        me.callback();
                    }
                    if (data.message == "async") {
                        top.window.app.upload.add();
                        jmaa.msg.show("正在后台导入".t(), {delay: 5000});
                    } else {
                        jmaa.msg.show(data.message, {delay: 5000});
                    }
                    me.wizard.close();
                } else {
                    jmaa.msg.error(data, {delay: 50000});
                }
            },
            error: function (data) {
                me.wizard.busy(false);
                console.log(data);
                jmaa.msg.error('发生错误:' + data);
            },
        });
    },
    initWizard(dialog) {
        const me = this;
        dialog.body
            .html(me.getTpl())
            .on('change', '[type=file]', function (e) {
                me.fileChange(e);
            })
            .on('change', '.header-rows', function (e, a) {
                const value = parseInt($(this).val());
                $(this).val(isNaN(value) ? 1 : value);
                me.showSheet(me.sheetIndex);
            })
            .on('change', '#hasHeader', function () {
                if ($(this).is(':checked')) {
                    dialog.body.find('.head-rows-panel').show();
                } else {
                    dialog.body.find('.head-rows-panel').hide();
                }
                me.showSheet(me.sheetIndex);
            })
            .on('change', 'select', function () {
                const select = $(this);
                select.css('color', select.val() ? 'black' : 'red');
            });

        dialog.body.find('.header-rows').inputSpinner({
            // the template of the input
            template: `<div class="mr-2">
                            <button style="display: none" class="btn-decrement" type="button"></button>
                            <button style="display: none" class="btn-increment" type="button"></button>
                            <input type="text" inputmode="decimal" class="rows-input"/>
                        </div>`,
        });
        dialog.body.on('click', '.sheet-link', function () {
            me.showSheet($(this).attr('data-sheet'));
        });
        dialog.dom.find('[role=btn-submit]').attr('disabled', true);
        dialog.dom.find('.btn-down-tpl').on('click', function (e) {
            me.downTemplate();
            e.preventDefault();
            return true;
        });
    },
    downTemplate() {
        let me = this;
        let fields = me.filterFields(Object.values(me.fields));
        let fieldList = [];
        let fieldName = [];
        let usePresent = [];
        for (let f of fields) {
            if (f.required) {
                fieldList.push(f);
                fieldName.push(f.name);
            }
            if (['many2one', 'many2many'].includes(f.type)) {
                usePresent.push(f.name);
            }
        }
        for (let f of fields) {
            if (!f.required) {
                fieldList.push(f);
                fieldName.push(f.name);
            }
        }
        let downXls = function (demo) {
            let data = [[], []];
            for (let i = 0; i < demo.length; i++) {
                data.push([]);
            }
            for (const field of fieldList) {
                let title = field.label;
                if (field.required && field.name != me.parentField) {
                    title += '(必填)';
                }
                data[0].push(title);
                let value = '';
                if (field.type === 'char' || field.type === 'text' || field.type === 'many2one') {
                    value = '字符文本';
                } else if (field.type === 'integer') {
                    value = 1;
                } else if (field.type === 'float') {
                    value = 0.1;
                } else if (field.type === 'boolean') {
                    value = true;
                } else if (field.type === 'selection') {
                    value = Object.values(field.options).join('/');
                } else if (field.type === 'date') {
                    value = moment().format('yyyy-MM-DD');
                } else if (field.type === 'datetime') {
                    value = moment().format('yyyy-MM-DD HH:mm:ss');
                }
                data[1].push(value);
                for (let i = 0; i < demo.length; i++) {
                    let value = demo[i][field.name];
                    if (field.type === 'selection') {
                        value = field.options[value] || value;
                    } else if (field.type === 'many2one') {
                        value = value ? value[1] : '';
                    } else if (field.type === 'many2many') {
                        let v = [];
                        for (let r of value || []) {
                            v.push(r[1]);
                        }
                        value = v.join();
                    }
                    data[i + 2].push(value);
                }
            }
            let wb = XLSX.utils.book_new();
            let ws = XLSX.utils.aoa_to_sheet(data);
            XLSX.utils.book_append_sheet(wb, ws, "Sheet1");
            XLSX.writeFile(wb, me.model + "-template.xlsx", {numbers: 1, compression: true});
        }
        jmaa.rpc({
            model: me.model,
            method: 'search',
            args: {
                fields: fieldName,
                limit: 5,
                criteria: []
            },
            context: {
                usePresent,
            },
            onsuccess(r) {
                downXls(r.data.values);
            },
            onerror() {
                downXls([]);
            },
        })
    },
    fileChange(event) {
        const me = this;
        const fileReader = new FileReader();
        const sheets = me.wizard.body.find('#sheet-tab');
        const preview = me.wizard.body.find('.data-preview');
        preview.empty();
        sheets.empty();
        me.workbook = null;
        me.file = event.target.files[0];
        let fileName = event.target.value
        event.target.value = '';
        if (me.file) {
            me.wizard.dom.find('[role=btn-submit]').attr('disabled', false);
            me.wizard.dom.find('.import-file-name').text(fileName);
            fileReader.onload = function (e) {
                const data = e.target.result;
                me.workbook = XLSX.read(data, {type: 'binary', cellDates: true});
                const sheetNum = me.workbook.SheetNames.length;
                for (let i = 0; i < sheetNum; i++) {
                    sheets.append(`<li class="tab-head">
                                        <a class="sheet-link nav-link ${i == 0 ? 'active' : ''}"
                                           data-toggle="pill"
                                           data-sheet="${i}"
                                           href="#"
                                           role="tab"
                                           aria-selected="true">${me.workbook.SheetNames[i]}</a>
                                    </li>`);
                }
                if (sheetNum > 0) {
                    me.showSheet(0);
                }
            };
            fileReader.readAsArrayBuffer(me.file);
        } else {
            me.wizard.dom.find('[role=btn-submit]').attr('disabled', true);
            me.wizard.dom.find('.import-file-name').text('选择文件'.t());
        }
    },
    getHeader(end) {
        const header = [];
        if (end.length == 1) {
            for (let a = 65; a <= end.charCodeAt(0); a++) {
                header.push(String.fromCharCode(a));
            }
        }
        if (end.length == 2) {
            for (let a = 65; a <= 90; a++) {
                header.push(String.fromCharCode(a));
            }
            for (let a = 65; a < end[0].charCodeAt(0); a++) {
                for (let b = 65; b <= 90; b++) {
                    header.push(String.fromCharCode(a) + String.fromCharCode(b));
                }
            }
            for (let b = 65; b <= end[1].charCodeAt(0); b++) {
                header.push(end[0] + String.fromCharCode(b));
            }
        }
        return header;
    },
    filterFields(fields) {
        const me = this;
        const list = [];
        const ignore = ['id', 'create_uid', 'create_date', 'update_uid', 'update_date'];
        if (!me.parents && me.parentField) {
            ignore.push(me.parentField); // 不支持配置父id列
        }
        for (const field of fields) {
            if (ignore.indexOf(field.name) == -1 && field.store && field.type !== 'one2many') {
                list.push(field);
            }
        }
        return list;
    },
    showSheet(index) {
        const me = this;
        const preview = me.wizard.body.find('.data-preview');
        preview.empty();
        me.sheetIndex = index;
        if (!me.workbook) {
            return;
        }
        const sheetData = me.workbook.Sheets[me.workbook.SheetNames[index]];
        const rows = XLSX.utils.sheet_to_json(sheetData, me.sheet2JSONOpts);
        if (rows.length == 0) {
            return;
        }
        const cols = [];
        let thead = '<th class="x-cell x-row-head"></th>';
        let tbody = `<td class='x-cell x-row-head'></td>`;
        let tfoot = '';
        const fields = Object.values(me.fields).sort(function (x, y) {
            if (x.required != y.required) {
                return y.required - x.required;
            }
            return x.label.localeCompare(y.label, 'zh');
        });
        const totalWidth = preview.width() - 37;
        const colWidth = (totalWidth / Object.keys(rows[0]).length).toFixed(2);
        let start = -1;
        if (me.wizard.body.find('#hasHeader').is(':checked')) {
            start = me.wizard.body.find('.header-rows').val() - 1;
        }
        if (start > rows.length - 1) {
            return;
        }
        for (const col in rows[0]) {
            if (col !== '__rowNum__') {
                let colName = start > -1 ? `${rows[start][col]}` : col;
                thead += `<th class='x-cell' style="min-width: ${colWidth}px">${colName}${start > -1 ? ` (${col})` : ''}</th>`;
                cols.push(col);
                let fieldSelect = `<option value="" style="color:gray">${'请选择字段映射'.t()}</option>`;
                let selectedCss = "style='color:red'";
                for (const field of me.filterFields(fields)) {
                    const checked = colName == field.name || colName == field.label || colName == (field.label + '(必填)');
                    fieldSelect += `<option value="${field.name}" ${checked ? 'selected="true"' : ''} ${field.name == me.parentField ? 'title="不指定时,默认为当前父表数据"' : ''}
                                                class="${field.required && field.name != me.parentField ? 'required-field' : ''}">
                                            ${field.label} [${field.name}]${field.name == me.parentField ? ' (父表列)' : ''}
                                        </option>`;
                    if (checked) {
                        selectedCss = '';
                    }
                }
                tbody += `<td class='x-cell x-field' style="min-width: ${colWidth}px"><select ${selectedCss}>${fieldSelect}</select></td>`;
            }
        }
        for (let i = start + 1; i < Math.min(me.previewMaxRows, rows.length); i++) {
            tbody += `<tr class='x-row'><td class='x-cell x-row-head' >${i + 1}</td>`;
            for (const col of cols) {
                let value = rows[i][col];
                if (value instanceof Object) {
                    value = value.toLocaleDateString();
                }
                tbody += `<td class='x-cell' style="min-width: ${colWidth}px">${value}</td>`;
            }
            tbody += '</tr>';
        }
        if (rows.length > me.previewMaxRows) {
            tfoot = `<tr><td colspan="${cols.length + 1}">总共${rows.length}行,还有${rows.length - me.previewMaxRows}行未显示</td></tr>`;
        }
        preview.html(`<table id="data-table" class="x-table">
            <thead>${thead}</thead>
            <tbody>${tbody}</tbody>
            <tfoot>${tfoot}</tfoot>
        </table>`);
    },
});
