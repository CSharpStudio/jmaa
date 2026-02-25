/**
 * web视图
 */
jmaa.component('JView', {
    /**
     * 图标，用于视图切换
     */
    icon: {grid: 'fa-list-ul', card: 'fa-th-large'},
    dataView: ['grid', 'card'],
    /**
     * 获取视图模板
     * @returns
     */
    getViewTpl() {
        const me = this;
        let viewContents = '';
        const getViewType = function () {
            let viewSwitch = '';
            if (me.dataViews.length > 1) {
                $.each(me.dataViews, function (i, viewType) {
                    const active = me.urlHash.view ? me.urlHash.view === viewType : i === 0;
                    viewSwitch += `<label part="radio-view-type" data="${viewType}" class="btn btn-sm btn-secondary${active ? ' active' : ''}">
                                        <input type="radio" name="options" autocomplete="off"${active ? ' checked="checked"' : ''}/>
                                        <i class="fa ${me.icon[viewType]}"></i>
                                    </label>`;
                });
            }
            if (viewSwitch) {
                viewSwitch = `<div class="btn-group btn-group-toggle ml-2" data-toggle="buttons">${viewSwitch}</div>`;
            }
            return viewSwitch;
        };
        $.each(me.dataViews, (i, v) => viewContents += `<div part="${v}"></div>`);
        return `<div class="data-panel view-panel">
                    <div class="header">
                        <div class="content-header p-2">
                            <div class="container-fluid">
                                <div part="search"></div>
                            </div>
                            <div class="btn-row">
                                <div part="toolbar" class="toolbar"></div>
                                <div class="btn-toolbar toolbar-right">
                                    <div part="pager" class="ml-2"></div>
                                    <div part="view-type">${getViewType()}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="content">
                        <aside part="search-panel" class="left-aside border-right" style="display:none"></aside>
                        <div class="view-content container-fluid">${viewContents}</div>
                    </div>
                </div>`;
    },
    /**
     * 获取表单模板
     * @returns {string}
     */
    getFormTpl() {
        return `<div class="form-panel view-panel" part="form"></div>`;
    },
    /**
     * 获取控件模板
     * @returns {string}
     */
    getTpl() {
        const me = this;
        let tpl = '';
        if (me.dataViews.length > 0) {
            tpl += me.getViewTpl();
        }
        if (me.views.form) {
            tpl += me.getFormTpl();
        }
        if (me.views.custom) {
            tpl += '<div class="custom-panel view-panel"></div>';
        }
        return tpl;
    },
    /**
     * 构建实例
     * @param opt
     */
    __init__(opt) {
        const me = this;
        window.view = me;
        let theme = 'theme-jmaa';//localStorage.getItem('user_theme');
        if (theme) {
            $('html').removeClass().addClass(theme);
        }
        let user = JSON.parse(localStorage.getItem("user_info") || '{}');
        let company = JSON.parse(localStorage.getItem("company_info") || '{}');
        window.env = {user, company};
        if (window.history && window.history.pushState) {
            $(window).on('popstate', function () {
                if (window.view) {
                    window.view.urlHash = jmaa.web.getParams(window.location.hash.substring(1));
                    window.view.changeView();
                }
            });
        }
        $('html,body').css('height', '100%');
        if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(top.window.navigator.userAgent)) {
            $('body').addClass('mobile');
        }
        $(document).on('click', function () {
            top.window.$('.dropdown-menu').removeClass('show');
        });
        jmaa.utils.apply(true, me, opt);
        me.render();
        me.dom.on('click', '[t-click]', function (e) {
            if (!me.design) {
                const btn = $(this);
                const click = btn.attr('t-click');
                const gap = eval(btn.attr('gap') || 500);
                btn.attr('disabled', true);
                btn.attr('clicking', "1");
                const fn = new Function('return this.' + click).call(me, e);
                if (fn instanceof Function) {
                    fn.call(me, e);
                }
                setTimeout(function () {
                    if (btn.attr("clicking") === "1") {
                        btn.attr('disabled', false);
                        btn.removeAttr('clicking');
                    }
                }, gap);
            }
        });
        let i = me.init();
        if (i instanceof Promise) {
            i.then(() => me.dom.triggerHandler('init', [me]));
        } else {
            me.dom.triggerHandler('init', [me]);
        }
        window.addEventListener("beforeunload", function (event) {
            if (me.isDirty()) {
                event.preventDefault();
                event.returnValue = "确定要离开此页面吗？";
            }
        });
    },
    isDirty() {
        let me = this;
        let activeElement = document.activeElement;
        if (activeElement) {
            activeElement.blur(); // 触发失焦事件
        }
        return me.curView.dirty;
    },
    /**
     * 加载数据
     */
    load() {
        const me = this;
        if (me.curView && me.curView.load) {
            me.curView.load();
        }
    },
    /**
     * 视图切换
     * @param
     */
    onViewChange() {
    },
    /**
     * 渲染查询控件
     */
    renderSearch() {
        const me = this;
        me.search = new JSearch({
            dom: me.dom.find('[part=search]'),
            model: me.model,
            module: me.module,
            arch: me.views.search.arch,
            fields: me.fields,
            view: me,
            vid: me.views.search.view_id,
            submitting(e, search) {
                me.resizeContent();
                me.pager.reset();
                me.load();
                me.curView.select && me.curView.select();
            },
        });
        me.resizeContent();
        $(window).on('resize', function () {
            me.resizeContent();
        });
    },
    resizeContent() {
        const me = this;
        const h = me.dom.find('.data-panel .header').height();
        me.dom.find('.data-panel .content').css('height', 'calc(100% - ' + h + 'px)');
    },
    /**
     * 渲染分页控件
     */
    renderPager() {
        const me = this;
        me.pager = new JPager({
            dom: me.dom.find('[part=pager]'),
            limitChange(e, pager) {
                if (me.curView) {
                    me.curView.limit = pager.limit;
                }
            },
            pageChange(e, pager) {
                me.load();
            },
            counting(e, pager) {
                jmaa.rpc({
                    model: me.model,
                    module: me.module,
                    method: 'count',
                    args: {
                        criteria: me.search.getCriteria(),
                    },
                    onsuccess(r) {
                        pager.update({
                            total: r.data,
                        });
                    },
                });
            },
        });
    },
    /**
     * 渲染表格控件
     */
    renderGrid() {
        const me = this;
        me.grid = me.dom.find('[part=grid]').JGrid({
            filterColumn: true,
            model: me.model,
            module: me.module,
            arch: me.views.grid.arch,
            fields: me.fields,
            search: me.search,
            pager: me.pager,
            view: me,
            vid: me.views.grid.view_id,
            customizable: true,
            ajax(grid, callback, data, settings) {
                me.searchData(grid, callback, data, settings);
            },
            loadEdit(grid, id, callback) {
                me.readData(grid.editForm, id).then(data => callback({data}));
            },
            on: {
                async save(e, grid, dirty, data) {
                    await me.saveData(grid, dirty);
                },
                async delete(e, grid) {
                    await me.deleteData(grid, grid.getSelected());
                    grid.select();
                },
                selected(e, grid, sel) {
                    let selected = grid.data.filter(d => sel.includes(d.id));
                    if (me.toolbar) {
                        me.toolbar.update(selected);
                    }
                },
                async rowDblClick(e, grid, id) {
                    await me.browseData(e, grid, id);
                }
            }
        });
    },
    /**
     * 渲染卡片控件
     */
    renderCard() {
        const me = this;
        me.card = me.dom.find('[part=card]').JCard({
            model: me.model,
            module: me.module,
            arch: me.views.card.arch,
            fields: me.fields,
            search: me.search,
            pager: me.pager,
            view: me,
            vid: me.views.card.view_id,
            on: {
                selected(e, card, sel) {
                    let selected = card.data.filter(d => sel.includes(d.id));
                    if (me.toolbar) {
                        me.toolbar.update(selected);
                    }
                },
                async delete(e, card) {
                    await me.deleteData(card, card.getSelected());
                    card.select();
                },
                async cardDblClick(e, card, id) {
                    await me.browseData(e, card, id);
                }
            },
            ajax(card, callback) {
                me.searchData(card, callback);
            },
        });
        $('t').each(function () {
            let el = $(this);
            el.replaceWith(el.text().t());
        });
    },
    /**
     * 渲染表单控件
     */
    renderForm() {
        const me = this;
        me.form = me.dom.find('[part=form]').JForm({
            arch: me.views.form.arch,
            model: me.model,
            module: me.module,
            fields: me.fields,
            view: me,
            vid: me.views.form.view_id,
            on: {
                treeSelect(e, tree, selected) {
                    if (selected.length > 0) {
                        me.urlHash.id = selected[0].id;
                    } else {
                        delete me.urlHash.id;
                    }
                    me.changeView();
                    tree.form.ajax(tree.form, function (r) {
                        tree.form.setData(r.data);
                        tree.form.clean();
                        tree.form.dom.triggerHandler('load', [tree.form]);
                    });
                    if (me.toolbar) {
                        me.toolbar.update(selected);
                    }
                },
                selected(e, form) {
                    if (me.toolbar) {
                        const data = form.getRawData();
                        me.toolbar.update([data]);
                    }
                },
                async delete(e, form, id) {
                    delete me.urlHash.id;
                    await me.deleteData(form, [id]);
                    window.location.hash = $.param(me.urlHash);
                },
                async save(e, form) {
                    const data = form.getSubmitData();
                    let saved = await me.saveData(form, data);
                    if (!me.urlHash.id) {
                        me.urlHash.id = saved;
                        window.location.hash = $.param(me.urlHash);
                    }
                    me.load();
                },
            },
            ajax(form, callback) {
                if (me.urlHash.id) {
                    me.readData(form, me.urlHash.id).then(data => callback({data}));
                } else {
                    let data = {}
                    if (me.urlHash.present) {
                        data[me.present[0]] = me.urlHash.present;
                    }
                    me.form.create(data);
                }
            },
        });
        $('t').each(function () {
            let el = $(this);
            el.replaceWith(el.text().t());
        });
        if (!me.backView) {
            me.backView = me.urlHash.back;
        }
        if (me.backView) {
            let tb = me.form.dom.find('.form-header .toolbar');
            if (tb.length == 0) {
                me.form.dom.find('.form-header').append(`<div part="form-toolbar" class="toolbar">
                    <div class="btn-group"><button part="form-close" class="btn btn-icon btn-flat" type="button" >${'返回'.t()}</button></div></div>`);
            } else {
                tb.prepend(`<div class="btn-group"><button part="form-close" class="btn btn-icon btn-flat" type="button" >${'返回'.t()}</button></div>`);
            }
            me.form.dom.find('[part=form-close]').on('click', function () {
                if (me.form.dirty) {
                    jmaa.msg.confirm({
                        title: '提示'.t(),
                        content: '数据未保存，是否继续？'.t(),
                        submit() {
                            delete me.urlHash.readonly;
                            me.changeView(me.backView);
                        }
                    })
                } else {
                    delete me.urlHash.readonly;
                    me.changeView(me.backView);
                }
            });
        }
    },
    /**
     * 渲染自定义控件
     */
    renderCustom() {
        const me = this;
        me.custom = new JCustom({
            dom: me.dom.find('.custom-panel'),
            model: me.model,
            module: me.module,
            arch: me.views.custom.arch,
            fields: me.fields,
            view: me,
        });
        $('t').each(function () {
            let el = $(this);
            el.replaceWith(el.text().t());
        });
    },
    /**
     * 渲染控件
     */
    render() {
        const me = this;
        me.urlHash = jmaa.web.getParams(window.location.hash.substring(1));
        me.dataViews = me.urlHash.views.split(',');
        me.dataViews.remove(v => !me.dataView.includes(v));
        me.viewkey = me.urlHash.key;
        me.dom
            .html(me.getTpl())
            .find('[part=radio-view-type]')
            .on('click', function (i) {
                const viewType = $(this).attr('data');
                if (viewType != me.viewType) {
                    // click触发两次
                    me.changeView(viewType);
                }
            });
        if (me.dataViews.length > 0) {
            if (!me.search) {
                me.renderSearch();
            }
            if (!me.pager) {
                me.renderPager();
            }
        }
        me.changeView();
        me.dom.trigger('render', [me]);
    },
    /**
     * 切换视图
     * @param viewType
     */
    changeView(viewType) {
        const me = this;
        let m = viewType || me.urlHash.view;
        if (!m && me.urlHash.views) {
            m = me.urlHash.views.split(',')[0];
        }
        const changed = me.viewType !== m;
        if (changed) {
            if (m == 'grid') {
                me.showGridView();
            } else if (m == 'card') {
                me.showCardView();
            } else if (m == 'form') {
                me.showForm();
            } else if (m == 'custom') {
                me.showCustom();
            }
            me.viewType = m;
            me.urlHash.view = me.viewType;
        }
        me.updateHash();
        me.resizeContent();
        return changed;
    },
    /**
     * 更新url地址#
     */
    updateHash() {
        const me = this;
        window.location.hash = $.param(me.urlHash);
        if (!me.urlHash.top) {
            const p = jmaa.web.getParams(top.window.location.hash.substring(1));
            if (p.u) {
                p.u = window.location.pathname + '#' + $.param($.extend($.param(unescape(p.u)), me.urlHash));
            }
            top.window.location.hash = $.param(p);
        }
    },
    /**
     * 显示指定视图
     * @param name
     */
    showDataView(name) {
        const me = this;
        me.dom.find('.view-panel').hide();
        me.dom.find('.data-panel').show();
        $.each(me.dataViews, function (i, v) {
            me.dom.find('[part=' + v + ']').hide();
        });
        me.dom.find('[part=' + name + ']').show();
        me.backView = name;
        me.urlHash.back = name;
    },
    /**
     * 显示表格视图
     */
    showGridView() {
        const me = this;
        if (!me.grid) {
            me.renderGrid();
        } else {
            me.pager.limit = me.grid.limit;
            me.grid.load();
        }
        me.toolbar = new JToolbar({
            dom: me.dom.find('[part=toolbar]'),
            arch: me.grid.tbarArch,
            auths: me.auths,
            defaultButtons: 'query|create|edit|delete|export|import',
            target: me.grid,
            view: me,
        });
        me.curView = me.grid;
        me.showDataView('grid');
        me.resizeContent();
        me.onViewChange();
    },
    /**
     * 显示卡片视图
     */
    showCardView() {
        const me = this;
        if (!me.card) {
            me.renderCard();
        } else {
            me.pager.limit = me.card.limit;
            me.card.load();
        }
        me.toolbar = new JToolbar({
            dom: me.dom.find('[part=toolbar]'),
            arch: me.card.tbarArch,
            auths: me.auths,
            defaultButtons: 'query|create|edit|delete|export',
            target: me.card,
            view: me,
        });
        me.curView = me.card;
        me.showDataView('card');
        me.resizeContent();
        me.onViewChange();
    },
    /**
     * 显示表单视图
     */
    showForm() {
        const me = this;
        if (!me.form) {
            me.renderForm();
        }
        me.toolbar = me.form.toolbar;
        me.curView = me.form;
        me.dom.find('.view-panel').hide();
        me.dom.find('.form-panel').show();
        me.form.load();
        me.onViewChange();
    },
    /**
     * 显示自定义视图
     */
    showCustom() {
        const me = this;
        if (!me.custom) {
            me.renderCustom();
        }
        me.toolbar = null;
        me.curView = me.custom;
        me.dom.find('.view-panel').hide();
        me.dom.find('.custom-panel').show();
        me.custom.load && me.custom.load();
        me.onViewChange();
    },
    /**
     * 获取选中数据
     * @returns {*}
     */
    getSelected() {
        return this.curView.getSelected();
    },
    /**
     * 显示加载中，请稍等
     * @param busy
     */
    busy(busy) {
        if (busy) {
            jmaa.mask('加载中,请稍等'.t());
        } else {
            jmaa.mask();
        }
    },
    /**
     * 浏览或者编辑数据
     * @param target
     * @param id
     * @returns {Promise<void>}
     */
    async browseData(e, target, id) {
        let me = this;
        const btn = me.toolbar.dom.find("[name='edit']");
        if (btn.length > 0) {
            btn.click();
        } else {
            await me.browse(e, target);
        }
    },
    /**
     * 读取指定id的数据
     * @param target 目标控件
     * @param id id
     * @param fields 要读取的字段
     * @param callback 回调函数
     */
    async readData(target, id) {
        const me = this;
        if (id) {
            return (await me.rpc(me.model, 'read', {
                ids: [id],
                fields: target.getFields(),
            }, {
                usePresent: target.getUsePresent(),
            }))[0];
        } else {
            return {};
        }
    },
    /**
     * 查询数据
     * @param target 目标控件
     * @param callback 回调函数
     */
    searchData(target, callback) {
        const me = this;
        jmaa.rpc({
            model: me.model,
            module: me.module,
            method: 'search',
            args: {
                criteria: me.search.getCriteria(),
                nextTest: true,
                offset: target.pager.getOffset(),
                limit: target.pager.getLimit(),
                fields: target.getFields(),
                order: target.getSort(),
            },
            context: {
                usePresent: target.getUsePresent(),
            },
            onsuccess(r) {
                if (r.data.values.length > 0) {
                    target.pager.update(r.data);
                } else {
                    target.pager.noData();
                }
                callback({
                    data: r.data.values,
                });
            },
        });
    },
    /**
     * 保存数据
     * @param target 目标控件
     * @param id id
     * @param data 数据
     */
    async saveData(target, data) {
        const me = this;
        let id = data.id;
        delete data.id;
        let result;
        if (id && !id.startsWith('new')) {
            result = await me.rpc(me.model, 'update', {
                ids: [id],
                values: data,
            });
        } else {
            result = await me.rpc(me.model, 'create', data);
        }
        jmaa.msg.show('操作成功'.t());
        return result;
    },
    /**
     * 删除数据
     * @param target 目标控件
     * @param ids 要删除的id
     */
    async deleteData(target, ids) {
        const me = this;
        await me.rpc(me.model, 'delete', {ids});
        jmaa.msg.show('操作成功'.t());
        me.load();
    },
    /**
     * 保存按钮事件
     * @param e
     * @param target 目标控件
     */
    async save(e, target) {
        await target.save();
    },
    /**
     * 删除按钮事件
     * @param e
     * @param target 目标控件
     */
    async delete(e, target) {
        await target.delete();
    },
    /**
     * 编辑按钮事件
     * @param e
     * @param target 目标控件
     */
    async edit(e, target) {
        const me = this;
        if (target.editable) {
            await target.edit();
        } else {
            me.urlHash.id = me.getSelected()[0];
            me.urlHash.readonly = false;

            if (!me.changeView('form')) {
                me.form.load();
            }
        }
    },
    /**
     * 浏览，不编辑
     * @param e
     * @param target
     * @returns {Promise<void>}
     */
    async browse(e, target) {
        const me = this;
        if (target.editable) {
            const form = await target.edit();
            for (const editor in form.editors) {
                form.editors[editor].setReadonly(true);
            }
        } else {
            if (me.views.form) {
                me.urlHash.id = me.getSelected()[0];
                me.urlHash.readonly = true;
                if (!me.changeView('form')) {
                    me.form.load();
                }
            }
        }
    },
    query(e, target) {
        let me = this;
        target.search.confirm();
    },
    reload(e, target) {
        if (target.reload) {
            target.reload();
        } else {
            target.load();
        }
    },
    /**
     * 创建按钮事件
     * @param e
     * @param target 目标控件
     * @param view
     */
    async create(e, target) {
        const me = this;
        if (!target) {
            //冒烟测试时target未初始化问题
            return;
        }
        if (target.editable) {
            await target.create({});
        } else {
            delete me.urlHash.id;
            me.urlHash.readonly = false;
            if (!me.changeView('form')) {
                me.form.load();
            }
        }
    },
    /**
     * 创建子按钮事件
     * @param e
     * @param target 目标控件
     */
    async createChild(e, target) {
        const me = this;
        me.changeView('form');
        me.form.createChild();
    },
    /**
     * 复制按钮事件
     * @param e
     * @param target 目标控件
     */
    async copy(e, target) {
        const me = this;
        const ids = me.getSelected();
        delete me.urlHash.id;
        me.changeView('form');
        let data = await me.rpc(me.model, 'copy', {
            ids,
            defaultValues: {},
        });
        me.urlHash.id = data[0];
        jmaa.msg.show('操作成功'.t());
        window.location.hash = $.param(me.urlHash);
        me.form.load();
    },
    /**
     *
     * @param e
     * @param target 目标控件
     */
    import(e, target) {
        let me = this;
        jmaa.create('JImportXls', {
            model: target.model,
            fields: target.fields,
            target,
            callback() {
                me.load();
            },
        });
    },
    exportDataTable(table, filename) {
        $.fn.DataTable.ext.buttons.excelHtml5.action.call(
            table.buttons(),
            null,
            table,
            {},
            {
                header: true,
                footer: true,
                extension: '.xlsx',
                filename,
                exportOptions: {
                    format: {
                        body: function (html, row, column, node) {
                            let value = String($.fn.DataTable.Buttons.stripData(html, {stripHtml: true})).replace(/^\s+/, '');
                            if (/^\d/.test(value) && String(html).includes('char')) {
                                return "\t" + value;
                            }
                            return value;
                        }
                    },
                    columns: function (idx, data, node) {
                        return !$(node).find("input.all-check-select").length;
                    }
                },
            },
        );
    },
    export(e, target) {
        let me = this;
        if (target.table) {
            let filename = jmaa.web.getParams(top.window.location.hash.substring(1)).t || '*';
            if (target.owner && target.owner.field) {
                filename += "-" + target.owner.field.label.t();
            }
            me.exportDataTable(target.table, filename);
        }
    },
    exportAll(e, target) {
        let me = this;
        if (target.table) {
            let filename = jmaa.web.getParams(top.window.location.hash.substring(1)).t || '*';
            if (target.owner && target.owner.field) {
                filename += "-" + target.owner.field.label.t();
            }
            $('<div></div>').JGrid({
                arch: target.arch,
                fields: target.fields,
                filterColumn: true,
                model: target.model,
                module: target.module,
                search: target.search,
                pager: {
                    getLimit() {
                        return 100000;
                    },
                    getOffset() {
                        return 0;
                    },
                    update() {
                    },
                    noData() {
                    }
                },
                exporting: true,
                view: target.view,
                vid: target.vid,
                customizable: target.customizable,
                ajax(grid, callback, data, settings) {
                    target.ajax(grid, function (data) {
                        callback(data);
                        me.exportDataTable(grid.table, filename);
                    }, data, settings);
                }
            });
        }
    },
    /**
     * 调用rpc服务
     * @param svc
     * @param ids
     */
    call(svc, target) {
        const me = this;
        let ids = target.getSelected();
        me.busy(true);
        jmaa.rpc({
            model: target.model || me.model,
            module: target.module || me.module,
            method: svc,
            args: {
                ids,
            },
            onerror(e) {
                me.busy(false);
                jmaa.msg.error(e);
            },
            onsuccess(r) {
                me.busy(false);
                const d = r.data || {};
                if (d.message) {
                    jmaa.msg.show(d.message);
                }
                if (d.action === 'js') {
                    eval(d.script);
                } else if (d.action === 'reload') {
                    me.load();
                } else if (d.action === 'service') {
                    // TODO
                } else if (d.action === 'dialog') {
                    // TODO
                } else if (d.action === 'view') {
                    // TODO
                }
            },
        });
    },
    plusReady(callback) {
        if (top.window.plus) {
            callback();
        } else {
            document.addEventListener('plusready', callback);
        }
    },
    /**
     * 异步请求rpc方法
     * @param model
     * @param method
     * @param args
     * @returns {Promise<unknown>}
     */
    rpc(model, method, args, context) {
        let me = this;
        let timeout = setTimeout(() => {
            me.busy(true);
        }, 800);
        return new Promise((resolve, reject) => {
            jmaa.rpc({
                module: me.module,
                model,
                method,
                args,
                context,
                onsuccess: function (r) {
                    resolve(r.data);
                },
                onerror: (error) => {
                    jmaa.msg.error(error);
                    reject(error);
                }
            });
        }).finally(() => {
            clearTimeout(timeout);
            me.busy(false)
        });
    },
    loadView(model, type, key) {
        return new Promise((resolve, reject) => {
            jmaa.rpc({
                model: "ir.ui.view",
                module: "base",
                method: "loadView",
                args: {
                    model: model,
                    type: type,
                    key: key
                },
                onsuccess: function (r) {
                    resolve(r.data);
                },
                onerror: (error) => {
                    reject(error);
                    console.error(error);
                }
            });
        })
    },
});

/**
 * 定义扩展的JView视图
 *
 * @example
 * jmaa.view({
 * })
 *
 * @param {Object} define
 */
jmaa.view = function (define) {
    if (typeof define === "function") {
        define = define();
    }
    define.extends = define.extends || 'JView';
    jmaa.define('JView', define);
};
