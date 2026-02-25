//@ sourceURL=ir_assets.js
jmaa.view({
    onFormLoad(e, form) {
        let editor = form.editors.content.codeEditor;
        editor.refresh();
        let type = form.editors.type.getValue();
        if ('js' == type || 'json' == type) {
            editor.setOption('mode', 'javascript');
        } else if ('css' == type) {
            editor.setOption('mode', 'css');
        } else if ('svg' == type) {
            editor.setOption('mode', 'xml');
        }
    }
});
