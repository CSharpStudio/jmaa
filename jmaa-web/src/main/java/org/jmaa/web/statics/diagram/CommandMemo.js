//@ sourceURL=CommandMemo.js
jmaa.component("CommandMemo", {
    init() {
        let me = this;
        me.observer = [];
        me.undoStack = [];
        me.redoStack = [];
        me.change && me.onChange(me.change);
    },
    add(cmd) {
        let me = this;
        me.redoStack = [];
        me.undoStack.push(cmd);
        me.triggerChange();
    },
    clear() {
        let me = this;
        me.undoStack = [];
        me.redoStack = [];
        me.triggerChange();
    },
    clearRedo() {
        let me = this;
        me.redoStack = [];
        me.triggerChange();
    },
    clearUndo() {
        let me = this;
        me.undoStack = [];
        me.triggerChange();
    },
    undo() {
        let me = this;
        if (me.undoStack.length > 0) {
            let cmd = me.undoStack.pop();
            cmd.undo();
            me.redoStack.push(cmd);
            me.triggerChange();
            return cmd;
        }
    },
    redo() {
        let me = this;
        if (me.redoStack.length > 0) {
            let cmd = me.redoStack.pop();
            cmd.redo();
            me.undoStack.push(cmd);
            me.triggerChange();
            return cmd;
        }
    },
    canRedo() {
        return this.redoStack.length > 0;
    },
    canUndo() {
        return this.undoStack.length > 0;
    },
    triggerChange() {
        let me = this;
        $.each(me.observer, function () {
            this(me);
        });
    },
    onChange(handler) {
        this.observer.push(handler);
    }
});
