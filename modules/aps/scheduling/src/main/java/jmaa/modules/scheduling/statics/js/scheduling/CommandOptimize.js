//@ sourceURL=CommandOptimize.js
jmaa.define('CommandOptimize', {
    __init__(controller, gantt) {
        let me = this;
        me.snapshots = [];
        me.controller = controller;
        me.gantt = gantt;
        me.selected = [...me.gantt.selected];
        me.activedItem = me.gantt.activedItem ? me.gantt.activedItem.data.id : null;
    },
    undo(reset) {
        let me = this;
        let update = function () {
            let toLocate = [];
            for (let i = me.snapshots.length - 1; i > -1; i--) {
                let snapshot = me.snapshots[i];
                let item = me.gantt.items[snapshot.taskId];
                if (snapshot.phantom) {
                    if (item) {
                        item.dom.remove();
                    }
                    delete me.gantt.items[snapshot.taskId];
                    delete me.controller.simulator.data.tasks[snapshot.taskId];
                } else {
                    jmaa.utils.apply(true, item.data, snapshot.oldValue);
                    item.update();
                    toLocate.push(item.dom);
                }
            }
            me.gantt.activeItem(me.activedItem);
            me.gantt.select(me.selected);
            if (toLocate.length) {
                me.gantt.locateItem(toLocate);
            }
        }
        if (reset) {
            update();
        } else {
            let toDelete = [];
            let toUpdate = {};
            for (let i = me.snapshots.length - 1; i > -1; i--) {
                let snapshot = me.snapshots[i];
                if (snapshot.phantom) {
                    toDelete.push(snapshot.taskId);
                } else {
                    toUpdate[snapshot.taskId] = jmaa.utils.apply(true, {id: snapshot.taskId}, snapshot.oldValue);
                }
            }
            me.controller.view.rpc(me.controller.view.model, "saveTasks", {
                toUpdate: Object.values(toUpdate),
                toDelete,
            }).then(r => {
                let total = Object.keys(toUpdate).length + toDelete.length;
                me.controller.view.log('撤销排程[{0}]个任务'.t().formatArgs(total));
                update();
            });
        }
    },
    redo() {
        let me = this;
        let toUpdate = {};
        let toCreate = [];
        for (let snapshot of me.snapshots) {
            if (snapshot.phantom) {
                let task = snapshot.oldValue;
                let data = me.controller.createTaskData(task);
                toCreate.push(data);
            } else {
                toUpdate[snapshot.taskId] = jmaa.utils.apply(true, {id: snapshot.taskId}, snapshot.newValue);
            }
        }
        me.controller.view.rpc(me.controller.view.model, "saveTasks", {
            toUpdate: Object.values(toUpdate),
            toCreate
        }).then(r => {
            let total = Object.keys(toUpdate).length + toCreate.length;
            me.controller.view.log('恢复排程[{0}]个任务'.t().formatArgs(total));
            let toLocate = [];
            for (let i = 0; i < me.snapshots.length; i++) {
                let snapshot = me.snapshots[i];
                let item;
                if (snapshot.phantom) {
                    let task = r.tasks[snapshot.taskId];
                    me.controller.simulator.addTask(task);
                    item = new GanttItem({
                        data: task,
                        view: me.gantt
                    });
                    me.gantt.items[task.id] = item;
                    item.draw();
                    toLocate.push(item.dom);
                } else {
                    item = me.gantt.items[snapshot.taskId];
                    jmaa.utils.apply(true, item.data, snapshot.newValue);
                    item.update();
                    toLocate.push(item.dom);
                }
            }
            me.gantt.select(me.selected);
            me.gantt.locateItem(toLocate);
        });
    },
    createSnapshot(task) {
        let me = this;
        let snapshot = {taskId: task.id};
        if (task.phantom) {
            snapshot.phantom = true;
            snapshot.oldValue = jmaa.utils.apply(true, {}, task);
        } else {
            snapshot.oldValue = {
                plan_start: task.plan_start,
                plan_end: task.plan_end,
                work_start: task.work_start,
                duration: task.duration,
                resource_id: task.resource_id,
            }
        }
        me.snapshots.push(snapshot);
        return snapshot;
    },
    updateSnapshot(task) {
        let me = this;
        let snapshot = me.snapshots.find(s => s.taskId == task.id);
        if (snapshot.phantom) {
            snapshot.oldValue.plan_start = task.plan_start;
            snapshot.oldValue.plan_end = task.plan_end;
            snapshot.oldValue.work_start = task.work_start;
            snapshot.oldValue.duration = task.duration;
            snapshot.oldValue.resource_id = task.resource_id;
            snapshot.oldValue.is_warning = task.is_warning;
            snapshot.oldValue.transfer_time = task.transfer_time;
        } else {
            snapshot.newValue = {
                plan_start: task.plan_start,
                plan_end: task.plan_end,
                work_start: task.work_start,
                duration: task.duration,
                resource_id: task.resource_id,
                is_warning: task.is_warning,
                transfer_time: task.transfer_time,
            };
        }
    }
});
