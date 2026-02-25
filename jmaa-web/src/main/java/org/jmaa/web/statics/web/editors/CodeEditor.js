//@ sourceURL=CodeEditor.js
jmaa.editor('code-editor', {
    css: 'e-code-editor',
    mode: 'xml',
    getTpl() {
        return `<textarea id="${this.getId()}" rows="${this.rows}" type="text" class="form-control"/>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.rows = me.nvl(dom.attr("rows"), me.rows, 20);
        me.mode = me.nvl(dom.attr('mode'), me.mode);
        dom.html(me.getTpl());
        me.codeEditor = CodeMirror.fromTextArea(document.getElementById(me.getId()), {
            lineNumbers: true,
            mode: me.mode,
            lineWrapping: true
        });
        me.codeEditor.on('change', function (instance, changeObj) {
            me.dom.trigger('change');
        });
        me.codeEditor.setSize('auto', `${me.rows * 24.5}px`);
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        let me = this;
        me.codeEditor.setOption('readOnly', readonly);
    },
    getValue() {
        let me = this;
        return me.codeEditor.getValue();
    },
    setValue(value) {
        let me = this;
        if (value === null) {
            value = "";
        }
        if (value != me.getValue()) {
            me.codeEditor.setValue(value);
            me.dom.trigger('change');
        }
    }
});
