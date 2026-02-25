//@ sourceURL=enterprise_model.js
jmaa.view({
    beforeDrop(treeId, treeNodes, targetNode, moveType) {
        let me = this;
        for(let n of treeNodes){
            if(n.parent_id !=  targetNode.parent_id){
                return false;
            }
        }
    }
});
jmaa.editor("enterprise_tpl_selector", {
    extends: 'editors.selection',
    usePresent: true,
    filterData(keyword) {
        let me = this;
        let parentId = me.owner.getEditor("parent_id").getRawValue();
        if (me.parentId !== parentId) {
            me.parentId = parentId;
            me.dom.find('.dropdown-select ul').html(`<li>${'加载中'.t()}</li>`);
            jmaa.rpc({
                model: "md.enterprise_model",
                module: me.view.module,
                method: "getTemplate",
                args: {
                    parentId
                },
                onsuccess(r) {
                    if (r.data && Object.keys(r.data).length) {
                        me.options = r.data;
                        let selected = me.dom.attr('data-value');
                        let options = [];
                        for (const key in me.options) {
                            options.push(`<li class="options${selected == key ? ' selected' : ''}" value="${key}">${me.options[key]}</li>`);
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
});
