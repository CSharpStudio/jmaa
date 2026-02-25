//@ sourceURL=TaskController.js
jmaa.define('TaskController', {
    __init__(opt) {
        jmaa.utils.apply(true, this, opt || {});
    },
    /**加载排产数据（资源、设置、日历、任务、工时定额）*/
    loadData(callback) {
        let me = this;
        jmaa.rpc({
            model: me.view.model,
            module: me.view.module,
            method: 'loadData',
            onsuccess(r) {
                let settings = $.extend({
                    bg_minor_dates: '#ffcca3',
                    bg_past_dates: '#bcbec0',
                    bg_today: '#f8a866',
                    bg_sunday: '#f3f3f3',
                    bg_saturday: '#f3f3f3',
                    bg_holiday: '#cea2a2',
                    bd_advance: '#66bff3',
                    bd_severe_advance: '#3C8DBC',
                    bd_delay: '#f86976',
                    bd_severe_delay: '#DC3545',
                    bd_normal: '#28A745',
                }, r.data.settings);
                settings.beginDate = moment().subtract(settings.task_begin_offset, 'days').startOf('day');
                settings.endDate = moment().add(settings.task_end_offset, 'days').endOf('day');
                settings.minorBeginDate = moment().subtract(settings.task_minor_begin_offset, 'days').startOf('day');
                settings.minorEndDate = moment().add(settings.task_minor_end_offset, 'days').endOf('day');
                delete r.data.settings;
                callback(r.data, settings);
            }
        });
    },
    async loadMoldResource(moldIds) {
        let me = this;
        if (moldIds.length) {
            let r = await me.view.rpc(me.view.model, 'loadMoldResource', {
                moldIds
            });
            let data = me.simulator.data;
            if (data.moldResource) {
                jmaa.utils.apply(true, data.moldResource, r.moldResource);
            } else {
                data.moldResource = r.moldResource;
            }
            if (data.moldProduct) {
                jmaa.utils.apply(true, data.moldProduct, r.moldProduct);
            } else {
                data.moldProduct = r.moldProduct;
            }
            if (data.molds) {
                jmaa.utils.apply(true, data.molds, r.molds);
            } else {
                data.molds = r.molds;
            }
        }
    },
    deleteTasks(gantt, tasks) {
        let me = this;
        let cmd = new CommandDelete(me, gantt);
        for (let id of tasks) {
            let task = me.simulator.data.tasks[id];
            cmd.createSnapshot(task);
        }
        me.view.rpc(me.view.model, 'saveTasks', {
            toDelete: tasks,
        }).then(d => {
            me.view.log('删除[{0}]个任务'.t().formatArgs(tasks.length));
            me.view.memo.add(cmd);
            for (let id of tasks) {
                delete me.simulator.data.tasks[id];
                gantt.items[id].dom.remove();
                delete gantt.items[id];
            }
            gantt.select([]);
            gantt.activeItem();
        });
    },
    moveTask(gantt, item, start, resourceId) {
        let me = this;
        let cmd = new CommandMove(me, gantt);
        try {
            let toUpdate = {};
            let task = item.data;
            let snapshot = cmd.createSnapshot(task);
            let oldTasks = {};
            oldTasks[task.id] = snapshot.oldValue;
            me.simulator.assignToResource(task.id, start, resourceId);
            cmd.updateSnapshot(snapshot, task);
            let excludes = [task.id];
            let changes = me.moveAfterTasks(cmd, task, snapshot.oldValue, resourceId, excludes);
            if (me.simulator.settings.task_move_linkage) {
                changes.push(...me.moveLinkage(cmd, task, oldTasks));
            }
            //TODO  计算冲突
            for (let task of changes) {
                let item = gantt.items[task.id];
                item.update();
                item.dirty(true);
                toUpdate[task.id] = {
                    id: task.id,
                    plan_start: task.plan_start,
                    plan_end: task.plan_end,
                    work_start: task.work_start,
                    duration: task.duration,
                    is_warning: task.is_warning,
                    resource_id: task.resource_id,
                };
            }
            me.view.rpc(me.view.model, "saveTasks", {
                toUpdate: Object.values(toUpdate),
            }).then(r => {
                me.view.memo.add(cmd);
                me.view.log('移动更新[{0}]个任务'.t().formatArgs(Object.keys(toUpdate).length));
            });
        } catch (e) {
            console.error(e);
            cmd.undo(true);
            me.view.error(e.message);
            gantt.locateItem(item.dom);
        }
    },
    moveLinkage(cmd, task, oldTasks) {
        let me = this;
        let changes = [];
        let linkTask = [task];
        me.loadPrevTasks(task, linkTask);
        me.loadNextTasks(task, linkTask);
        let solution = me.moveOptimization(task, linkTask);
        for (let js of solution) {
            if (js.job.id == task.id) {
                continue;
            }
            let change = me.simulator.data.tasks[js.job.id];
            if (!js.job.setup && js.from_time > me.simulator.settings.minorBeginDate) {
                let snapshot = cmd.createSnapshot(change);
                oldTasks[change.id] = snapshot.oldValue;
                me.simulator.assignToResource(js.job.id, js.from_time, js.resource_id, true);
                cmd.updateSnapshot(snapshot, change);
            }
            changes.push(change);
        }
        //
        return changes;
    },
    loadPrevTasks(task, linkTask) {
        let me = this;
        for (let id of Object.keys(task.prevs || {})) {
            let item = me.simulator.data.tasks[id];
            linkTask.push(item);
            me.loadPrevTasks(item, linkTask);
        }
    },
    loadNextTasks(task, linkTask) {
        let me = this;
        for (let id of Object.keys(task.nexts || {})) {
            let item = me.simulator.data.tasks[id];
            linkTask.push(item);
            me.loadNextTasks(item, linkTask);
        }
    },
    moveOptimization(task, tasks) {
        let me = this;
        let start = me.simulator.today;
        let minor = me.simulator.settings.minorEndDate;
        if (start < minor) {
            start = minor;
        }
        let resourceIds = [];
        for (let resource of Object.values(me.simulator.data.resources)) {
            if (!resource.readonly) {
                resourceIds.push(resource.id);
            }
        }
        let jobs = me.createJobs(tasks, resourceIds);
        jobs[task.id].setup = true;
        let resources = {};
        let canStart = {};
        for (let resourceId of resourceIds) {
            let resource = me.simulator.data.resources[resourceId];
            canStart[resourceId] = me.simulator.getWorkMinutes(resource, me.simulator.today, start);
            resources[resourceId] = me.createResource(resource, tasks, start);
        }
        let solution = new Solution({
            start,
            jobs,
            resources,
            canStart,
            simulator: me.simulator,
        });
        solution.solve();
        return Object.values(solution.jobSolution);
    },
    moveAfterTasks(cmd, task, oldTask, resourceId, excludes) {
        let me = this;
        let removeGaps = me.simulator.settings.task_remove_gaps;
        let changes = [];
        let oldStart = oldTask.plan_start;
        if (oldTask.resource_id != resourceId) {
            //更换资源
            let start = task.plan_start;
            changes = me.updateAfterTasks(cmd, task.resource_id, start, task.status == 'done' ? start : task.plan_end, removeGaps, excludes);
            changes.push(task);
            changes.push(...me.updateAfterTasks(cmd, oldTask.resource_id, oldStart, oldStart, removeGaps, excludes));
        } else {
            if (task.plan_start <= oldTask.plan_end) {
                let start = task.plan_start;
                changes = me.updateAfterTasks(cmd, task.resource_id, start, task.status == 'done' ? start : task.plan_end, removeGaps, excludes);
            } else {
                changes = me.updateAfterTasks(cmd, resourceId, oldStart, oldStart, false, excludes);
            }
            changes.push(task);
        }
        return changes;
    },
    /**更新资源指定时间范围的任务*/
    updateAfterTasks(cmd, resourceId, start, end, removeGaps, excludes) {
        let me = this;
        let tasks = me.simulator.getResourceTask(resourceId);
        let afterTasks = tasks.filter(t => t.plan_end > start).sort((x, y) => {
            if (x.plan_start !== y.plan_start) {
                return x.plan_start.localeCompare(y.plan_start);
            }
            let ex = excludes.includes(x.id);
            let ey = excludes.includes(y.id);
            return ex && !ey ? -1 : !ex && ey ? 1 : 0;
        });
        if (!tasks.length) {
            return [];
        }
        let ids = afterTasks.map(t => t.id);
        let others = tasks.filter(t => !ids.includes(t.id));
        let resource = me.simulator.data.resources[resourceId];
        let prevTask;
        for (let task of afterTasks) {
            if (task.status == 'done') {
                prevTask = null;
                continue;
            }
            if (excludes.includes(task.id)) {
                //获取前任务计算转款
                // prevTask = getPrevTask
                me.resetToResource(task, prevTask, resourceId, cmd);
                if (end < task.plan_end) {
                    end = task.plan_end;
                }
                prevTask = task;
                continue;
            }
            if (removeGaps || moment(task.plan_start).diff(moment(end), 'seconds') <= 2) {
                if ((task.status == 'order' || task.is_locked) && end <= task.plan_start) {
                    me.resetToResource(task, prevTask, resourceId, cmd);
                    end = task.plan_end;
                    prevTask = task;
                    continue;
                }
                //需要转款
                //let transfer = (task.transferTime <= 0 || needTransfer()) && getTransferTime() > 0;
                let check = moment(end).diff(moment(task.work_start), 'seconds') > 2;
                if (check) {
                    if (task.is_locked) {
                        throw new Error('任务[{0}]已锁定，不能移动'.t().formatArgs(task.name));
                    }
                    if (task.status == 'order' && task.is_order_editable) {
                        throw new Error('任务[{0}]状态为已下达,不能移动'.t().formatArgs(task.name));
                    }
                }
                let snapshot = cmd.createSnapshot(task);
                end = me.simulator.getActualStart(resource, end, excludes, others).format('yyy-MM-DD HH:mm:ss');
                me.simulator.assignToResource(task.id, moment(end), resourceId, true, prevTask);
                cmd.updateSnapshot(snapshot, task);
            } else {
                me.resetToResource(task, prevTask, resourceId, cmd);
            }
            end = task.plan_end;
            prevTask = task;
        }
        return afterTasks;
    },
    resetToResource(task, prevTask, resourceId, cmd) {
        let me = this;
        let snapshot = cmd.createSnapshot(task);
        let start = task.plan_start;
        //计算转款时间
        me.simulator.assignToResource(task.id, moment(start), resourceId, true, prevTask);
        cmd.updateSnapshot(snapshot, task);
    },
    activeTask(gantt, taskId) {
        let me = this;
        let item = gantt.items[taskId];
        me.showTaskInfo(item ? item.data : null);
        let related = item ? Object.values(me.loadRelatedTasks(item.data)) : [];
        me.showRelatedTasks(related);
        gantt.dom.find(`.line-item.active`).removeClass("active");
        gantt.lines.dom.find('.line-header.highlight').removeClass('highlight').each(function () {
            let item = $(this);
            item.attr('title', item.attr('data-title'));
        });
        gantt.clearConnector();
        if (!item) {
            return;
        }
        item.dom.addClass('active');
        gantt.calendar.deadline(item.data.factory_due_date);
        gantt.calendar.showRange(item.dom.position().left, item.dom.width(), moment(item.data.plan_start).format('HH:mm'));
        for (let q of me.simulator.getTaskQuota(item.data)) {
            let item = gantt.lines.dom.find(`.line-header[data-id="${q.resource_id}"]`).addClass('highlight');
            item.attr('title', item.attr('data-title') + `\n${'优先级：'.t()}${q.priority}\n${'工时定额：'.t()}${q.cycle_time}s`);
            item.find('.cycle-time').html(q.cycle_time + "s");
            item.find('.priority').html(q.priority + "p");
        }
        if (me.simulator.settings.task_show_link) {
            for (let task of related) {
                for (let id of Object.keys(task.nexts || {})) {
                    let from = gantt.items[task.id];
                    let to = gantt.items[id];
                    if (from && to) {
                        gantt.drawConnector(from, to);
                    }
                }
            }
        }
    },
    loadRelatedTasks(task, related) {
        let me = this;
        if (!related) {
            related = {};
        }
        for (let id of Object.keys(task.nexts || {})) {
            let next = me.simulator.data.tasks[id];
            if (next) {
                if (!related[id]) {
                    related[id] = next;
                    me.loadRelatedTasks(next, related);
                }
            }
        }
        for (let id of Object.keys(task.prevs || {})) {
            let prev = me.simulator.data.tasks[id];
            if (prev) {
                if (!related[id]) {
                    related[id] = prev;
                    me.loadRelatedTasks(prev, related);
                }
            }
        }
        return related;
    },
    getTaskList(tasks) {
        let me = this;
        let list = [];
        for (let task of tasks) {
            let resource = me.simulator.data.resources[task.resource_id];
            let materialCode = {};
            let materialName = {};
            let productOrder = {};
            let craftProcess = {};
            let customerDueDate = {};
            for (let d of task.details) {
                let po = me.simulator.data.productOrders[d.product_order_id];
                let m = me.simulator.data.materials[d.material_id];
                let process = me.simulator.data.craftProcess[d.craft_process_id];
                productOrder[po.id] = po.code;
                customerDueDate[po.id] = po.customer_due_date;
                materialCode[m.id] = m.code;
                materialName[m.id] = m.name_spec;
                craftProcess[process.id] = process.name;
            }
            let data = {
                id: task.id,
                code: task.code,
                plan_start: task.plan_start,
                plan_end: task.plan_end,
                work_start: task.work_start,
                duration: task.duration,
                resource_id: [resource.id, resource.present],
                factory_due_date: task.factory_due_date,
                plan_qty: task.plan_qty,
                status: task.status,
                efficiency: task.efficiency * 100,
                product_order: Object.values(productOrder).join(";"),
                material_code: Object.values(materialCode).join(";"),
                material_name_spec: Object.values(materialName).join(";"),
                customer_due_date: Object.values(customerDueDate).join(";"),
                craft_process: Object.values(craftProcess).join(";"),
            }
            list.push(data);
        }
        return list;
    },
    showRelatedTasks(related) {
        let me = this;
        if (!me.view.taskRelatedGrid) {
            return;
        }
        me.view.taskRelatedGrid.data = me.getTaskList(related);
        me.view.taskRelatedGrid.load();
    },
    showTaskInfo(task) {
        let me = this;
        if (!me.view.taskInfoForm) {
            return;
        }
        let data = {};
        if (task) {
            let resource = me.simulator.data.resources[task.resource_id];
            let quota = me.simulator.getTaskQuota(task).find(q => q.resource_id == task.resource_id);
            let details = [];
            let craftTypeId;
            for (let d of task.details) {
                let po = me.simulator.data.productOrders[d.product_order_id];
                let m = me.simulator.data.materials[d.material_id];
                let process = me.simulator.data.craftProcess[d.craft_process_id];
                craftTypeId = process.craft_type_id;
                details.push({
                    plan_qty: d.plan_qty,
                    craft_process_id: [process.id, process.name],
                    output: d.output,
                    material_ready_date: d.material_ready_date,
                    material_id: [m.id, m.code],
                    material_name_spec: m.name_spec,
                    customer_id: po.customer_id,
                    customer_due_date: po.customer_due_date,
                    product_order_id: [po.id, po.code],
                    sales_order_id: po.sales_order_id,
                    order_qty: po.plan_qty,
                })
            }
            let mold = task.mold_id ? me.simulator.data.molds[task.mold_id] : null;
            data = {
                id: task.id,
                code: task.code,
                plan_start: task.plan_start,
                plan_end: task.plan_end,
                work_start: task.work_start,
                duration: task.duration,
                resource_id: [task.resource_id, resource.present],
                factory_due_date: task.factory_due_date,
                plan_qty: task.plan_qty,
                status: task.status,
                efficiency: task.efficiency * 100,
                cycle_time: quota ? '{0} 秒/单位'.t().formatArgs(quota.cycle_time) : '',
                craft_type_id: craftTypeId,
                mold_code: mold ? mold.code : '',
                mold_model: mold ? mold.model_id[1] : '',
                details
            }
        }
        me.view.taskInfoForm.setData(data);
        me.view.taskInfoForm.setReadonly(true);
    },
    async optimization(gantt, tasks, resourceIds, rules, start, cmd) {
        let me = this;
        me.activeTask(gantt);
        let minor = me.simulator.settings.minorEndDate;
        if (start < minor) {
            start = minor;
        }
        for (let task of tasks) {
            cmd.createSnapshot(task);
        }
        let jobs = me.createJobs(tasks, resourceIds);
        let resources = {};
        let canStart = {};
        for (let resourceId of resourceIds) {
            let resource = me.simulator.data.resources[resourceId];
            canStart[resourceId] = me.simulator.getWorkMinutes(resource, me.simulator.today, start);
            resources[resourceId] = me.createResource(resource, tasks, start);
        }
        let solutions = [];
        for (let rule of rules) {
            let solution = new Solution({
                start,
                jobs,
                resources,
                canStart,
                simulator: me.simulator,
            });
            solution.solve();
            solutions.push(solution);
        }
        return solutions;
    },
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    },
    createTaskData(task) {
        let data = {
            id: task.id,
            craft_type_id: task.craft_type_id,
            mold_id: task.mold_id,
            factory_due_date: task.factory_due_date,
            plan_qty: task.plan_qty,
            plan_start: task.plan_start,
            plan_end: task.plan_end,
            duration: task.duration,
            resource_id: task.resource_id,
            work_start: task.work_start,
            is_warning: task.is_warning,
            transfer_time: 0,
            efficiency: 1,
            details_ids: []
        }
        for (let d of task.details) {
            data.details_ids.push([0, 0, {
                craft_order_id: d.craft_order_id,
                plan_qty: d.plan_qty
            }]);
        }
        return data;
    },
    async submitSolution(gantt, solution) {
        let me = this;
        let jobSolution = Object.values(solution.jobSolution);
        let toCreate = [];
        let toUpdate = [];
        for (let js of jobSolution) {
            if (!js.job.setup) {
                me.simulator.assignToResource(js.job.id, js.from_time, js.resource_id, true);
            }
        }
        //TODO  更新冲突警告
        for (let js of jobSolution) {
            let task = me.simulator.data.tasks[js.job.id];
            if (task.phantom) {
                let data = me.createTaskData(task);
                toCreate.push(data);
            } else {
                toUpdate.push({
                    id: task.id,
                    plan_start: task.plan_start,
                    plan_end: task.plan_end,
                    duration: task.duration,
                    resource_id: task.resource_id,
                    work_start: task.work_start,
                    is_warning: task.is_warning,
                });
            }
        }
        let result = await me.view.rpc(me.view.model, 'saveTasks', {
            toCreate,
            toUpdate,
        });
        jmaa.utils.apply(true, me.simulator.data.materials, result.materials);
        jmaa.utils.apply(true, me.simulator.data.productOrders, result.productOrders);
        me.simulator.addTask(Object.values(result.tasks));
        let lineGroups = Object.values(gantt.lines.groups);
        let groups = {};
        for (let lines of lineGroups) {
            for (let line of lines) {
                let items = jobSolution.filter(js => js.resource_id == line.id);
                if (items.length) {
                    groups[line.id] = items;
                }
            }
        }
        for (let group of Object.values(groups)) {
            for (let js of group) {
                let item = gantt.items[js.job.id];
                if (!item) {
                    let task = result.tasks[js.job.id];
                    item = new GanttItem({
                        data: task,
                        view: gantt
                    });
                    gantt.items[task.id] = item;
                    item.draw();
                } else {
                    item.update();
                }
                item.dom.show();
                if (!js.job.setup) {
                    item.dirty(true);
                    gantt.locateItem(item.dom);
                }
                if (me.simulator.settings.task_show_animate) {
                    await me.sleep(150);
                }
            }
        }
    },
    getDateStart(resource, start) {
        let me = this;
        let date = moment(start).startOf('date');
        let minutes = start.diff(date, 'minutes');
        while (1) {
            let times = me.simulator.getCalendarTime(resource, date);
            for (let time of times) {
                if (time.start >= minutes) {
                    return date.add(time.start, 'minutes');
                } else if (time.start <= minutes && time.end > minutes) {
                    return date.add(minutes, 'minutes');
                }
            }
            minutes = 0;
            date.add(1, 'day');
        }
    },
    createResource(resource, tasks, start) {
        let me = this;
        let result = {id: resource.id, parallelism: resource.parallelism, space: [], used: []};
        let begin = me.getDateStart(resource, start).format('yyyy-MM-DD HH:mm:ss');
        let ts = {ids: [], list: []};
        for (let task of tasks) {
            if (task.status == 'new' && task.resource_id == resource.id && !task.is_locked) {
                ts.ids.push(task.id);
            }
        }
        for (let task of Object.values(me.simulator.data.tasks)) {
            if (task.resource_id == resource.id && task.status != 'done' && !ts.ids.includes(task.id) && task.plan_end > begin) {
                ts.list.push(task);
                ts.ids.push(task.id);
            }
        }
        let list = ts.list.sort((x, y) => x.plan_start.localeCompare(y.plan_start));
        let today = me.simulator.today;
        if (list.length) {
            let first = list.find(x => x.work_start <= begin);
            if (!first) {
                let to = me.simulator.getWorkMinutes(resource, today, moment(list[0].work_start));
                if (to > 0) {
                    let from = me.simulator.getWorkMinutes(resource, today, start);
                    result.space.push({from, to});
                }
            }
            for (let i = 0; i < list.length; i++) {
                let task = list[i];
                let from = me.simulator.getWorkMinutes(resource, today, moment(task.work_start));
                let to = me.simulator.getWorkMinutes(resource, today, moment(task.plan_end));
                let join = result.used.find(s => s.to == from);
                if (join) {
                    join.to = to;
                } else {
                    result.used.push({from, to});
                }
                if (i + 1 < list.length && moment(list[i + 1].work_start).diff(moment(task.plan_end), 'seconds') > 60) {
                    let nextFrom = me.simulator.getWorkMinutes(resource, today, moment(list[i + 1].work_start));
                    result.space.push({
                        from: to,
                        to: nextFrom,
                        from_time: task.plan_end,
                        to_time: list[i + 1].work_start,
                    })
                } else if (i + 1 >= list.length) {
                    let nextFrom = me.simulator.getWorkMinutes(resource, today, me.simulator.settings.endDate);
                    result.space.push({from: to, to: nextFrom});
                }
            }
        } else {
            let from = me.simulator.getWorkMinutes(resource, today, start);
            let to = me.simulator.getWorkMinutes(resource, today, me.simulator.settings.endDate);
            result.space.push({from, to});
        }
        return result;
    },
    createJobs(tasks, resourceIds) {
        let me = this;
        let jobs = {};
        for (let task of tasks) {
            me.createJob(jobs, task, resourceIds);
        }
        me.createRelatedJobs(jobs, tasks, resourceIds);
        for (let job of Object.values(jobs)) {
            let task = me.simulator.data.tasks[job.id];
            for (let id of Object.keys(task.prevs || {})) {
                let prevJob = jobs[id];
                let prevTask = me.simulator.data.tasks[id];
                job.prevs[id] = me.createJobRelationShip(task.prevs[id], prevTask, task, prevJob.durations, job.durations);
            }
            for (let id of Object.keys(task.nexts || {})) {
                let nextJob = jobs[id];
                let nextTask = me.simulator.data.tasks[id];
                job.nexts[id] = me.createJobRelationShip(task.nexts[id], task, nextTask, job.durations, nextJob.durations);
            }
        }
        return jobs;
    },
    createJobRelationShip(rs, task, nextTask, durations, nextDurations) {
        //TODO
        return {id: task.id, nextId: nextTask.id, rs}
    },
    /**根据生产订单创建相关的job*/
    createRelatedJobs(jobs, tasks, resourceIds) {
        let me = this;
        let orders = {};
        for (let task of tasks) {
            for (let d of task.details) {
                orders[d.product_order_id] = true;
            }
        }
        let distinctResource = {};
        for (let id of resourceIds) {
            distinctResource[id] = true;
        }
        for (let task of Object.values(me.simulator.data.tasks)) {
            if (!jobs[task.id]) {
                for (let d of task.details) {
                    if (orders[d.product_order_id]) {
                        if (!distinctResource[task.resource_id]) {
                            distinctResource[task.resource_id] = true;
                            resourceIds.push(task.resource_id);
                        }
                        let job = me.createJob(jobs, task, resourceIds);
                        job.setup = true;
                        break;
                    }
                }
            }
        }
    },
    /**把任务转为job*/
    createJob(jobs, task, resourceIds) {
        let me = this;
        if (jobs[task.id]) {
            return;
        }
        let resource = me.simulator.data.resources[task.resource_id];
        let today = me.simulator.today;
        let job = {
            id: task.id,
            resource_id: task.resource_id,
            setup: task.is_locked || task.status != 'new',
            done: task.status == 'done',
            factory_due_date: moment(task.factory_due_date),
            priority: {},
            deadline: {},
            durations: {},
            earliest_start: moment(me.simulator.getTaskEarliestStart(task) || me.simulator.today),
            from: me.simulator.getWorkMinutes(resource, today, moment(task.plan_start)),
            from_time: task.plan_start,
            to: me.simulator.getWorkMinutes(resource, today, moment(task.plan_end)),
            to_time: task.plan_end,
            start: me.simulator.getWorkMinutes(resource, today, moment(task.work_start)),
            start_time: task.work_start,
            nexts: {},
            prevs: {},
            //
        };
        if (resource && resource.readonly || task.is_locked) {
            job.durations[task.resource_id] = task.duration;
        } else {
            for (let resourceId of resourceIds) {
                let resource = me.simulator.data.resources[resourceId];
                let duration = me.simulator.getResourceDuration(task, resource);
                if (duration) {
                    job.durations[resourceId] = duration;
                    //最早可开工时间 ES Date
                    if (task.factory_due_date) {
                        job.deadline[resourceId] = me.simulator.getWorkMinutes(resource, today, moment(task.factory_due_date));
                    }
                }
                let quote = me.simulator.getTaskQuota(task).find(r => r.resource_id == resourceId);
                if (quote) {
                    job.priority[resourceId] = quote.priority;
                }
            }
        }
        jobs[job.id] = job;
        return job;
    },
    lockTasks(gantt, taskId, locked) {
        let me = this;
        let ids = [];
        let item = gantt.items[taskId];
        if (item.lock(locked)) {
            ids.push(taskId);
        }
        if (me.simulator.settings.task_lock_related) {
            if (locked) {
                ids.push(...me.lockPrevTasks(gantt, item.data, locked));
            } else {
                ids.push(...me.lockNextTasks(gantt, item.data, locked));
            }
        }
        return ids;
    },
    lockPrevTasks(gantt, task, locked) {
        let me = this;
        let ids = [];
        for (let id of Object.keys(task.prevs || {})) {
            let item = gantt.items[id];
            if (item.lock(locked)) {
                ids.push(id);
            }
            ids.push(...me.lockPrevTasks(gantt, item.data, locked));
        }
        return ids;
    },
    lockNextTasks(gantt, task, locked) {
        let me = this;
        let ids = [];
        for (let id of Object.keys(task.nexts || {})) {
            let item = gantt.items[id];
            if (item.lock(locked)) {
                ids.push(id);
            }
            ids.push(...me.lockNextTasks(gantt, item.data, locked));
        }
        return ids;
    }
});
