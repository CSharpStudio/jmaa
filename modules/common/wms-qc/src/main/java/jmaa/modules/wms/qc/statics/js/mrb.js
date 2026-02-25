//@ sourceURL=qc_mrb.js
jmaa.view({
    onFormLoad(e, form) {
        let me = this;
        let status = form.editors.status.getRawValue();
        if (['commit', 'close', 'done'].includes(status)) {
            form.setReadonly(true);
            me.toolbar.dom.find('button[name=save]').attr('disabled', true);
        }else{
            form.setReadonly(false);
            me.toolbar.dom.find('button[name=save]').attr('disabled', false);
        }
    },
});
