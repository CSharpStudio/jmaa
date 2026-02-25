//@ sourceURL=inspect_mark.js
jmaa.editor('inspect_mark', {
    setValue(value) {
        let me = this;
        if (value && value.length) {
            me.dom.html(`
                <div class="inspect_mark" style="width: 95px;height: 80px;margin-top: 14px">
                    <img class="img" src="/web/jmaa/modules/wms/qc/statics/img/${value[0]}.svg">
                    <div class="text">
                      <span>${value[1]}</span>
                    </div>
                </div>`);
        } else {
            me.dom.html('');
        }
    }
});
