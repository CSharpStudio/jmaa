jmaa.editor('password', {
    extends: "editors.char",
    getTpl() {
        return `<input type="password" autocomplete="off" focusable class="form-control" id="${this.getId()}"/>`;
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', 'input:password', function (e) {
            handler(e, me);
        });
    },
});
