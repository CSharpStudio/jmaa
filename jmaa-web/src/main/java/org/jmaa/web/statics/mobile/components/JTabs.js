/**
 * 页签
 * @example
 * <tabs>
 *     <tab label="a">
 *     </tab>
 *     <tab label="b">
 *     </tab>
 * </tabs>
 */
jmaa.component('JTabs', {
    init() {
        let me = this;
        let dom = me.dom;
        let position = dom.attr('position');
        let links = $(`<ul></ul>`);
        let contents = [];
        me.dom = $(`<div data-role="tabs"></div>`);
        me.copyAttr(dom, me.dom);
        dom.children('tab').each(function (i) {
            let tab = $(this);
            let label = tab.attr('label') || '';
            label && (label = label.t());
            let id = tab.attr('id') || 'tab-' + jmaa.nextId();
            let link = $(`<a href="#${id}" data-ajax="false">${label}</a>`);
            (i == 0) && link.addClass('ui-btn-active');
            me.copyAttr(tab, link);
            links.append($('<li></li>').html(link));
            contents.push($(`<div id="${id}" class="tab-content"></div>`).html(tab.children()));
        });
        let navbar = $(`<div data-role="navbar" class="jui-tabs-nav${position == 'footer' ? ' ui-footer ui-footer-fixed' : ''}"></div>`).html(links);
        me.dom.addClass('jui-tabs').append(navbar).append(contents)
    },
    copyAttr(from, to) {
        let me = this;
        for (let attr of from[0].attributes) {
            to.attr(attr.name, attr.value);
        }
    },
    open(name) {
        let me = this;
        me.dom.find('a.ui-tabs-anchor.ui-btn-active').removeClass('ui-btn-active');
        if (name) {
            me.dom.find(`a.ui-link[name=${name}]`).addClass('ui-btn-active').click();
        } else {
            me.dom.find(`a.ui-link:first`).addClass('ui-btn-active').click();
        }
    },
});
$.fn.JTabs = function (opt) {
    let com = $(this).data(name);
    if (!com) {
        com = new JTabs($.extend({dom: this}, opt));
        $(this).data(name, com);
    }
    return com;
};
