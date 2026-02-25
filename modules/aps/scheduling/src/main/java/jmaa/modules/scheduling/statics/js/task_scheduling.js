//@ sourceURL=task_scheduling.js
jmaa.view({
    init() {
        let me = this;
        me.dom.find('.rp-tabs').JTabs();
        me.dom.find('.mf-tabs').JTabs();
        me.initTaskInfo();
        me.msg = new jmaa.editors.msg_editor({
            view: me.view,
            dom: me.dom.find('.msg-box'),
        });
        me.controller = new TaskController({view: me, simulator: new Simulator()});
        let timeout;
        me.gantt = new GanttView({
            dom: me.dom.find('.m-body'),
            logger: me,
            on: {
                beforeItemMove(e, item) {
                    me.controller.activeTask(me.gantt, item.data.id);
                },
                itemMove(e, item, start, resourceId) {
                    me.controller.moveTask(me.gantt, item, start, resourceId);
                },
                selected(e, selected) {
                    me.dom.find('[t-click=deleteTask],[t-click=locate]').attr('disabled', !selected.length).removeAttr('clicking');
                },
                activeItem(e, taskId) {
                    me.controller.activeTask(me.gantt, taskId);
                },
                lineDblClick(e, lineId) {
                    me.showResourceTasks(lineId);
                },
                zoomChange(e, value) {
                    clearTimeout(timeout);
                    timeout = setTimeout(() => {
                        jmaa.rpc({
                            model: me.model,
                            module: me.module,
                            method: 'updateUserSettings',
                            args: {
                                values: {task_zoom: value}
                            }
                        });
                    }, 1000);
                },
            }
        });
        me.selected = [];
        me.gantt.dom.on('contextmenu', '.g-line', function (e) {
            e.preventDefault();
            e.stopPropagation();
            me.onLineContextMenu(e);
        }).on('contextmenu', '.line-item', function (e) {
            e.preventDefault();
            e.stopPropagation();
            me.onItemContextMenu(e, $(this));
        });
        $(document).on('click', function (e) {
            if (!$(e.target).hasClass('dropdown-toggle')) {
                $('#contextMenu').remove();
            }
        });
        me.loadData();
        me.memo = jmaa.create("CommandMemo", {
            change() {
                me.dom.find('[t-click=redo]').attr('disabled', !me.memo.canRedo()).removeAttr('clicking');
                me.dom.find('[t-click=undo]').attr('disabled', !me.memo.canUndo()).removeAttr('clicking');
            }
        });
        me.memo.triggerChange();
        me.dom.find('[t-click=enableEdit]').click();
    },
    reload() {
        this.loadData();
        this.memo.clear();
    },
    loadData() {
        let me = this;
        jmaa.mask('数据加载中...'.t());
        me.log('数据加载中...'.t());
        me.controller.loadData(function (data, settings) {
            me.log('数据完成'.t());
            me.gantt.zoom(settings.task_zoom);
            //me.zoomSlider.bootstrapSlider('setValue', settings.task_zoom, true, true);
            jmaa.mask();
            me.controller.simulator.setData(data, settings);
            //me.mockData(data);
            me.gantt.setData(data, settings);
        });
    },
    mockData(data) {
        let me = this;
        //模拟 数据
        for (let key of Object.keys(data.resources)) {
            let res = data.resources[key];
            let begin = moment().subtract(7, 'days').startOf('day');
            for (let j = 0; j < 10; j++) {
                let duration = 1440 + Math.random() * 1440;
                let id = `t-${res.id}-${j}`;
                let start = me.controller.simulator.getAvailableStart(res, begin);
                let end = me.controller.simulator.getOffsetDate(res, start, duration);
                data.tasks[id] = {
                    id,
                    name: `任务-${j}`,
                    plan_start: start.format('yyyy-MM-DD HH:mm:ss'),
                    plan_end: end.format('yyyy-MM-DD HH:mm:ss'),
                    work_start: start.add(Math.random() * 30, 'minutes').format('yyyy-MM-DD HH:mm:ss'),
                    duration,
                    resource_id: res.id,
                    factory_due_date: moment(end).add(18 - Math.random() * 20, 'days').format('yyyy-MM-DD'),
                    is_warning: Math.random() > 0.8,
                    plan_qty: 1000 + Math.random() * 500,
                    status: 'new',
                    "algorithm": "molding",
                    "craft_type_id": res.craft_type_id,
                    details: [{
                        "craft_order_id": "05i81kytyxog0",
                        "efficiency": 1.0,
                        "plan_qty": 1000.0,
                        "craft_process_id": "05i7zy4g3fp4w",
                        "task_id": "05ifetgjg7u2o",
                        "order_qty": null,
                        "output": null,
                        "work_order_id": null,
                        "cycle_time": 120,
                        "material_id": null,
                        "id": "05ifeujs5si68",
                        "release_start": "",
                        "release_end": "",
                    }]
                };
                begin = end;
            }
        }
    },
    showResourceTasks(resourceId) {
        let me = this;
        let tasks = [];
        for (let task of Object.values(me.gantt.items)) {
            if (resourceId == task.data.resource_id) {
                tasks.push(task.data);
            }
        }
        jmaa.showDialog({
            title: '任务列表'.t(),
            init(dialog) {
                me.loadView('as.plan_task', 'grid', 'task-related').then(v => {
                    dialog.grid = dialog.body.JGrid({
                        model: v.model,
                        module: v.module,
                        fields: v.fields,
                        arch: v.views.grid.arch,
                        vid: v.views.grid.view_id,
                        ajax(grid, callback) {
                            let list = me.controller.getTaskList(tasks);
                            callback({data: list});
                        },
                        on: {
                            rowDblClick(e, grid, id) {
                                let item = me.gantt.items[id];
                                me.gantt.locateItem(item.dom);
                                me.controller.activeTask(me.gantt, id);
                                dialog.close();
                            }
                        }
                    });
                });
            },
        })
    },
    onItemContextMenu(e, item) {
        let me = this;
        $('#contextMenu').remove();
        let id = item.attr('data-id');
        let task = me.gantt.items[id];
        let menu = $(`<div class="dropdown-menu show" id="contextMenu">
                <button class="dropdown-item" action="cutItem">${'剪切'.t()}</button>
                <button class="dropdown-item" action="pasteAfter">${'粘贴在后'.t()}</button>
                <button class="dropdown-item" action="pasteBefore">${'粘贴在前'.t()}</button>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" action="lockTasks">${task.data.is_locked ? '解锁'.t() : '锁定'.t()}</button>
                <button class="dropdown-item" action="lockBeforeTasks">${'锁定之前任务'.t()}</button>
                <button class="dropdown-item" action="unlockAfterTasks">${'解锁之后任务'.t()}</button>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" action="releaseItem">${'下达生产'.t()}</button>
            </div>`).on('click', '.dropdown-item', function () {
            let action = $(this).attr('action');
            me[action](task);
        });
        if (task.readonly) {
            menu.find('.dropdown-item').attr('disabled', 'disabled');
        }
        $('body').append(menu);
        menu.css({
            left: e.pageX + 'px',
            top: e.pageY + 'px',
            display: 'block'
        });
    },
    onLineContextMenu(e) {
        let me = this;
        $('#contextMenu').remove();
        let menu = $(`<div class="dropdown-menu show" id="contextMenu">
                <div class="dropright dropdown-submenu">
                    <button class="dropdown-item dropdown-toggle" data-toggle="dropdown">${'优化排程'.t()}</button>
                    <div class="dropdown-menu">
                        <button class="dropdown-item" action="lineOptimize">${'当前资源'.t()}</button>
                        <button class="dropdown-item" action="workshopOptimize">${'当前车间'.t()}</button>
                        <button class="dropdown-item" action="globalOptimize">${'整体优化'.t()}</button>
                    </div>
                </div>
                <button class="dropdown-item" action="setSpecialTime">${'设置特殊时间'.t()}</button>
                <div class="dropright dropdown-submenu">
                    <button class="dropdown-item dropdown-toggle" data-toggle="dropdown">${'移除特殊时间'.t()}</button>
                    <div class="dropdown-menu">
                        <button class="dropdown-item" action="removeSpecialTime">${'移除当前'.t()}</button>
                        <button class="dropdown-item" action="removeSpecialTimes">${'批量移除'.t()}</button>
                    </div>
                </div>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" action="lockBefore">${'锁定当天之前的任务'.t()}</button>
                <button class="dropdown-item" action="unlockAfter">${'解锁当天之后的任务'.t()}</button>
                <div class="dropdown-divider"></div>
                <div class="dropright dropdown-submenu">
                    <button class="dropdown-item dropdown-toggle" data-toggle="dropdown">${'删除当天之后的任务'.t()}</button>
                    <div class="dropdown-menu">
                        <button class="dropdown-item" action="removeLineTasks">${'当前资源'.t()}</button>
                        <button class="dropdown-item" action="removeWorkshopTasks">${'当前车间'.t()}</button>
                        <button class="dropdown-item" action="removeAllTasks">${'所有资源'.t()}</button>
                    </div>
                </div>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" action="pasteItem">${'粘贴'.t()}</button>
            </div>`).on('click', '[action]', function () {
            let action = $(this).attr('action');
            me[action](e);
        });
        if ($(e.target).hasClass('readonly')) {
            menu.find('.dropdown-item').attr('disabled', 'disabled');
        } else {
            let dt = me.gantt.getDate(e.pageX);
            if (!me.gantt.editable()) {
                if (dt < me.gantt.settings.minorBeginDate) {
                    menu.find('.dropdown-item:not([action=lockBefore])').attr('disabled', 'disabled');
                } else {
                    menu.find('.dropdown-item:not([action=lockBefore]):not([action=unlockAfter])').attr('disabled', 'disabled');
                }
            } else {
                if (dt < me.gantt.settings.minorBeginDate) {
                    menu.find('[action=setSpecialTime],[action=removeSpecialTime],[action=unlockAfter],[action=pasteItem]').attr('disabled', 'disabled');
                } else {

                }
            }
        }
        $('body').append(menu);
        menu.css({
            left: e.pageX + 'px',
            top: e.pageY + 'px',
            display: 'block'
        });
    },
    removeLineTasks(e) {
        let me = this;
        let start = me.gantt.getDate(e.pageX).format('yyyy-MM-DD');
        let resourceId = $(e.target).attr('data-id');
        me.removeResourceTask([resourceId], start);
    },
    removeWorkshopTasks(e) {
        let me = this;
        let start = me.gantt.getDate(e.pageX).format('yyyy-MM-DD');
        let resourceId = $(e.target).attr('data-id');
        let resource = me.gantt.lines.data[resourceId];
        let resources = [];
        for (let line of Object.values(me.gantt.lines.data)) {
            if (line.visible && !line.readonly && resource.workshop == line.workshop && resource.craft_type_id == line.craft_type_id) {
                resources.push(line.id);
            }
        }
        me.removeResourceTask(resources, start);
    },
    removeAllTasks(e) {
        let me = this;
        let start = me.gantt.getDate(e.pageX).format('yyyy-MM-DD');
        let resources = [];
        for (let line of Object.values(me.gantt.lines.data)) {
            if (line.visible && !line.readonly) {
                resources.push(line.id);
            }
        }
        me.removeResourceTask(resources, start);
    },
    removeResourceTask(resources, start) {
        let me = this;
        let tasks = [];
        for (let task of Object.values(me.gantt.items)) {
            if (resources.includes(task.data.resource_id) && task.data.status == 'new' && !task.data.is_locked && task.data.plan_start > start) {
                tasks.push(task.data.id);
            }
        }
        if (tasks.length) {
            jmaa.msg.confirm({
                title: '确认'.t(),
                content: '确认删除选中的[{0}]个任务？'.t().formatArgs(tasks.length),
                submit() {
                    me.controller.deleteTasks(me.gantt, tasks);
                }
            });
        } else {
            me.error('没有可删除任务'.t());
        }
    },
    releaseTask(e) {

    },
    submitLock(ids, lock) {
        let me = this;
        let toUpdate = [];
        for (let id of ids) {
            toUpdate.push({id, is_locked: lock});
        }
        me.rpc(me.model, 'saveTasks', {
            toUpdate,
        }).then(d => {
            me.log('{0}[{1}]个任务'.t().formatArgs((lock ? '锁定' : '解锁').t(), ids.length));
            let cmd = new CommandLock(me.controller, me.gantt);
            cmd.createSnapshot(ids, lock);
            me.memo.add(cmd);
        });
    },
    lockTasks(task) {
        let me = this;
        let lock = !task.data.is_locked;
        let ids = [];
        for (let id of me.gantt.select()) {
            ids.push(...me.controller.lockTasks(me.gantt, id, lock));
        }
        me.submitLock(ids, lock);
    },
    lockBeforeTasks(task) {
        let me = this;
        let lineId = task.data.resource_id;
        let start = task.data.plan_start;
        let ids = [];
        ids.push(...me.controller.lockTasks(me.gantt, task.data.id, true));
        for (let item of Object.values(me.gantt.items)) {
            if (item.data.resource_id == lineId && item.data.plan_start < start) {
                ids.push(...me.controller.lockTasks(me.gantt, item.data.id, true));
            }
        }
        me.submitLock(ids, true);
    },
    unlockAfterTasks(task) {
        let me = this;
        let lineId = task.data.resource_id;
        let start = task.data.plan_start;
        let ids = [];
        ids.push(...me.controller.lockTasks(me.gantt, task.data.id, false));
        for (let item of Object.values(me.gantt.items)) {
            if (item.data.resource_id == lineId && item.data.plan_start > start) {
                ids.push(...me.controller.lockTasks(me.gantt, item.data.id, false));
            }
        }
        me.submitLock(ids, false);
    },
    lockBefore(e) {
        let me = this;
        let date = me.gantt.getDate(e.pageX).format('yyyy-MM-DD');
        let ids = [];
        for (let item of Object.values(me.gantt.items)) {
            if (item.data.plan_start < date) {
                ids.push(...me.controller.lockTasks(me.gantt, item.data.id, true));
            }
        }
        me.submitLock(ids, true);
    },
    unlockAfter(e) {
        let me = this;
        let date = me.gantt.getDate(e.pageX).format('yyyy-MM-DD');
        let ids = [];
        for (let item of Object.values(me.gantt.items)) {
            if (item.data.plan_start > date) {
                ids.push(...me.controller.lockTasks(me.gantt, item.data.id, false));
            }
        }
        me.submitLock(ids, false);
    },
    removeSpecialTime(e) {
        let me = this;
        let lineId = $(e.target).attr('data-id');
    },
    removeSpecialTimes(e) {
        let me = this;
        let lineId = $(e.target).attr('data-id');
    },
    setSpecialTime(e) {
        let me = this;
        let lineId = $(e.target).attr('data-id');
    },
    lineOptimize(e) {
        let me = this;
        let resourceId = $(e.target).attr('data-id');
        me.optimize([resourceId]);
    },
    workshopOptimize(e) {
        let me = this;
        let resourceId = $(e.target).attr('data-id');
        let resource = me.gantt.lines.data[resourceId];
        let resources = [];
        for (let line of Object.values(me.gantt.lines.data)) {
            if (line.visible && !line.readonly && resource.workshop == line.workshop && resource.craft_type_id == line.craft_type_id) {
                resources.push(line.id);
            }
        }
        me.optimize(resources);
    },
    globalOptimize() {
        let me = this;
        let resources = [];
        for (let line of Object.values(me.gantt.lines.data)) {
            if (line.visible && !line.readonly) {
                resources.push(line.id);
            }
        }
        me.optimize(resources);
    },
    optimize(resources, newTasks, callback) {
        let me = this;
        jmaa.showDialog({
            title: newTasks ? '导入排程'.t() : '优化排程'.t(),
            css: 'modal-md',
            init(dialog) {
                me.initOptimizeDialog(dialog, !!newTasks);
            },
            createScheme(e, dialog) {
                me.editScheme(null, function () {
                    dialog.grid.load();
                });
            },
            editScheme(e, dialog) {
                me.editScheme(dialog.grid.getSelected()[0], function () {
                    dialog.grid.load();
                });
            },
            deleteScheme(e, dialog) {
                jmaa.msg.confirm({
                    title: '确认'.t(),
                    content: '确认删除？'.t(),
                    submit() {
                        me.rpc('as.scheme', 'delete', {
                            ids: dialog.grid.getSelected(),
                        }).then(d => {
                            dialog.grid.load();
                        });
                    }
                });
            },
            async submit(dialog) {
                await me.submitOptimize(dialog, resources, newTasks, callback);
            }
        });
    },
    async submitOptimize(dialog, resources, newTasks, callback) {
        let me = this;
        let scheme = dialog.grid.getSelected()[0];
        if (!scheme) {
            return jmaa.msg.error('请选择方案'.t());
        }
        if (!dialog.form.valid()) {
            return jmaa.msg.error(dialog.form.getErrors());
        }
        let begin = performance.now();
        let opt = dialog.form.getData();
        let tasks = [];
        if (newTasks) {
            let moldIds = [];
            for (let task of newTasks) {
                task.phantom = true;
                task.resource_id = resources[0];
                task.plan_start = '2025-01-01';
                task.plan_end = '2025-01-02';
                tasks.push(task);
                if (task.mold_id) {
                    moldIds.push(task.mold_id);
                }
            }
            me.controller.simulator.addTask(newTasks);
            await me.controller.loadMoldResource(moldIds);
        }
        let start = opt.begin;
        if (!opt.append) {
            for (let task of Object.values(me.gantt.items)) {
                if (resources.includes(task.data.resource_id) && task.data.status == 'new' && !task.data.is_locked && task.data.plan_start > start) {
                    tasks.push(task.data);
                    task.dom.hide();
                }
            }
        }
        dialog.close();
        callback && callback();
        jmaa.mask('计算中...'.t());
        let cmd = new CommandOptimize(me.controller, me.gantt);
        try {
            let solutions = await me.controller.optimization(me.gantt, tasks, resources, [{}], moment(start), cmd);
            //TODO 选方案
            for (let s of solutions) {
                //记录快照
            }
            jmaa.mask();
            let end = performance.now();
            let duration = (end - begin) / 1000;
            me.log('计算完成，用时：{0}s'.t().formatArgs(duration.toFixed(4)));
            await me.controller.submitSolution(me.gantt, solutions[0]);
            for (let task of tasks) {
                cmd.updateSnapshot(task);
            }
            me.memo.add(cmd);
        } catch (e) {
            console.error(e);
            cmd.undo(true);
            jmaa.mask();
            me.error(e.message);
        }
    },
    initOptimizeDialog(dialog, append) {
        let me = this;
        let begin = moment(me.controller.simulator.settings.minorEndDate).add(1, 'day').format("yyyy-MM-DD");
        dialog.body.html(`<div class="scheme-form"></div>
            <div class="scheme-toolbar">
                <button type="button" class="btn btn-flat btn-primary" t-click="createScheme">${'创建'.t()}</button>
                <button type="button" class="btn btn-flat btn-blue" t-click="editScheme">${'编辑'.t()}</button>
                <button type="button" class="btn btn-flat btn-danger" t-click="deleteScheme">${'删除'.t()}</button>
            </div>
            <div class="scheme-grid"></div>`);
        dialog.form = dialog.body.find('.scheme-form').JForm({
            arch: `<form>
                <editor name="create" type="boolean" visible="0"></editor>
                <editor name="append" type="boolean" label="追加排程" t-visible="create"></editor>
                <editor name="begin" type="date" label="排程开始" colspan="3" min="${begin}" required="1"></editor>
            </form>`
        });
        dialog.form.create({append, begin, create: append});
        me.loadView('as.scheme', 'grid').then(v => {
            dialog.grid = dialog.body.find('.scheme-grid').JGrid({
                model: v.model,
                module: v.module,
                arch: v.views.grid.arch,
                fields: v.fields,
                view: me,
                ajax(grid, callback) {
                    me.rpc(v.model, "search", {
                        criteria: [],
                        fields: grid.getFields(),
                        limit: 1000,
                    }, {
                        usePresent: grid.getUsePresent(),
                    }).then(d => {
                        callback({data: d.values});
                        if (d.values.length) {
                            dialog.grid.select([d.values[0].id]);
                        }
                    });
                },
                on: {
                    selected(e, grid, sel) {
                        dialog.body.find('[t-click=editScheme]').attr('disabled', sel.length != 1);
                        dialog.body.find('[t-click=deleteScheme]').attr('disabled', !sel.length);
                    },
                }
            });
        });
    },
    pasteItem(e) {
        let me = this;
        let lineId = $(e.target).attr('data-id');
        let date = me.gantt.getDate(e.pageX);

    },
    showToPlanOrder() {
        let me = this;
        jmaa.showDialog({
            id: 'to-plan-order-dialog',
            title: '未排计划单'.t(),
            css: 'modal-lg modal-max',
            init(dialog) {
                dialog.body.html(`<div class="data-panel view-panel">
                    <div class="header">
                        <div class="content-header p-2">
                            <div class="mb-1 pl-2 pr-2">
                                <div part="search"></div>
                            </div>
                            <div class="btn-row">
                                <div part="toolbar" class="toolbar"></div>
                                <div class="btn-toolbar toolbar-right">
                                    <div part="pager" class="ml-2"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="content">
                        <aside part="search-panel" class="left-aside border-right" style="display:none"></aside>
                        <div class="container-fluid grid-sm">
                            <div class="to-order-grid"></div>
                            <div class="btn-row">
                                <div class="toolbar">
                                    <button type="button" class="btn btn-blue btn-flat btn-import-task">${'导入排程'.t()}</button>
                                </div>
                            </div>
                            <div class="to-plan-grid"></div>
                        </div>
                    </div>
                </div>`);
                me.initToPlanOrderDialog(dialog);
            }
        })
    },
    initToPlanOrderDialog(dialog) {
        let me = this;
        me.loadView('as.to_plan_order', 'grid').then(v => {
            dialog.search = new JSearch({
                dom: dialog.body.find('[part=search]'),
                model: v.model,
                module: v.module,
                arch: v.views.search.arch,
                fields: v.fields,
                view: me,
                submitting(e, search) {
                    delete dialog.grid.success;
                    delete dialog.grid.errors;
                    delete dialog.grid.taskData;
                    dialog.grid.toPlanGrid.load();
                    dialog.pager.reset();
                    dialog.grid.load();
                },
            });
            dialog.pager = new JPager({
                dom: dialog.body.find('[part=pager]'),
                limitChange(e, pager) {
                    if (dialog.grid) {
                        dialog.grid.limit = pager.limit;
                    }
                },
                pageChange(e, pager) {
                    dialog.grid.load();
                },
                counting(e, pager) {
                    jmaa.rpc({
                        model: v.model,
                        module: v.module,
                        method: 'countOrder',
                        args: {
                            criteria: dialog.search.getCriteria(),
                        },
                        onsuccess(r) {
                            pager.update({
                                total: r.data,
                            });
                        },
                    });
                },
            });
            dialog.grid = dialog.body.find('.to-order-grid').JGrid({
                model: v.model,
                module: v.module,
                arch: v.views.grid.arch,
                fields: v.fields,
                view: me,
                vid: v.views.grid.view_id,
                customizable: true,
                search: dialog.search,
                on: {
                    selected(e, grid, sel) {
                        let selected = grid.data.filter(d => sel.includes(d.id));
                        if (dialog.toolbar) {
                            dialog.toolbar.update(selected);
                        }
                    },
                },
                updateResult() {
                    let grid = this;
                    if (grid.errors) {
                        for (let key of Object.keys(grid.errors)) {
                            let ids = key.split("|");
                            grid.dom.find(`.result-column[data-id=${ids[0]}]`).html('生成失败：'.t() + grid.errors[key].join(';'));
                        }
                    }
                    if (grid.success) {
                        for (let id of grid.success) {
                            grid.dom.find(`.result-column[data-id=${id}]`).html('生成成功'.t());
                        }
                    }
                },
                ajax(grid, callback) {
                    jmaa.rpc({
                        model: v.model,
                        module: v.module,
                        method: 'searchOrder',
                        args: {
                            criteria: dialog.search.getCriteria(),
                            nextTest: true,
                            offset: dialog.pager.getOffset(),
                            limit: dialog.pager.getLimit(),
                            fields: grid.getFields(),
                            order: grid.getSort(),
                        },
                        context: {
                            usePresent: grid.getUsePresent(),
                        },
                        onsuccess(r) {
                            if (r.data.values.length > 0) {
                                dialog.pager.update(r.data);
                            } else {
                                dialog.pager.noData();
                            }
                            callback({
                                data: r.data.values,
                            });
                            grid.updateResult();
                        },
                    });
                },
            });
            dialog.toolbar = new JToolbar({
                dom: dialog.body.find('[part=toolbar]'),
                arch: dialog.grid.tbarArch,
                auths: '@all',
                target: dialog.grid,
                view: me,
            });
            me.loadView('as.to_plan_order', 'grid', "to-plan").then(v => {
                dialog.grid.toPlanGrid = dialog.body.find('.to-plan-grid').JGrid({
                    model: v.model,
                    module: v.module,
                    arch: v.views.grid.arch,
                    fields: v.fields,
                    view: me,
                    vid: v.views.grid.view_id,
                    on: {
                        selected(e, grid, sel) {
                            dialog.body.find('.btn-import-task').attr('disabled', !sel.length);
                        },
                    },
                    ajax(grid, callback) {
                        callback({data: dialog.grid.taskData ? dialog.grid.taskData.values : []})
                    },
                });
            });
            dialog.body.on('mousedown', '.to-order-grid .link-column', function () {
                me.showToPlanCraftOrder($(this).attr('data-id'), dialog);
                return false;
            }).on('mousedown', '.to-plan-grid .link-column', function () {
                me.showPlanTaskDetails($(this).attr('data-id'), dialog);
                return false;
            }).on('click', '.btn-import-task', function () {
                me.importTask(dialog);
            });
        });
    },
    importTask(owner) {
        let me = this;
        let newTasks = owner.grid.toPlanGrid.getSelectedData();
        let resources = [];
        for (let line of Object.values(me.gantt.lines.data)) {
            if (line.visible && !line.readonly) {
                resources.push(line.id);
            }
        }
        me.optimize(resources, newTasks, function () {
            owner.close();
        });
    },
    editScheme(id, callback) {
        let me = this;
        jmaa.showDialog({
            title: '编辑方案'.t(),
            id: 'edit-scheme',
            init(dialog) {
                me.loadView('as.scheme', 'form').then(v => {
                    dialog.form = dialog.body.JForm({
                        model: v.model,
                        module: v.module,
                        arch: v.views.form.arch,
                        fields: v.fields,
                        view: me
                    });
                    if (id) {
                        me.rpc('as.scheme', 'read', {
                            ids: [id],
                            fields: dialog.form.getFields(),
                        }, {
                            usePresent: true,
                        }).then(d => {
                            dialog.form.setData(d[0]);
                        });
                    } else {
                        dialog.form.create({});
                    }
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                if (id) {
                    me.rpc('as.scheme', 'update', {
                        ids: [id],
                        values: dialog.form.getSubmitData(),
                    }).then(d => {
                        dialog.close();
                        callback();
                    });
                } else {
                    me.rpc('as.scheme', 'create', dialog.form.getSubmitData()).then(d => {
                        dialog.close();
                        callback();
                    });
                }
            }
        })
    },
    showToPlanCraftOrder(id, dialog) {
        let me = this;
        jmaa.showDialog({
            title: '制程单明细'.t(),
            init(dialog) {
                me.loadView('as.craft_order', 'grid', "to-plan").then(v => {
                    dialog.grid = dialog.body.JGrid({
                        model: v.model,
                        module: v.module,
                        arch: v.views.grid.arch,
                        fields: v.fields,
                        view: me,
                        vid: v.views.grid.view_id,
                        customizable: true,
                        ajax(grid, callback) {
                            me.rpc("as.to_plan_order", "loadCraftOrder", {
                                ids: [id],
                                fields: grid.getFields()
                            }, {
                                usePresent: grid.getUsePresent(),
                            }).then(d => {
                                callback({data: d});
                            });
                        }
                    });
                });
            }
        });
    },
    generatePlanTask(e, target) {
        let me = this;
        let ids = target.getSelected();
        target.dom.find('.result-column[data-id]').html('');
        me.rpc('as.to_plan_order', "generateTask", {
            ids
        }).then(d => {
            target.taskData = {};
            target.taskData.values = d.tasks;
            for (let t of d.tasks) {
                target.taskData[t.id] = t;
            }
            target.toPlanGrid.load();
            target.errors = d.errors;
            target.success = [...ids];
            for (let key of Object.keys(d.errors)) {
                let ids = key.split("|");
                target.success.remove(ids[0]);
            }
            target.updateResult();
        });
    },
    showPlanTaskDetails(id, owner) {
        let me = this;
        jmaa.showDialog({
            title: '制程单明细'.t(),
            init(dialog) {
                me.loadView('as.craft_order', 'grid', "plan-task").then(v => {
                    dialog.grid = dialog.body.JGrid({
                        model: v.model,
                        module: v.module,
                        arch: v.views.grid.arch,
                        fields: v.fields,
                        view: me,
                        vid: v.views.grid.view_id,
                        customizable: true,
                        ajax(grid, callback) {
                            let task = owner.grid.taskData[id];
                            callback({data: task.details})
                        }
                    });
                });
            }
        });
    },
    orderUserSettings() {
        let me = this;
        jmaa.showDialog({
            title: '计划单生成规则'.t(),
            css: 'modal-m',
            init(dialog) {
                me.loadView("as.user_settings", "form", "order").then(v => {
                    dialog.form = dialog.body.JForm({
                        model: v.model,
                        fields: v.fields,
                        arch: v.views.form.arch,
                    });
                    me.rpc(me.model, "readUserSettings", {
                        fields: dialog.form.getFields(),
                    }).then(d => {
                        dialog.form.setData(d);
                    });
                });
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                me.rpc(me.model, "updateUserSettings", {
                    values: data
                }).then(d => {
                    jmaa.msg.show("操作成功".t());
                });
            }
        });
    },
    showOrder() {

    },
    undo() {
        this.memo.undo();
    },
    redo() {
        this.memo.redo();
    },
    enableEdit(e) {
        let me = this;
        let canEdit = !me.gantt.editable();
        me.gantt.editable(canEdit);
        if (canEdit) {
            $(e.target).css('opacity', '1').html('只读'.t());
        } else {
            $(e.target).css('opacity', '0.6').html('编辑'.t());
        }
    },
    deleteTask() {
        let me = this;
        let selected = me.gantt.select();
        if (!selected.length) {
            return jmaa.msg.error('没有选中的任务'.t());
        }
        jmaa.msg.confirm({
            title: '确认'.t(),
            content: '确认删除选中的[{0}]个任务？'.t().formatArgs(selected.length),
            submit() {
                me.controller.deleteTasks(me.gantt, selected);
            }
        })
    },
    locate() {
        let me = this;
        me.gantt.locateItem(me.gantt.dom.find('.line-item.active'));
    },
    locateToday() {
        let me = this;
        let today = me.dom.find('.c-today');
        me.gantt.locateItem(today);
    },
    editUserSettings() {
        let me = this;
        jmaa.showDialog({
            title: '编辑设置'.t(),
            css: 'modal-lg',
            init(dialog) {
                dialog.dom.find('.modal-dialog').css('max-width', '1000px');
                dialog.dom.find('.buttons-right').prepend(`<button class="btn btn-flat btn-default" t-click="resetUserSettings">${'重置'.t()}</button>`)
                me.loadView('as.user_settings', 'form', 'task').then(v => {
                    dialog.form = dialog.body.JForm({
                        arch: v.views.form.arch,
                        model: v.model,
                        fields: v.fields,
                        view: me,
                    });
                    dialog.form.dom.find('.d-grid').css('column-gap', '2rem');
                    me.rpc(me.model, "readUserSettings", {
                        fields: dialog.form.getFields(),
                    }).then(d => {
                        dialog.form.setData(d);
                    });
                });
            },
            async resetUserSettings(e, dialog) {
                let ids = dialog.form.editors.hide_resource_ids.values;
                await dialog.form.create();
                dialog.form.editors.hide_resource_ids.values = ids;
            },
            submit(dialog) {
                if (!dialog.form.valid()) {
                    return jmaa.msg.error(dialog.form.getErrors());
                }
                let data = dialog.form.getSubmitData();
                me.rpc(me.model, "updateUserSettings", {
                    values: data
                }).then(d => {
                    jmaa.msg.show("操作成功".t());
                    me.loadData();
                    dialog.close();
                });
            }
        });
    },
    log(msg) {
        let me = this;
        me.msg.setValue({msg: msg});
    },
    error(msg) {
        let me = this;
        me.msg.setValue({error: true, msg: msg});
    },
    initTaskInfo() {
        let me = this;
        me.loadView('as.plan_task', 'form', 'task-info').then(v => {
            me.taskInfoForm = me.dom.find('.task-info').JForm({
                model: v.model,
                module: v.module,
                fields: v.fields,
                arch: v.views.form.arch,
            });
            me.taskInfoForm.setReadonly(true);
        });
        me.loadView('as.plan_task', 'grid', 'task-related').then(v => {
            me.taskRelatedGrid = me.dom.find('.task-related').JGrid({
                model: v.model,
                module: v.module,
                fields: v.fields,
                arch: v.views.grid.arch,
                vid: v.views.grid.view_id,
                ajax(grid, callback) {
                    let data = {data: []};
                    if (me.taskRelatedGrid && me.taskRelatedGrid.data) {
                        data.data = me.taskRelatedGrid.data;
                    }
                    callback(data);
                },
                on: {
                    rowDblClick(e, grid, id) {
                        let item = me.gantt.items[id];
                        me.gantt.locateItem(item.dom);
                        me.controller.activeTask(me.gantt, id);
                    }
                }
            });
        });
    },
    kitting() {
        let me = this;
        me.canvas.globalOptimize();
    }
});
jmaa.column('craft-column', {
    render: function () {
        return function (data, type, row) {
            if (!data) {
                data = '';
            }
            return `<a class="link-column" data-id="${data}">${'明细'.t()}</a>`;
        }
    }
});
jmaa.column('result-column', {
    render: function () {
        return function (data, type, row) {
            if (!data) {
                data = '';
            }
            return `<a class="result-column" data-id="${row.id}"></a>`;
        }
    }
});
jmaa.editor('duration', {
    extends: 'editors.char',
    setValue(value) {
        let me = this;
        let num = Number(value);
        if (num != NaN) {
            let hours = Math.floor(num / 60);
            let mins = Math.round(num % 60);
            let result = '';
            if (hours > 0) {
                result += `${hours} h `;
            }
            if (mins > 0 || num === 0) {
                result += `${mins} m`;
            }
            value = result;
        }
        me.callSuper(value);
    }
});
jmaa.editor('resource-visible', {
    getTpl() {
        let me = this;
        let body = [];
        for (let ws of Object.values(me.workshop)) {
            let checkId = jmaa.nextId();
            body.push(`<tr>
                <td colspan="2">
                    <input id="res-ckb-${checkId}" checked="checked" title="显示资源" type="checkbox" class="check-select check-group">
                    <label for="res-ckb-${checkId}" class="form-check-label" style="vertical-align:text-bottom;">
                        ${ws.name}
                    </label>
                </td>
            </tr>`);
            for (let row of ws.lines) {
                body.push(`<tr>
                    <td class="pl-5">
                        <input id="res-ckb-${row.id}" data-id="${row.id}" checked="checked" group-id="res-ckb-${checkId}" title="显示资源" type="checkbox" class="check-select">
                        <label for="res-ckb-${row.id}" class="form-check-label" style="vertical-align:text-bottom;">
                            ${row.present}
                        </label>
                    </td>
                    <td>
                        ${row.readonly ? '查看'.t() : '编辑'.t()}
                    </td>
                </tr>`);
            }
        }
        return `<table class="table table-bordered">
            <thead>
                <tr>
                    <th>
                        <input id="res-check-all" checked="checked" title="显示资源" type="checkbox" class="all-check-select">
                        <label for="res-check-all" style="margin-bottom:0;vertical-align:text-bottom;">
                            ${'显示资源'.t()}
                        </label>
                    </th>
                    <th>${'权限'.t()}</th>
                </tr>
            </thead>
            <tbody>${body.join('')}</tbody>
        </table>`;
    },
    init() {
        let me = this;
        me.workshop = {};
        let resource = me.owner.view.controller.simulator.data.resources;
        for (let row of Object.values(resource)) {
            let ws = me.workshop[row.workshop];
            if (!ws) {
                ws = {id: row.workshop, name: row.workshop, lines: []};
                me.workshop[row.workshop] = ws;
            }
            ws.lines.push(row);
        }
        me.dom.html(me.getTpl()).addClass('grid-sm').on('change', '.all-check-select', function () {
            let ckb = $(this);
            me.dom.find('input.check-select').prop('checked', ckb.is(':checked'));
        }).on('change', '.check-group', function () {
            let ckb = $(this);
            me.dom.find(`input[group-id=${ckb.attr('id')}]`).prop('checked', ckb.is(':checked'));
            if (ckb.is(':checked')) {
                if (!me.dom.find(`input[group-id]`).is(':not(:checked)')) {
                    me.dom.find(`.all-check-select`).prop('checked', true);
                }
            } else {
                me.dom.find(`.all-check-select`).prop('checked', false);
            }
        }).on('change', 'input[data-id]', function () {
            let ckb = $(this);
            if (ckb.is(':checked')) {
                if (!me.dom.find(`input[group-id=${ckb.attr('group-id')}]`).is(':not(:checked)')) {
                    me.dom.find(`#${ckb.attr('group-id')}`).prop('checked', true);
                    if (!me.dom.find(`input[group-id]`).is(':not(:checked)')) {
                        me.dom.find(`.all-check-select`).prop('checked', true);
                    }
                }
            } else {
                me.dom.find(`.all-check-select,#${ckb.attr('group-id')}`).prop('checked', false);
            }
        })
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'input:checkbox', function (e) {
            handler(e, me);
        });
    },
    setValue(value) {
        let me = this;
        me.values = value || [];
        me.dom.find(`input:checkbox`).prop('checked', true);
        for (let id of me.values) {
            me.dom.find(`input[data-id=${id}]`).prop('checked', false).trigger('change');
        }
    },
    getValue() {
        let me = this;
        let list = [];
        me.dom.find('input[data-id]:not(:checked)').each(function () {
            let ckb = $(this);
            list.push(ckb.attr('data-id'));
        });
        let values = [];
        for (let id of list) {
            if (!me.values.includes(id)) {
                values.push([4, id]);
            }
        }
        for (let id of me.values) {
            if (!list.includes(id)) {
                values.push([3, id]);
            }
        }
        return values;
    }
});
jmaa.editor('task-details', {
    init() {
        let me = this;
        me.form = $(`<div></div>`).JForm({
            arch: `<form cols="1">
                <editor name='product_order_id' type="many2one" label="生产订单"></editor>
                <editor name='craft_process_id' type="many2one" label="制程名称"></editor>
                <editor name='sales_order_id' type="many2one" label="销售订单"></editor>
                <editor name='customer_due_date' type="char" label="客户交期"></editor>
                <editor name='material_id' type="many2one" label="物料编码"></editor>
                <editor name='material_name_spec' type="char" label="规格型号"></editor>
                <editor name='customer_id' type="many2one" label="客户"></editor>
                <editor name='plan_qty' type="float" label="计划数量"></editor>
                <editor name='order_qty' type="float" label="订单数量"></editor>
                <editor name='output' type="float" label="完工数量"></editor>
                <editor name='material_ready_date' type="date" label="齐料日期"></editor>
            </form>`
        });
    },
    setValue(value) {
        let me = this;
        me.dom.html('');
        if (value) {
            for (let v of value) {
                me.form.setData(v);
                me.form.setReadonly(true);
                me.dom.append('<div class="divider"></div>');
                me.dom.append(me.form.dom.clone());
            }
        }
    }
});
