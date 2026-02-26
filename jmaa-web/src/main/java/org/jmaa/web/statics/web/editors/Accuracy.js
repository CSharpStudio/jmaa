jmaa.editor('accuracy', {
    extends: 'editors.float',
    init() {
        let me = this;
        me.decimals = 10;
        me.callSuper();
        let field = me.dom.attr('accuracy');
        me.owner.editors[field].onValueChange(function () {
            let accuracy = me.owner.getData()[field];
            me.setAttr('data-decimals', accuracy);
        });
    }
});
