//@ sourceURL=aql_editor.js
jmaa.editor("aql-editor", {
    extends: 'editors.selection',
    filterData(keyword) {
        let me = this;
        let samplingPlanId = me.owner.getRawData("sampling_plan_id");
        if (me.samplingPlanId !== samplingPlanId) {
            me.samplingPlanId = samplingPlanId;
            me.dom.find('.dropdown-select ul').html(`<li>${'加载中'.t()}</li>`);
            jmaa.rpc({
                model: me.owner.model,
                module: me.owner.module,
                method: "getAqlList",
                args: {
                    samplingPlanId
                },
                onsuccess(r) {
                    if (r.data && r.data.length) {
                        let selected = me.dom.attr('data-value');
                        let options = [];
                        me.options = {};
                        for (const data of r.data) {
                            options.push(`<li class="options${selected == data ? ' selected' : ''}" value="${data}">${data}</li>`);
                            me.options[data] = data;
                        }
                        me.dom.find('.dropdown-select ul').html(options.join(''));
                    } else {
                        me.dom.find('.dropdown-select ul').html(`<li class="m-1 text-center">${'没有数据'.t()}</li>`);
                    }
                }
            });
        } else {
            me.callSuper(keyword);
        }
    },
    setValue(value) {
        let me = this;
        if (value != me.getValue()) {
            me.dom.find('li.selected').removeClass('selected');
            if (value) {
                if (Array.isArray(value)) {
                    me.dom.find(`li[value="${value[0]}"]`).addClass('selected');
                    me.dom.find('input').val(value[1]).attr('placeholder', '');
                    me.dom.attr('data-value', value[0]).data('text', value[1]).trigger('valueChange');
                } else {
                    me.dom.find(`li[value="${value}"]`).addClass('selected');
                    me.dom.find('input').val(value).attr('placeholder', '');
                    me.dom.attr('data-value', value).data('text', value).trigger('valueChange');
                }
            } else {
                me.dom.find('input').val('').attr('placeholder', me.placeholder);
                me.dom.attr('data-value', '').data('text', '').trigger('valueChange');
            }
        }
    },
});
