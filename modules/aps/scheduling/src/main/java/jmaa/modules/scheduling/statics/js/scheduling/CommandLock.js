//@ sourceURL=CommandLock.js
jmaa.define('CommandLock', {
    __init__(controller, gantt) {
        let me = this;
        me.snapshots = [];
        me.controller = controller;
        me.gantt = gantt;
        me.selected = [...me.gantt.selected];
        me.activedItem = me.gantt.activedItem ? me.gantt.activedItem.data.id : null;
    },
    undo() {
        let me = this;
        let toUpdate = [];
        for (let id of me.ids) {
            toUpdate.push({id, is_locked: !me.lock});
        }
        me.controller.view.rpc(me.controller.view.model, 'saveTasks', {
            toUpdate,
        }).then(d => {
            me.controller.view.log('撤销{0}[{1}]个任务'.t().formatArgs((me.lock ? '锁定' : '解锁').t(), me.ids.length));
            let toLocate = [];
            for (let id of me.ids) {
                let item = me.gantt.items[id];
                item.lock(!me.lock);
                toLocate.push(item.dom);
            }
            me.gantt.activeItem(me.activedItem);
            me.gantt.select(me.selected);
            me.gantt.locateItem(toLocate);
        });
    },
    redo() {
        let me = this;
        let toUpdate = [];
        for (let id of me.ids) {
            toUpdate.push({id, is_locked: me.lock});
        }
        me.controller.view.rpc(me.controller.view.model, 'saveTasks', {
            toUpdate,
        }).then(d => {
            me.controller.view.log('恢复{0}[{1}]个任务'.t().formatArgs((me.lock ? '锁定' : '解锁').t(), me.ids.length));
            let toLocate = [];
            for (let id of me.ids) {
                let item = me.gantt.items[id];
                item.lock(me.lock);
                toLocate.push(item.dom);
            }
            me.gantt.activeItem(me.activedItem);
            me.gantt.select(me.selected);
            me.gantt.locateItem(toLocate);
        });
    },
    createSnapshot(ids, lock) {
        let me = this;
        me.ids = ids;
        me.lock = lock;
    },
});
