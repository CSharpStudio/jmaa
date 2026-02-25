jmaa.component('JSelect', {
    getTpl() {
        let me = this;
        let opt = {};
        me.select.find('option').each(function () {
            let o = $(this);
            opt[o.attr('value')] = o.html();
        });
        let li = [];
        for (let key of Object.keys(opt)) {
            li.push(`<li class="jui-select-item" value="${key}">${opt[key]}</li>`);
        }
        let css = me.select.attr('class') || '';
        return `<div class="jui-select-button ${css}" data-toggle="dropdown">
                <span class="jui-select-result"></span>
                <i class="fa fa-angle-down"></i>
            </div>
            <div class="jui-select-dropdown dropdown-menu">
                <ul>${li.join('')}</ul>
            </div>`;
    },
    init() {
        let me = this;
        me.dom.wrap('<div class="jui-select"></div>');
        me.dom = me.dom.parent();
        me.select = me.dom.find('select').css('display', 'none');
        me.dom.append(me.getTpl());
        let selected = me.select.find('option:selected');
        if (selected.length) {
            me.dom.find('.jui-select-result').html(selected.text());
        }
        me.dom.on('click', '.jui-select-item', function () {
            let item = $(this);
            let value = item.attr('value');
            me.select.val(value).trigger('change');
            me.dom.find('.jui-select-result').html(me.select.find('option:selected').text());
        });
    }
});
