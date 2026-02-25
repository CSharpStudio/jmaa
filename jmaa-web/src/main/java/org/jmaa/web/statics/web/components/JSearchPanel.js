/**
 * 查询左侧面板
 */
jmaa.component("JSearchPanel", {
    /**
     * 显示最大限制
     */
    limit: 1000,
    /**
     * 显示字段名称
     */
    presentField: 'present',
    /**
     * 父字段名称
     */
    parentField: 'parent_id',
    /**
     * id字段名称
     */
    idField: 'id',
    /**
     * 设置
     * @param setting
     */
    config(setting) {
    },
    /**
     * 获取模板
     * @returns {`<div class="search-panel">
                    <label class="ml-2 mt-2 mb-0">${*}</label>
                    <div id="search_panel_${*}" class="ztree"></div>
                </div>`}
     */
    getTpl() {
        return `<div class="search-panel">
                    <label class="ml-2 mt-2 mb-0">${this.label}</label>
                    <div id="search_panel_${jmaa.nextId()}" class="ztree"></div>
                </div>`
    },
    /**
     * 初始化控件
     */
    init() {
        let me = this;
        me.selected = [];
        me._fields = [];
        if (me.arch) {
            let arch = jmaa.utils.parseXML(me.arch).children('aside');
            let name = arch.attr('field');
            let field = me.fields[name];
            if (field.type !== 'many2one') {
                throw new Error('aside not support：' + field.type);
            }
            me.label = me.nvl(arch.attr('label'), field.label);
            me.field = field;
            me.isTree = Boolean(eval(arch.attr("tree")));
            me.lookup = eval(arch.attr('lookup')) || [];
            me.presentField = me.nvl(arch.attr('present'), me.presentField);
            me._fields.push(me.presentField);
            if (me.isTree) {
                me._fields.push(me.parentField);
            }
            me.dom.html(me.getTpl());
            me.ztreeSetting = {
                view: {
                    showIcon: false,
                    selectedMulti: false
                },
                data: {
                    simpleData: {
                        enable: true,
                        pIdKey: "parent_id"
                    },
                    key: {
                        name: me.presentField
                    },
                },
                callback: {
                    onClick(e, id, node) {
                        me.selected = me.ztree.getSelectedNodes();
                        me.dom.triggerHandler("selected", [me, me.selected]);
                    }
                }
            };
            me.ztreeSetting.view.showLine = me.isTree;
            me.config(me.ztreeSetting);
        }
        me.dom.triggerHandler('init', [me]);
    },
    /**
     * 加载数据
     */
    load() {
        let me = this;
        me.ajax(me, function (data) {
            let all = {id: 'all'};
            all[me.presentField] = '全部'.t()
            data.splice(0, 0, all);
            me.ztree = $.fn.zTree.init(me.dom.find('.ztree'), me.ztreeSetting, data);
            me.ztree.expandAll(true);
            me.dom.triggerHandler('load', [me]);
        });
    },
    /**
     * 获取数据过滤条件
     * @returns
     */
    getLookup() {
        return this.lookup;
    },
    /**
     * 获取查询条件
     * @returns [[field,op,value]]
     */
    getCriteria() {
        let me = this;
        let sel = me.selected[0];
        if (sel && sel.id != 'all') {
            //TODO 处理树形数据递归
            return [[me.field.name, '=', sel.id]];
        }
        return [];
    },
    /**
     * 获取控件字段
     * @returns {[]}
     */
    getFields() {
        return this._fields;
    },
});
