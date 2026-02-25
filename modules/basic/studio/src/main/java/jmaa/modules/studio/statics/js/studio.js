//@ sourceURL=studio.js
jmaa.view({
    design: true,
    getViewTpl() {
        return `<div class="view-items">
            <div class="view-item inactive" type="form">
                <span class="active-view">激活视图</span>
                <img src="/web/jmaa/modules/studio/statics/icons/form.svg"/>
                <div class="b-row">
                    <div class="title">表单</div>
                    <div class="menu"><i class="fa fa-ellipsis-v"></i></div>
                </div>
            </div>
            <div class="view-item inactive" type="search">
                <span class="active-view">激活视图</span>
                <img src="/web/jmaa/modules/studio/statics/icons/search.svg"/>
                <div class="b-row">
                    <div class="title">查询</div>
                    <div class="menu"><i class="fa fa-ellipsis-v"></i></div>
                </div>
            </div>
            <div class="view-item inactive" type="grid">
                <span class="active-view">激活视图</span>
                <img src="/web/jmaa/modules/studio/statics/icons/grid.svg"/>
                <div class="b-row">
                    <div class="title">列表</div>
                    <div class="menu"><i class="fa fa-ellipsis-v"></i></div>
                </div>
            </div>
            <div class="view-item inactive" type="lookup">
                <span class="active-view">激活视图</span>
                <img src="/web/jmaa/modules/studio/statics/icons/lookup.svg"/>
                <div class="b-row">
                    <div class="title">下拉框</div>
                    <div class="menu"><i class="fa fa-ellipsis-v"></i></div>
                </div>
            </div>
            <div class="view-item inactive" type="card">
                <span class="active-view">激活视图</span>
                <img src="/web/jmaa/modules/studio/statics/icons/card.svg"/>
                <div class="b-row">
                    <div class="title">卡片</div>
                    <div class="menu"><i class="fa fa-ellipsis-v"></i></div>
                </div>
            </div>
        </div>`;
    },
    getTpl() {
        return `<div class="studio-head">
                    <div class="title"></div>
                    <div type="info" class="tab active">${'基本信息'.t()}</div>
                    <div type="form" class="tab d-none">${'表单'.t()}</div>
                    <div type="search" class="tab d-none">${'查询'.t()}</div>
                    <div type="grid" class="tab d-none">${'表格'.t()}</div>
                    <div type="card" class="tab d-none">${'卡片'.t()}</div>
                    <div type="lookup" class="tab d-none">${'下拉'.t()}</div>
                    <div type="mobile" class="tab d-none">${'移动端'.t()}</div>
                    <div type="resource" class="tab">${'资源'.t()}</div>
                    <div class="btn btn-view"><i class="fa fa-plus mr-1"></i>${'视图'.t()}</div>
                </div>
                <div class="studio-content">
                    <div tab="info" class="tab-panel active">${'基础信息'.t()}</div>
                    <div tab="source-code" class="tab-panel"></div>
                </div>`;
    },
    init() {
        let me = this;
        let ps = jmaa.web.getParams(window.location.hash.substring(1));
        me.editors = {};
        me.data = {};
        me.data.model = ps.name;
        me.dom.addClass('studio').html(me.getTpl()).unbind().on('click', '.studio-head .tab', function () {
            me.dom.find('.studio-head .tab.active,.studio-content .tab-panel.active').removeClass('active');
            let tab = $(this).addClass('active');
            let type = tab.attr('type');
            let panel = me.dom.find(`.studio-content [tab=${type}]`);
            if (panel.length) {
                panel.addClass('active');
                if (type == 'info') {
                    me.form && me.form.load();
                } else {
                    me.editors[type].loadViewData();
                }
            } else {
                me.createTab(type);
            }
            me.urlHash.tab = type;
            window.location.hash = $.param(me.urlHash);
        }).on('click', '.s-collapse', function () {
            let b = $(this);
            if (b.hasClass('open')) {
                b.next().addClass("d-none");
                b.removeClass('open');
            } else {
                b.next().removeClass("d-none");
                b.addClass('open');
            }
        }).on('click', '.btn-view', function () {
            me.editView();
        });
        if (!eval(jmaa.web.cookie('ctx_debug'))) {
            me.dom.find('[type=resource]').remove();
        }
        me.initTabs();
    },
    editView() {
        let me = this;
        jmaa.showDialog({
            title: '视图'.t(),
            css: 'modal-md',
            init(dialog) {
                dialog.body.html(me.getViewTpl()).on('click', '.menu', function () {
                    me.openMenu(dialog, $(this));
                }).on('click', '.menu', function () {
                    me.openMenu(dialog, $(this));
                }).on('click', '.inactive', function () {
                    let type = $(this).attr('type');
                    me.activeView(type, function () {
                        dialog.close();
                        me.urlHash.tab = type;
                        window.location.hash = $.param(me.urlHash);
                        me.init();
                    });
                }).on('click', '.menu-inactive', function () {
                    let type = $(this).closest('.dropdown-menu').attr('type');
                    me.inactiveView(type, function () {
                        dialog.close();
                        me.init();
                    });
                }).on('click', '.menu-restore', function () {
                    let type = $(this).closest('.dropdown-menu').attr('type');
                    me.restoreView(type, function () {
                        dialog.close();
                        me.init();
                    });
                });
                let type = [];
                for (let view of me.data.views) {
                    type.push(`[type=${view}]`);
                }
                dialog.body.find(type.join(',')).addClass('active').removeClass('inactive');
                dialog.dom.on('mouseup', function (e) {
                    let target = $(e.target);
                    if (!target.closest('.menu').length) {
                        dialog.body.find('.dropdown-menu').hide();
                    }
                });
            }
        })
    },
    callService(method, args, callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            method: method,
            args,
            onsuccess(r) {
                callback();
            }
        });
    },
    restoreView(type, callback) {
        let me = this;
        jmaa.msg.confirm({
            title: '确认'.t(),
            content: '确认恢复默认视图吗?'.t(),
            submit() {
                me.callService("restoreView", {
                    model: me.data.model,
                    type,
                }, callback);
            }
        });
    },
    activeView(type, callback) {
        let me = this;
        jmaa.msg.confirm({
            title: '确认'.t(),
            content: '确认激活视图吗?'.t(),
            submit() {
                me.callService("activeView", {
                    model: me.data.model,
                    type,
                }, callback);
            }
        });
    },
    inactiveView(type, callback) {
        let me = this;
        jmaa.msg.confirm({
            title: '确认'.t(),
            content: '确认禁用视图吗?'.t(),
            submit() {
                me.callService("inactiveView", {
                    model: me.data.model,
                    type,
                }, callback);
            }
        })
    },
    openMenu(dialog, menu) {
        let me = this;
        let item = menu.closest('.view-item');
        let m = dialog.body.find('.dropdown-menu');
        if (!m.length) {
            m = $(`<div class="dropdown-menu">
                    <span role="button" class="menu-restore dropdown-item">${'恢复默认'.t()}</span>
                    <span role="button" class="menu-inactive dropdown-item">${'禁用'.t()}</span>
                </div>`);
            dialog.body.append(m);
        }
        let itemPosition = item.position();
        let menuPosition = menu.position();
        m.css({
            left: itemPosition.left + item.width() + 22 - m.width(),
            top: menuPosition.top + itemPosition.top + menu.height(),
        }).show().attr('type', item.attr('type'));
    },
    initTabs() {
        let me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'design',
            args: {
                model: me.data.model,
            },
            onsuccess(r) {
                me.data.module = r.data.studio;
                me.data.modelId = r.data.id;
                me.data.views = r.data.views;
                let type = [];
                for (let view of r.data.views) {
                    type.push(`[type=${view}]`);
                }
                me.dom.find(type.join(",")).removeClass('d-none');
                me.loadResource(function (content) {
                    me.dom.find('.studio-content').prepend(content);
                });
                me.initBaseForm(r.data.id);
                let open = me.urlHash.tab || 'form';
                let tab = me.dom.find(`[type="${open}"]`);
                if (!tab.hasClass('d-none')) {
                    tab.click();
                }
            }
        });
    },
    createTab(type) {
        let me = this;
        let html = $(`<div tab="${type}" class="tab-panel active">${type}</div>`);
        me.dom.find('.studio-content').append(html);
        let opt = {dom: html, studio: me, type};
        if (type == 'form') {
            me.editors[type] = jmaa.create("EditForm", opt);
        } else if (type == 'grid') {
            me.editors[type] = jmaa.create("EditGrid", opt);
        } else if (type == 'search') {
            me.editors[type] = jmaa.create("EditSearch", opt);
        } else if (type == 'card') {
            me.editors[type] = jmaa.create("EditCard", opt);
        } else if (type == 'mobile') {
            me.editors[type] = jmaa.create("EditMobile", opt);
        } else if (type == 'lookup') {
            me.editors[type] = jmaa.create("EditLookup", opt);
        } else {
            me.editors[type] = jmaa.create("EditSource", opt);
        }
    },
    editSource(type) {
        let me = this;
        me.dom.find('.studio-content .tab-panel.active').removeClass('active');
        let dom = me.dom.find('[tab=source-code]').addClass('active');
        let opt = {
            dom,
            studio: me,
            type,
            canCreate: false,
            goBack() {
                me.dom.find('.studio-content .tab-panel.active').removeClass('active');
                me.dom.find(`.studio-content [tab=${type}]`).addClass('active');
                me.editors[type].loadViewData();
            }
        };
        jmaa.create("EditSource", opt);
    },
    initBaseForm(id) {
        let me = this;
        me.urlHash.id = id;
        window.location.hash = $.param(me.urlHash);
        me.loadView("ir.model", "form", "studio").then(v => {
            me.form = me.dom.find('.studio-content [tab=info]').JForm({
                model: v.model,
                module: v.module,
                fields: v.fields,
                arch: v.views.form.arch,
                studio: me,
                onEvent: function () {
                },
                view: {
                    auths: ["update", "read"],
                    save(e, target) {
                        target.save();
                    },
                    reload(e, target) {
                        target.reload();
                    },
                    export(e, target) {
                        me.export(e, target);
                    },
                    delete(e, target) {
                        target.delete();
                    },
                    edit(e, target) {
                        target.edit();
                    }
                },
                on: {
                    async save(e, form) {
                        const data = form.getSubmitData();
                        await me.rpc(me.model, 'updateModel', {
                            modelId: id,
                            values: data,
                        });
                        me.form.load();
                        jmaa.msg.show('操作成功'.t());
                    },
                },
                ajax(form, callback) {
                    me.rpc(me.model, 'readModel', {
                        modelId: id,
                        fields: form.getFields(),
                    }, {
                        usePresent: form.getUsePresent(),
                    }).then(data => {
                        me.dom.find('.studio-head .title').html(data[0].name);
                        document.title = '设计 - {0}'.t().formatArgs(data[0].name);
                        callback({data: data[0]});
                    });
                },
            });
            me.form.load();
        });
    },
    loadResource(callback) {
        let me = this;
        jmaa.rpc({
            model: me.model,
            method: 'searchView',
            args: {
                criteria: [['model', '=', me.data.model], ['type', '=', "resource"], ['active', '=', true]],
                fields: ["mode", "priority", "arch", "module"],
                limit: 1000,
                order: 'mode desc,priority'
            },
            context: {
                usePresent: true
            },
            onsuccess(r) {
                for (let row of r.data.values) {
                    let arch = jmaa.utils.parseXML(row.arch);
                    for (let res of arch.find('link,script')) {
                        let isLink = res.tagName == 'LINK';
                        let url = isLink ? res.href : res.src;
                        if (url.includes("jquery-ui") || url.includes('codemirror')) {
                            continue;
                        }
                        const xhr = new XMLHttpRequest();
                        xhr.open('GET', url, false); // 第三个参数为false表示同步
                        try {
                            xhr.send();
                            if (xhr.status === 200) {
                                callback(isLink ? `<style>${xhr.responseText}</style>` : `<script>${xhr.responseText}</script>`);
                            } else {
                                throw new Error(`资源文件加载失败：${xhr.status}`);
                            }
                        } catch (error) {
                            console.error('XHR同步加载失败：', error);
                        }
                    }
                }
            }
        });
    },
});
jmaa.editor("studio-field-editor", {
    extends: "editors.one2many",
    searchData(grid, callback) {
        let me = this;
        let data = me.owner.getRaw();
        jmaa.rpc({
            model: 'dev.studio',
            module: me.module,
            method: 'searchField',
            args: {
                criteria: [['model_id.model', '=', data.model]],
                offset: grid.pager.getOffset(),
                limit: grid.pager.getLimit(),
                fields: grid.getFields(),
                order: grid.getSort(),
            },
            context: {
                usePresent: grid.getUsePresent(),
            },
            onsuccess: function (r) {
                me.renderData(r.data, callback);
            }
        });
    },
    readData(grid, id, callback) {
        let me = this;
        jmaa.rpc({
            model: 'dev.studio',
            module: me.module,
            method: 'searchField',
            args: {
                criteria: [['id', '=', id]],
                offset: me.pager.getOffset(),
                limit: me.pager.getLimit(),
                fields: grid.editForm.getFields(),
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                let data = r.data.values[0];
                callback({
                    data: data,
                });
            },
        });
    },
    deleteData() {
        let me = this;
        let data = me.owner.getRaw();
        jmaa.rpc({
            model: 'dev.studio',
            module: me.module,
            method: 'deleteField',
            args: {
                fieldIds: me.grid.getSelected(),
            },
            onsuccess: function (r) {
                delete me.owner.studio.data.modelFields;
                me.load();
            }
        });
    },
    submitEdit(dirty, data) {
        let me = this;
        data = data || dirty;
        if (data.id) {
            jmaa.rpc({
                model: 'dev.studio',
                module: me.module,
                method: 'updateField',
                args: {
                    viewId: data.id,
                    values: dirty,
                },
                onsuccess: function (r) {
                    me.load();
                }
            });
        }
    },
});
jmaa.editor("studio-view-editor", {
    extends: "editors.one2many",
    searchData(grid, callback) {
        let me = this;
        let data = me.owner.getRaw();
        jmaa.rpc({
            model: 'dev.studio',
            module: me.module,
            method: 'searchView',
            args: {
                criteria: [['model', '=', data.model]],
                offset: grid.pager.getOffset(),
                limit: grid.pager.getLimit(),
                fields: grid.getFields(),
                order: grid.getSort(),
            },
            context: {
                usePresent: grid.getUsePresent(),
            },
            onsuccess: function (r) {
                me.renderData(r.data, callback);
            }
        });
    },
    readData(grid, id, callback) {
        let me = this;
        jmaa.rpc({
            model: 'dev.studio',
            module: me.module,
            method: 'searchView',
            args: {
                criteria: [['id', '=', id]],
                offset: me.pager.getOffset(),
                limit: me.pager.getLimit(),
                fields: grid.editForm.getFields(),
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                let data = r.data.values[0];
                callback({
                    data: data,
                });
            },
        });
    },
    deleteData() {
        let me = this;
        jmaa.rpc({
            model: 'dev.studio',
            module: me.module,
            method: 'deleteView',
            args: {
                viewIds: me.grid.getSelected(),
            },
            onsuccess: function (r) {
                me.load();
            }
        });
    },
    submitEdit(dirty, data) {
        let me = this;
        data = data || dirty;
        if (data.id) {
            jmaa.rpc({
                model: 'dev.studio',
                module: me.module,
                method: 'updateView',
                args: {
                    viewId: data.id,
                    values: dirty,
                },
                onsuccess: function (r) {
                    me.load();
                }
            });
        }
    },
});
