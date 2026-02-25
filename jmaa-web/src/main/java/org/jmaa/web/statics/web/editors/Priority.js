jmaa.editor('priority', {
    extends: 'editors.radio',
    css: 'e-priority',
    fontIcon: "fa fa-star",
    getTpl() {
        let me = this;
        let name = me.name + "-group-" + jmaa.nextId();
        let options = [`<input name="${name}" value="" type="radio"><i></i>`];
        for (let key in me.options) {
            let id = me.name + "-" + jmaa.nextId();
            options.push(`<input id="${id}" name="${name}" value="${key}" type="radio">
                          <label for="${id}" class="${me.fontIcon}" title="${me.options[key]}"></label>`);
        }
        return `<div id="${this.getId()}">
                    ${options.join('')}
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.num = 0;
        let opt = dom.attr('options');
        if (opt) {
            me.options = eval("(" + opt + ")");
        } else {
            me.options = me.field.options || {};
        }
        dom.html(me.getTpl()).on('click', 'label', function (e) {
            if (!me.readonly()) {
                let id = $(this).attr('for');
                let radio = me.dom.find(`#${id}`);
                if (radio.prop('checked')) {
                    radio.prev().prev().prop('checked', true).trigger('change');
                    e.preventDefault();
                }
            }
        });
    }
});
