//@ sourceURL=scheduler.js
jmaa.view({
    editPermission() {
        let me = this;
        jmaa.showDialog({
            title: '编辑权限'.t(),
            css: 'modal-md',
            init(dialog) {
                dialog.dom.find('.modal-dialog').css('max-width', '650px');
                me.rpc(me.model, 'loadPermission', {
                    ids: [me.form.dataId]
                }, {
                    usePresent: true,
                }).then(r => {
                    let workshop = {};
                    let permission = {};
                    for (let row of r.permission) {
                        permission[row.resource_id[0]] = row.permission;
                    }
                    for (let row of r.resource) {
                        let ws = workshop[row.workshop_id[0]];
                        if (!ws) {
                            ws = {id: row.workshop_id[0], name: row.workshop_id[1], lines: []};
                            workshop[row.workshop_id[0]] = ws;
                        }
                        ws.lines.push(row);
                        row.permission = permission[row.id];
                    }
                    let body = [];
                    for (let key of Object.keys(workshop)) {
                        let ws = workshop[key];
                        body.push(`<tr><td colspan="2">${ws.name}</td>
                            <td>
                                <button type="button" data-ws="${ws.id}" class="btn btn-flat btn-default btn-read-ws">${'查看'.t()}</button>
                                <button type="button" data-ws="${ws.id}" class="btn btn-flat btn-default btn-edit-ws">${'编辑'.t()}</button>
                            </td>
                        </tr>`);
                        for (let row of ws.lines) {
                            body.push(`<tr>
                                <td class="pl-5">${row.present}</td>
                                <td>${row.type == 'line' ? '产线'.t() : '设备'.t()}</td>
                                <td>
                                    <select data-ws="${ws.id}" data-id="${row.id}" style="min-width:60px">
                                        <option value=""></option>
                                        <option${row.permission == 'read' ? ' selected="selected"':''} value="read">${'查看'.t()}</option>
                                        <option${row.permission == 'edit' ? ' selected="selected"':''} value="edit">${'编辑 '.t()}</option>
                                    </select>
                                </td>
                            </tr>`);
                        }
                    }
                    dialog.body.addClass('p-2').html(`<table class="table table-bordered">
                        <thead>
                            <tr>
                                <th>${'资源'.t()}</th>
                                <th>${'类型'.t()}</th>
                                <th>
                                    <button type="button" id="btnReadAll" class="btn btn-flat btn-default">${'查看'.t()}</button>
                                    <button type="button" id="btnEditAll" class="btn btn-flat btn-default">${'编辑'.t()}</button>
                                    <button type="button" id="btnRemoveAll" class="btn btn-flat btn-default">${'清空'.t()}</button>
                                </th>
                            </tr>
                        </thead>
                        <tbody>${body.join('')}</tbody>
                    </table>`).on('click', '#btnReadAll', function () {
                        dialog.body.find('select').val('read');
                    }).on('click', '#btnEditAll', function () {
                        dialog.body.find('select').val('edit');
                    }).on('click', '#btnRemoveAll', function () {
                        dialog.body.find('select').val('');
                    }).on('click', '.btn-read-ws', function () {
                        let ws = $(this).attr('data-ws');
                        dialog.body.find(`select[data-ws="${ws}"]`).val('read');
                    }).on('click', '.btn-edit-ws', function () {
                        let ws = $(this).attr('data-ws');
                        dialog.body.find(`select[data-ws="${ws}"]`).val('edit');
                    });
                });
            },
            submit(dialog) {
                let permission = [];
                dialog.body.find('select').each(function () {
                    let select = $(this);
                    let val = select.val();
                    if (val) {
                        permission.push([0, 0, {resource_id: select.attr('data-id'), permission: val}]);
                    }
                });
                me.rpc(me.model, 'update', {
                    ids: [me.form.dataId],
                    values: {
                        permission_ids: [[5, 0, 0], ...permission],
                    }
                }).then(r => {
                    jmaa.msg.show('操作成功'.t());
                    dialog.close();
                    me.load();
                });
            }
        })
    }
});
