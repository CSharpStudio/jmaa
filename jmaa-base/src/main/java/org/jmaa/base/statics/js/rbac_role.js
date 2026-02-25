//@ sourceURL=rbac_role.js
jmaa.view({
    globalId: 0,
    notFoundText: '找不到数据'.t(),
    arrowSign: '->',
    minLength: 1,
    highlightName: true,
    highlightPath: false,
    menuList: [],
    maxResults: 100,
    getId: function () {
        return this.globalId++;
    },
    _setAdmin(isAdmin) {
        let me = this;
        me.rpc('rbac.role', 'setAdmin', {ids: me.getSelected(), isAdmin})
            .then(() => {
                jmaa.msg.show('操作成功'.t());
                me.load();
            });
    },
    setAdmin() {
        this._setAdmin(true);
    },
    revokeAdmin() {
        this._setAdmin(false);
    },
    editPermission() {
        let me = this;
        let roleId = me.getSelected()[0];
        if (!roleId) return;
        jmaa.showDialog({
            title: '角色'.t() + '<span role="role-name"></span>' + '权限'.t(),
            init: function (dialog) {
                me.rpc('rbac.role', 'getPresent', {ids: [roleId]})
                    .then(data => {
                        dialog.dom.find('[role=role-name]').html('[' + data[0][1] + ']');
                    });
                me.loadPermission(function (data) {
                    me.renderPermission(dialog.body, data, roleId);
                });
                dialog.dom.on('mousedown', function (e) {
                    if ($(e.target).closest('.find-menu').length == 0) {
                        me.closeMenuResult(dialog.body);
                    }
                });
            },
            submit: function (dialog) {
                dialog.busy(true);
                let permission = [];
                let menus = [];
                dialog.body.find('.check-per').each(function () {
                    let input = $(this);
                    if (input.is(':checked')) {
                        permission.push(input.attr('data-id'));
                    }
                });
                dialog.body.find('.check-menu').each(function () {
                    let input = $(this);
                    if (input.is(':checked')) {
                        menus.push(input.attr('data-id'));
                    }
                });
                jmaa.rpc({
                    model: 'rbac.role',
                    method: 'savePermission',
                    args: {
                        ids: me.getSelected(),
                        permissions: permission, menus
                    },
                    dialog,
                    onsuccess(r) {
                        jmaa.msg.show('操作成功'.t());
                    }
                });
            },
        });
    },
    // 获取pda分组下对应的名称
    getGroupsTargetModels(info, searchName) {
        let list = info.models.filter((v) => v.menu.startsWith(searchName));
        list = list.map((item) => {
            item.parentName = info.name;
            return item;
        });
        return list;
    },
    searchMenu(searchName) {
        let me = this;
        let result = [];
        this.menuList.forEach((item) => {
            let modelsList = 'models' in item && Array.isArray(item.models) ? item.models : [];
            let targetModel = modelsList.filter((v) => v.menu.indexOf(searchName) >= 0);
            let targetMenus = [];
            let childMenus = [];
            let targetGroups = [];
            let groupsChildMenus = [];
            if ('menus' in item) {
                for (let key in item.menus) {
                    if (key !== 'sub' && 'sub' in item.menus[key]) {
                        childMenus.push(item.menus[key]);
                        item.menus[key].name.indexOf(searchName) >= 0 && targetMenus.push(item.menus[key]);
                    }
                }
            }
            if ('groups' in item) {
                item.groups.forEach((valByGroup) => {
                    valByGroup.name.indexOf(searchName) >= 0 && targetGroups.push(valByGroup);
                    if ('models' in valByGroup && Array.isArray(valByGroup.models)) groupsChildMenus = [...groupsChildMenus, ...me.getGroupsTargetModels(valByGroup, searchName)];
                });
            }
            if (item.app.indexOf(searchName) >= 0) {
                modelsList.forEach((valMax) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: valMax.menu,
                    });
                });
                childMenus.forEach((valPlus) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: valPlus.name,
                    });
                });
                targetGroups.forEach((vals) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: vals.name,
                    });
                });
                groupsChildMenus.forEach((valx) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: valx.parentName,
                        subChild: valx.menu,
                    });
                });
            } else if (targetMenus.length > 0) {
                targetMenus.forEach((val) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: val.name,
                    });
                });
            } else if (targetModel.length > 0) {
                targetModel.forEach((valPro) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: valPro.menu,
                    });
                });
            } else if (targetGroups.length > 0) {
                targetGroups.forEach((vals) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: vals.name,
                    });
                });
            } else if (groupsChildMenus.length > 0) {
                groupsChildMenus.forEach((valx) => {
                    result.push({
                        app: item.app,
                        appId: item.appId,
                        child: valx.parentName,
                        subChild: valx.menu,
                    });
                });
            }
        });
        return result;
    },
    findMenu(dom) {
        let me = this;
        let searchValue = dom.find('#role-input').val();
        if (searchValue.length < this.minLength) {
            dom.find('.list-group').empty();
            me._addNotFound(dom);
            me.closeMenuResult(dom);
            return;
        }
        let searchResults = me.searchMenu(searchValue);
        let endResults = $(searchResults.slice(0, me.maxResults));
        dom.find('.list-group').empty();
        if (endResults.length === 0) {
            me._addNotFound(dom);
        } else {
            endResults.each(function (i, result) {
                dom.find('.list-group').append(me._renderItem([result.app, result.child, result.subChild || null].filter(Boolean), result));
            });
        }
        me.openMenuResult(dom);
    },
    _addNotFound(dom) {
        dom.find('.list-group').append(this._renderItem([this.notFoundText]));
    },
    _renderItem(path, val = {}) {
        let me = this;
        let name = '';
        if (path.length >= 2) {
            name = path.join(' ' + me.arrowSign + ' ');
        } else {
            name = path.join(',');
        }
        let groupItemElement = $('<div/>', {
            class: 'list-group-item',
        })
            .attr('data-app', val.app)
            .attr('data-child', val.child)
            .attr('data-appId', val.appId)
            .attr('data-subChild', val.subChild || null);
        let searchPathElement = $('<div/>', {
            class: 'search-path',
        })
            .html(name)
            .attr('data-app', val.app)
            .attr('data-child', val.child)
            .attr('data-appId', val.appId)
            .attr('data-subChild', val.subChild || null);

        return groupItemElement.append(searchPathElement);
    },
    openMenuResult(dom) {
        dom.find('.find-menu').addClass('search-open');
    },
    closeMenuResult(dom) {
        dom.find('.find-menu').removeClass('search-open');
    },
    renderPermission(dom, data, roleId) {
        let me = this;
        me.menuList = data;
        let addModel = function (m, root) {
            let tplModel = [`<div class="per-menu" data-menu="${m.menu}">
                                <i class="fa fa-desktop"></i>
                                ${m.menu}
                                <span class="per-code" style="display:none">(${m.model})</span>
                                ${root ? `<div class="btn-group ml-2">
                                    <a class="btn btn-default btn-sm btn-sel-all">${'全选'.t()}</a>
                                    <a class="btn btn-default btn-sm btn-unsel-all">${'全不选'.t()}</a>
                                </div>` : ''}
                            </div>`];
            let services = [];
            $.each(m.services, function () {
                let p = this;
                if (root || p.auth != 'read') {
                    let id = 'per_' + me.getId();
                    let checked = p.role_ids.indexOf(roleId) > -1 ? ' checked="checked"' : '';
                    services.push(`<div class="form-check">
                                        <input${checked} data-id="${p.id}" type="checkbox" class="check-per" id="${id}"/>
                                        <label for="${id}" class="form-check-label">
                                            ${p.name.t()}
                                            <span class="per-code" style="display:none">(${p.auth})</span>
                                        </label>
                                   </div>`);
                }
            });
            tplModel.push(`<div class="per-service row ml-5 mb-2">${services.join('')}</div>`);
            if (m.fields[0]) {
                let fields = [];
                $.each(m.fields, function () {
                    let p = this;
                    let id = 'per_' + me.getId();
                    let checked = p.role_ids.indexOf(roleId) > -1 ? ' checked="checked"' : '';
                    fields.push(`<div class="form-check">
                                    <input${checked} data-id="${p.id}" type="checkbox" class="check-per" id="${id}"/>
                                    <label for="${id}" class="form-check-label">
                                        ${p.name.t()}
                                        <span class="per-code" style="display:none">
                                            (${p.auth})
                                        </span>
                                    </label>
                                </div>`);
                });
                tplModel.push(`<div class="per-field">
                                    <div class="per-menu">
                                        <i class="fa fa-tasks"></i>
                                        ${'字段权限'.t()}
                                    </div>
                                    <div class="row ml-5 mb-2">
                                        ${fields.join('')}
                                    </div>
                               </div>`);
            }
            if (m.related) {
                tplModel.push('<div class="per-related">');
                $.each(m.related, function () {
                    let html = addModel(this);
                    html && tplModel.push(html);
                });
                tplModel.push('</div>');
            }
            if (services.length || services.length || m.related) {
                return tplModel.join('');
            }
        };
        let addPermission = function (val, appId, index) {
            let tpl = [];
            $.each(val.models, function () {
                tpl.push(`<div class="module-permission">${addModel(this, true)}</div>`);
            });
            // 处理特殊菜单
            if (val.menus) {
                let getMenu = function (i) {
                    let m = val.menus[i];
                    let t = [];
                    if (m.role_ids) {
                        let id = 'menu_' + me.getId();
                        let checked = m.role_ids.indexOf(roleId) > -1 ? ' checked="checked"' : '';
                        t.push(`<input${checked} data-id="${i}" class="check-menu" type="checkbox" id="${id}"/>
                                <label for="${id}" class="form-check-label" data-menu="${m.name}">${m.name.t()}</label>`);
                    } else {
                        t.push(`<span data-menu="${m.name}"><i class="fa fa-link"></i> ${m.name.t()}</span>`);
                    }
                    if (m.sub) {
                        $.each(m.sub, function () {
                            t.push(getMenu(this));
                        });
                    }
                    return `<div class="ml-3 mt-1">${t.join('')}</div>`;
                };
                tpl.push('<div class="post"></div>');
                $.each(val.menus.sub, function () {
                    tpl.push(getMenu(this));
                });
            }
            // 处理pda权限
            if (val.groups) {
                let getGroups = function (group) {
                    let t = [];
                    if (group.role_ids) {
                        let id = 'menu_' + me.getId();
                        let checked = group.role_ids.indexOf(roleId) > -1 ? ' checked="checked"' : '';
                        t.push(`<input${checked} data-id="${group.id}" class="check-menu" type="checkbox" id="${id}"/>
                                <label for="${id}" class="form-check-label" data-menu="${group.name}">${group.name.t()}</label>`);
                    } else {
                        t.push(`<span data-menu="${group.name}"><i class="fa fa-link"></i> ${group.name.t()}</span>`);
                    }
                    if (group.models) {
                        $.each(group.models, function () {
                            let models = addModel(this, true);
                            t.push(`<div class="module-permission">${models}</div>`);
                        });
                    }
                    return `<div class="ml-3 mt-1">${t.join('')}</div>`;
                };
                $.each(val.groups, function () {
                    tpl.push('<div class="post"></div>');
                    tpl.push(getGroups(this));
                });
            }
            return `<div class="check-box ${+index === 0 ? 'check-box-show' : ''}" id="menu-${appId}">${tpl.join('')}</div>`;
        };
        let menu = [];
        let permission = [];
        $.each(data, function (i, v) {
            let a = this;
            let appId = 'app_' + me.getId();
            v.appId = appId;
            menu.push(`<div class="role-box-menu-con ${i === 0 ? 'role-box-menu-con-active' : ''}" id="${appId}" data-id="${appId}" data-app="${a.app}">
                           <div data-id="${appId}" data-app="${a.app}" class="w-100 role-box-menu-con-txt">${a.app}</div>
                       </div>`);
            permission.push(addPermission(a, appId, i));
        });
        let html = `<div class="m-3">
                        <input type="checkbox" id="showCode" class="mr-1 ml-3"/><label class="mb-0" for="showCode">显示编码</label>
                        <div class="form-inline float-right mr-3 find-menu">
                            <div data-widget="menu-search" class="input-group">
                                <input class="form-control form-control-sidebar" autocomplete="off" type="search" aria-label="Search" id="role-input" placeholder="请输入">
                                <div class="input-suffix">
                                    <button class="btn btn-default btn-search">
                                        <i class="fa fa-search"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="search-results">
                                <div class="list-group">
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="role-box">
                        <div class="role-box-menu">
                            ${menu.join('')}
                        </div>
                        <div class="role-box-info">${permission.join('')}</div>
                    </div>`;
        dom.html(html).on('change', '#showCode', function () {
            let me = $(this);
            if (me.is(':checked')) {
                dom.find('.per-code').show();
            } else {
                dom.find('.per-code').hide();
            }
        }).on('click', '.btn-sel-all', function () {
            let btn = $(this);
            btn.parents('.module-permission').find('[type=checkbox]').prop('checked', true);
        }).on('click', '.btn-unsel-all', function () {
            let btn = $(this);
            btn.parents('.module-permission').find('[type=checkbox]').prop('checked', false);
        }).on('click', '.role-box-menu-con-txt', function (e) {
            dom.find('.role-box-menu-con').removeClass('role-box-menu-con-active');
            dom.find(`[id=${e.target.dataset.id}]`).addClass('role-box-menu-con-active');
            dom.find('.check-box').removeClass('check-box-show');
            dom.find(`[id=menu-${e.target.dataset.id}]`).addClass('check-box-show');
        }).on('keyup', '#role-input', function (event) {
            setTimeout(function () {
                me.findMenu(dom);
            }, 200);
        }).on('click', '.btn-search', function () {
            me.findMenu(dom);
        }).on('click', '.list-group-item', function (e) {
            let {appid, child, subchild} = e.target.dataset;
            dom.find('.hilite').removeClass('hilite');
            let scrollIntoView = function (selector) {
                let el = dom.find(selector);
                if (el.length > 0) {
                    el[0].scrollIntoView(true);
                }
                return el;
            }
            if (appid) {
                dom.find('.role-box-menu-con').removeClass('role-box-menu-con-active');
                dom.find(`[id=${appid}]`).addClass('role-box-menu-con-active');
                dom.find('.check-box').removeClass('check-box-show');
                dom.find(`[id=menu-${appid}]`).addClass('check-box-show');
                scrollIntoView(`[id=${appid}]`);
                scrollIntoView(`[data-menu=${child}]`).addClass('hilite');
                scrollIntoView(`[data-menu=${subchild}]`).addClass('hilite');
                dom.find('.list-group').empty();
                me.closeMenuResult(dom);
            }
        });
    },
    loadPermission: function (callback) {
        let me = this;
        jmaa.rpc({
            model: 'rbac.role',
            method: 'loadPermissionList',
            args: {},
            onsuccess: function (r) {
                callback(r.data);
            },
        });
    },
});
