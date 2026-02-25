//@ sourceURL=KeyValue.js
jmaa.define("KeyValue", {
    __init__() {
        let me = this;
        me.values = [];
        me.keyValue = {};
    },
    add(key, value) {
        let me = this;
        me.keyValue[key] = value;
        me.values.push(value);
    },
    remove(key) {
        let me = this;
        let node = me.get(key);
        delete me.keyValue[key];
        me.values.remove(node);
        return node;
    },
    clear() {
        this.__init__();
    },
    get(key) {
        return this.keyValue[key];
    },
    getValues() {
        return this.values;
    },
    each(handler) {
        $.each(this.values, handler);
    },
    find(filter) {
        let found = [];
        $.each(this.values, function () {
            if (filter(this)) {
                found.push(this);
            }
        });
        return found;
    },
    first(filter) {
        let found = null;
        $.each(this.values, function () {
            if (filter(this)) {
                found = this;
                return false;
            }
        });
        return found;
    }
});
