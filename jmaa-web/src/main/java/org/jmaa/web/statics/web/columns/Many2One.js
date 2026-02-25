jmaa.column('many2one', {
    render() {
        let me = this;
        if(eval(me.arch.attr('link'))){
            me.owner.dom.on('click', `[link=${me.field.name}]`, function () {
                let dom = $(this);
                jmaa.showDialog({
                    title: me.field.label,
                    init(dialog) {
                        dialog.body.html(`<iframe style="width: 100%; border: 0px; height: ${$(document.body).height() - 300}px;" src="${jmaa.web.getTenantPath()}/view#model=${me.field.comodel}&id=${dom.attr('data-value')}&views=form&top=1&readonly=1"/>`)
                    }
                });
            });
        }
        return function (data, type, row) {
            if (data && data[0]) {
                if(eval(me.arch.attr('link'))){
                    return `<a link="${me.field.name}" class="char link-column" data-value="${data[0]}">${data[1]}</a>`;
                }
                return `<span class="char" data-value="${data[0]}">${data[1]}</span>`;
            }
            return '';
        }
    }
});
jmaa.column('many2one_reference', {
    render() {
        let me = this;
        me.owner.dom.on('click', `[link=${me.field.name}]`, function () {
            let dom = $(this);
            jmaa.showDialog({
                title: me.field.label,
                init(dialog) {
                    dialog.body.html(`<iframe style="width: 100%; border: 0px; height: ${$(document.body).height() - 300}px;" src="${jmaa.web.getTenantPath()}/view#model=${dom.attr('data-model')}&id=${dom.attr('data-value')}&views=form&top=1&readonly=1"/>`)
                }
            });
        });
        return function (data, type, row) {
            if (data && data[0]) {
                if(eval(me.arch.attr('link'))){
                    return `<a link="${me.field.name}" class="char link-column" data-value="${data[0]}" data-model="${row[me.field.modelField]}">${data[1]}</a>`;
                }
                return `<span class="char" data-value="${data[0]}">${data[1]}</span>`;
            }
            return '';
        }
    }
});
jmaa.column('ref-link', {
    render() {
        let me = this;
        let idField = me.arch.attr('id-field');
        let modelField = me.arch.attr('model-field');
        me.owner.dom.on('click', `[link=${me.field.name}]`, function () {
            let dom = $(this);
            jmaa.showDialog({
                title: me.field.label,
                init(dialog) {
                    dialog.body.html(`<iframe style="width: 100%; border: 0px; height: ${$(document.body).height() - 300}px;" src="${jmaa.web.getTenantPath()}/view#model=${dom.attr('data-model')}&id=${dom.attr('data-id')}&views=form&top=1&readonly=1"/>`)
                }
            });
        });
        return function (data, type, row) {
            if (data === null || data === undefined) {
                data = '';
            }
            if(row[idField] && row[modelField]){
                return `<a link="${me.field.name}" class="char link-column" data-id="${row[idField]}" data-model="${row[modelField]}">${data}</a>`;
            }
            return data;
        }
    }
});
