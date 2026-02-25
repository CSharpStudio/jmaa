//@ sourceURL=msg_editor.js
jmaa.editor('msg_editor', {
    getTpl: function () {
        return `<div class="collect-msg" id="${this.getId()}">
                    <div class="msg-list"></div>
                </div>`;
    },
    init: function () {
        let me = this;
        me.dom.html(me.getTpl());
        me.rows = 0;
    },
    setValue: function (value) {
        let me = this;
        if (value && value.msg) {
            me.dom.find('.msg-bold').removeClass('msg-bold');
            me.dom.find('.msg-list').prepend(`<span class="msg-bold ${value.error ? 'msg-error' : 'msg-info'}">> ${value.msg}</span>`).scrollTop(0);
            if (me.rows > 10) {
                me.dom.find('.msg-list span').last().remove();
            } else {
                me.rows++;
            }
        }
    },
    reset() {
        let me = this;
        me.rows = 0;
        me.dom.find('.msg-list').empty();
    }
});
