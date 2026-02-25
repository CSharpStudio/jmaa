//@ sourceURL=res_company.js
jmaa.editor('company', {
    extends:'editors.char',
    valid: function () {
        let me = this, val = this.getValue(), name = this.field.label;
        if (val.length > me.length) {
            return '当前长度'.t() + val.length + "超过最大长度".t() + me.length;
        }
        let org_code = me.owner.getData().org_code;
        let regex =  /^[^\u4e00-\u9fa5]+$/;
        if (val != '') {
            if (!regex.test(org_code)) {
                return '组织代码不能包含中文';
            }
        }
    }
});
