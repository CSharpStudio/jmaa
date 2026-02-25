//双层父子菜单
jmaa.component("JMenu", {
    appMenu: '[role=app-menu]',
    setData(data) {
        let me = this;
        me.data = data;
        me.dom.empty();
        $.each(me.data.root, function (i, id) {
            let m = me.data[id];
            if (m) {
                let state = m.state ? `<span class="right badge badge-danger">${m.state}</span>` : '';
                let icon = m.icon ? `<img src="${m.icon}" alt=" " class="img-app nav-icon mr-3">` : `<i class="far fa-circle nav-icon text-warning mr-3"></i>`;
                let rightIcon = m.sub && m.sub.length > 0 ? `<i class="right fas fa-angle-left"></i>` : '';
                let html = `<li class="nav-item ${m.sub ? '' : 'link-menu'}" id="nav-item-${id}">
                                <a target="${m.target || 'tab'}" href="${m.url || '#'}" class="${m.cls || ''} nav-link">${icon}<p data-id="${id}">${m.name + state} ${rightIcon}</p></a>
                                ${m.sub && m.sub.length > 0 ? `<ul class="nav nav-treeview">${me.getSubNavMenu(m.sub, id)} </ul>` : ''}
                            </li>`;
                me.dom.append(html);
            }
        });
        me.dom.on('shown.bs.dropdown', '.nav-submenu', function () {
            let nav = $(this);
            let top = nav.offset().top;
            let menu = nav.find('.dropdown-menu-fixed');
            let height = menu.height();
            if (top + height > $(window).height()) {
                menu.css({
                    'bottom': 5,
                    'top': 'auto',
                    'left': nav.offset().left + nav.width(),
                    'position': 'fixed'
                });
            } else {
                menu.css({
                    'top': top,
                    'bottom': 'auto',
                    'left': nav.offset().left + nav.width(),
                    'position': 'fixed'
                });
            }
        });
        me.dom.find('.dropdown-submenu > .dropdown-item').on('click', function (e) {
            let btn = $(this);
            if (btn.attr('href') == '#') {
                e.preventDefault();
            }
        });
        me.dom.find('[data-submenu]').submenupicker();
        let mousedownInterval, mousedown;
        $(document).on('mousedown', '[data-widget=app-menu-scrollleft]', function (e) {
            e.preventDefault();
            clearInterval(mousedownInterval);
            let scrollOffset = -40;
            mousedown = true;
            me.menuScroll(scrollOffset);
            mousedownInterval = setInterval(function () {
                me.menuScroll(scrollOffset);
            }, 250);
        }).on('mousedown', '[data-widget=app-menu-scrollright]', function (e) {
            e.preventDefault();
            clearInterval(mousedownInterval);
            let scrollOffset = 40;
            mousedown = true;
            me.menuScroll(scrollOffset);
            mousedownInterval = setInterval(function () {
                me.menuScroll(scrollOffset);
            }, 250);
        }).on('mouseup', function () {
            if (mousedown) {
                mousedown = false;
                clearInterval(mousedownInterval);
                mousedownInterval = null;
            }
        });
        $(window).on('resize', function () {
            me.updateMenuScroll();
        });
    },
    menuScroll(offset) {
        const leftPos = $('.app-menus').scrollLeft();
        $('.app-menus').animate({scrollLeft: leftPos + offset,}, 250, 'linear');
    },
    //导航菜单的2级菜单
    getSubNavMenu(sub, parentId) {
        let me = this;
        let tpl = [];
        $.each(sub, function (i, id) {
            let m = me.data[id];
            if (m) {
                let icon = m.icon ? `<img src="${m.icon}" alt=" " class="img-app nav-icon mr-3">` : `<i class="fa fa-file-signature nav-icon mr-3"></i>`;
                let click = m.click ? 'onclick="' + m.click + '"' : "";
                if (m.sub) {
                    tpl.push(`<li class="nav-item nav-submenu link-menu dropright">
                                <a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" class="nav-link ${m.cls || ''}" data-display="static" data-toggle="dropdown" data-submenu ${click}>
                                    ${icon}<p>${m.name}<i class="right fas fa-angle-right"></i></p>
                                </a>
                                <div class="dropdown-menu dropdown-menu-fixed nav-item">${me.getSubMenu(m.sub, id, id)}</div>
                            </li>`);
                } else {
                    tpl.push(`<li class="nav-item link-menu"><a target="${m.target || 'tab'}" href="${m.url || '#'}" data-id="${id}" class="${m.cls || ''} nav-link" ${click}>${icon}<p>${m.name}</p></a></li>`);
                    if(!m.top_menu){
                        m.topMenu = parentId;
                    }
                }
                m.path = id;
            }
        });
        return tpl.join('');
    },
    //导航菜单的3+级菜单
    getSubMenu(sub, topId, pathId) {
        let me = this;
        let tpl = [];
        $.each(sub, function (i, id) {
            let m = me.data[id];
            if (m) {
                let click = m.click ? 'onclick="' + m.click + '"' : "";
                if (m.sub) {
                    tpl.push(`<div class="dropdown dropright dropdown-submenu nav-item">
                                <a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" aria-expanded="true" class="dropdown-item dropdown-toggle nav-link ${m.cls || ''}" tabindex="0" data-toggle="dropdown" data-submenu ${click}>${m.name}</a>
                            <div class="dropdown-menu nav-item">${me.getSubMenu(m.sub, m.top_menu ? id : topId, pathId)}</div></div>`);
                } else {
                    tpl.push(`<a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" class="${m.cls || ''} dropdown-item nav-link" ${click}>${m.name}</a>`);
                }
                if(!m.top_menu){
                    m.topMenu = topId;
                }
                m.path = pathId;
            }
        });
        return tpl.join('');
    },
    activeNavMenu(app) {
        let me = this;
        let targetId = ''
        for (let key in me.data) {
            if ('sub' in me.data[key] && me.data[key].sub.indexOf(app) >= 0) {
                targetId = key
            }
        }
        if (targetId) {
            let menu = $(`#nav-item-${targetId}`).addClass('menu-is-opening menu-open');
            menu.children('.nav-treeview').show();
            menu.find(`a.nav-link[data-id=${app}]`).closest('li').addClass('active');
        } else {
            $(`#nav-item-${app} .nav-link`).addClass('active');
        }
    },
    //顶部应用菜单
    showAppMenu(app) {
        let me = this;
        let menu = me.data[app];
        if (!menu) return;
        let appMenu = $(me.appMenu);
        let menus = appMenu.find('.app-menus').empty();
        let click = menu.click ? 'onclick="' + menu.click + '"' : "";
        appMenu.find('.app-menu').html(`<a target="${menu.target || 'tab'}" data-id="${app}" href="${menu.url || '#'}" class="${menu.cls || ''} nav-app-link nav-link pl-1" ${click}><p>${menu.name}</p></a>`);
        if (menu.sub) {
            let html = [];
            $.each(menu.sub, function (i, id) {
                let m = me.data[id];
                if (m && (!m.sub || m.sub.length > 0)) {
                    let click = m.click ? 'onclick="' + m.click + '"' : "";
                    if (m.sub) {
                        html.push(`<li class="nav-item dropdown app-submenu">
                                        <a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" class="nav-link dropdown-toggle ${m.cls || ''}" tabindex="0" data-toggle="dropdown" data-submenu ${click}>
                                            ${m.name}
                                        </a>
                                        <div class="dropdown-menu dropdown-menu-fixed">${me.getAppSubMenu(m.sub)}</div>
                                    </li>`);
                    } else {
                        html.push(`<li class="nav-item"><a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" class="${m.cls || ''} nav-link"${click}><p>${m.name}</p></a></li>`);
                    }
                }
            });
            menus.html(html.join(''));
        }
        appMenu.on('shown.bs.dropdown', '.app-submenu', function () {
            let dropdown = $(this);
            dropdown.find('.dropdown-menu-fixed').css({
                'top': '40px',
                'left': dropdown.offset().left - $(window).scrollLeft(),
                'position': 'fixed'
            });
        });
        appMenu.find('.dropdown-submenu > .dropdown-item').on('click', function (e) {
            let btn = $(this);
            if (btn.attr('href') == '#') {
                e.preventDefault();
            }
        });
        appMenu.find('[data-submenu]').submenupicker();
        me.updateMenuScroll();
    },
    updateMenuScroll() {
        let me = this;
        let menus = $('.app-menus');
        let visible = false;
        if (menus.scrollLeft() > 0) {
            visible = true;
        } else {
            let last = menus.children('li').last();
            if (last.length && last.position().left + last.width() - 5 > menus.position().left + menus.width()) {
                visible = true;
            }
        }
        if (visible) {
            $('[data-widget=app-menu-scrollleft],[data-widget=app-menu-scrollright]').show();
        } else {
            $('[data-widget=app-menu-scrollleft],[data-widget=app-menu-scrollright]').hide();
        }
    },
    //顶部应用2+菜单
    getAppSubMenu(sub) {
        let me = this, tpl = '';
        $.each(sub, function (i, id) {
            let m = me.data[id];
            if (m) {
                let click = m.click ? 'onclick="' + m.click + '"' : "";
                if (m.sub) {
                    tpl += `<div class="dropdown dropright dropdown-submenu">
                                <a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" class="dropdown-item dropdown-toggle ${m.cls || ''}" tabindex="0" data-toggle="dropdown" data-submenu ${click}>${m.name}</a>
                            <div class="dropdown-menu">${me.getAppSubMenu(m.sub)}</div></div>`;
                } else {
                    tpl += `<a target="${m.target || 'tab'}" data-id="${id}" href="${m.url || '#'}" class="${m.cls || ''} dropdown-item" ${click}>${m.name}</a>`;
                }
            }
        });
        return tpl;
    }
});
