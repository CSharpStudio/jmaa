/**
 * 树型控件，基于zTree，参见https://treejs.cn/v3/api.php
 */
jmaa.component("JTree", {
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
     * 排序字段名称
     */
    sortField: null,
    /**
     * ajax绑定数据
     * @param tree
     * @param callback 数据绑定回调函数
     */
    ajax(tree, callback) {
    },
    /**
     * 初始化控件
     */
    init() {
        let me = this;
        me.ztreeSetting = {
            view: {
                showIcon: false,
                selectedMulti: false
            },
            edit: {
                enable: true,
                drag: {},
                showRemoveBtn: false,
                showRenameBtn: false
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
                    let selected = me.ztree.getSelectedNodes();
                    me.dom.triggerHandler('selected', [me, selected]);
                }
            }
        };
        me.fields = [];
        me.fields.push(me.parentField);
        me.fields.push(me.presentField);
        if (me.sortField) {
            me.fields.push(me.sortField);
        }
        me.dom.addClass("ztree");
        me.dom.triggerHandler('init', [me]);
        if (me.data) {
            me.ztree = $.fn.zTree.init(me.dom, me.ztreeSetting, me.data);
            me.onLoad();
        }
    },
    /**
     * 加载数据
     */
    load() {
        let me = this;
        me.ajax(me, function (data) {
            me.ztree = $.fn.zTree.init(me.dom, me.ztreeSetting, data);
            me.data = data;
            me.onLoad();
        });
    },
    onLoad() {
        let me = this;
        let urlHash = jmaa.web.getParams(window.location.hash.substring(1));
        let node = me.ztree.getNodeByParam("id", urlHash.id);
        if (node) {
            me.ztree.selectNode(node);
        } else {
            me.ztree.expandAll(false);
        }
        me.dom.triggerHandler('load', [me]);
    },
    /**
     * 获取控件字段
     * @returns {[]}
     */
    getFields() {
        return this.fields;
    },
    /**
     * 获取使用Present的字段名称列表
     * @returns {boolean}
     */
    getUsePresent() {
        return true;
    },
    /**
     * 展开选中节点
     */
    expandSelected() {
        let me = this;
        if (me.ztree) {
            let nodes = me.ztree.getSelectedNodes();
            if (nodes.length > 0) {
                me.ztree.expandNode(nodes[0], true, true, true);
            }
        }
    },
    /**
     * 展开所有节点
     */
    expandAll() {
        let me = this;
        if (me.ztree) {
            me.ztree.expandAll(true);
        }
    },
    /**
     * 收起选中节点
     */
    collapseSelected() {
        let me = this;
        if (me.ztree) {
            let nodes = me.ztree.getSelectedNodes();
            if (nodes.length > 0) {
                me.ztree.expandNode(nodes[0], false, true, true);
            }
        }
    },
    /**
     * 收起所有节点
     */
    collapseAll() {
        let me = this;
        if (me.ztree) {
            me.ztree.expandAll(false);
        }
    }
});
