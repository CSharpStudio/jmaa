//@ sourceURL=rpt_dataset.js
jmaa.view({
    onFormLoad(e, form) {
        let editor = form.editors.content.codeEditor;
        editor.refresh();
        editor.setOption('mode', 'sql');
    }
});
