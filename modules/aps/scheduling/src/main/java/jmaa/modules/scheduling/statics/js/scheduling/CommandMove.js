//@ sourceURL=CommandMove.js
jmaa.define('CommandMove', {
    __init__(controller, gantt) {
        let me = this;
        me.snapshots = [];
        me.controller = controller;
        me.gantt = gantt;
        me.selected = [...me.gantt.selected];
    },
    undo(reset) {
        let me = this;
        let update = function () {
            for (let i = me.snapshots.length - 1; i > -1; i--) {
                let snapshot = me.snapshots[i];
                let item = me.gantt.items[snapshot.taskId];
                jmaa.utils.apply(true, item.data, snapshot.oldValue);
                item.update();
                if (i == 0) {
                    me.gantt.locateItem(item.dom);
                    me.gantt.activeItem(snapshot.taskId);
                }
            }
            me.gantt.select(me.selected);
        }
        if (reset) {
            update();
        } else {
            let toUpdate = {};
            for (let i = me.snapshots.length - 1; i > -1; i--) {
                let snapshot = me.snapshots[i];
                toUpdate[snapshot.taskId] = jmaa.utils.apply(true, {id: snapshot.taskId}, snapshot.oldValue);
            }
            me.controller.view.rpc(me.controller.view.model, "saveTasks", {
                toUpdate: Object.values(toUpdate),
            }).then(r => {
                me.controller.view.log('撤销移动[{0}]个任务'.t().formatArgs(Object.keys(toUpdate).length));
                update();
            });
        }
    },
    redo() {
        let me = this;
        let toUpdate = {};
        for (let snapshot of me.snapshots) {
            toUpdate[snapshot.taskId] = jmaa.utils.apply(true, {id: snapshot.taskId}, snapshot.newValue);
        }
        me.controller.view.rpc(me.controller.view.model, "saveTasks", {
            toUpdate: Object.values(toUpdate),
        }).then(r => {
            me.controller.view.log('恢复移动[{0}]个任务'.t().formatArgs(Object.keys(toUpdate).length));
            for (let i = 0; i < me.snapshots.length; i++) {
                let snapshot = me.snapshots[i];
                let item = me.gantt.items[snapshot.taskId];
                jmaa.utils.apply(true, item.data, snapshot.newValue);
                item.update();
                if (i == 0) {
                    me.gantt.locateItem(item.dom);
                    me.gantt.activeItem(snapshot.taskId);
                }
            }
            me.gantt.select(me.selected);
        });
    },
    createSnapshot(task) {
        let me = this;
        let snapshot = {
            taskId: task.id,
            oldValue: {
                plan_start: task.plan_start,
                plan_end: task.plan_end,
                work_start: task.work_start,
                duration: task.duration,
                resource_id: task.resource_id,
                is_warning: task.is_warning,
                transfer_time: task.transfer_time,
            }
        };
        me.snapshots.push(snapshot);
        return snapshot;
    },
    updateSnapshot(snapshot, task) {
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
});
