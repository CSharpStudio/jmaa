//@ sourceURL=CommandDelete.js
jmaa.define('CommandDelete', {
    __init__(controller, gantt) {
        let me = this;
        me.snapshots = [];
        me.controller = controller;
        me.gantt = gantt;
    },
    undo() {
        let me = this;
        let toCreate = [];
        for (let task of me.snapshots) {
            let data = me.controller.createTaskData(task);
            toCreate.push(data);
        }
        me.controller.view.rpc(me.controller.view.model, 'saveTasks', {
            toCreate,
        }).then(r => {
            me.controller.view.log('撤销删除[{0}]个任务'.t().formatArgs(me.snapshots.length));
            me.snapshots = [];
            let tasks = Object.values(r.tasks);
            let toLocate = [];
            for (let i = 0; i < tasks.length; i++) {
                let task = tasks[i];
                me.controller.simulator.data.tasks[task.id] = task;
                let item = new GanttItem({
                    data: task,
                    view: me.gantt
                });
                me.gantt.items[task.id] = item;
                item.draw();
                me.createSnapshot(task);
                toLocate.push(item.dom);
                if (i == tasks.length - 1) {
                    me.gantt.activeItem(task.id);
                }
            }
            me.gantt.locateItem(toLocate);
            me.gantt.select(Object.keys(r.tasks));
        });
    },
    redo() {
        let me = this;
        let toDelete = [];
        for (let task of me.snapshots) {
            toDelete.push(task.id);
        }
        me.controller.view.rpc(me.controller.view.model, 'saveTasks', {
            toDelete: toDelete,
        }).then(d => {
            me.controller.view.log('恢复删除[{0}]个任务'.t().formatArgs(toDelete.length));
            for (let id of toDelete) {
                delete me.controller.simulator.data.tasks[id];
                me.gantt.items[id].dom.remove();
                delete me.gantt.items[id];
            }
            me.gantt.select([]);
            me.gantt.activeItem();
        });
    },
    createSnapshot(task) {
        let me = this;
        let snapshot = jmaa.utils.apply(true, {}, task);
        me.snapshots.push(snapshot);
        return snapshot;
    }
});
