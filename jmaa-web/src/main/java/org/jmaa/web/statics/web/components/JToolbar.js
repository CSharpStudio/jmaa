jmaa.component('JToolbar', {
    /**
     * 默认按钮
     */
    defaultButtons: 'create|edit|delete|import',
    /**
     * 默认按钮样式
     */
    defaultButtonCss: 'btn-default',
    /**
     * 默认按钮模板
     */
    buttonsTpl: {
        query: `<button name="query" auth="read" class="btn-secondary" t-click="query">${'查询'.t()}</button>`,
        create: `<button name="create" class="btn-primary btn-edit-group" t-click="create">${'创建'.t()}</button>`,
        createChild: `<button name="createChild" auth="create" class="btn-primary btn-edit-group" t-click="createChild">${'创建'.t()}</button>`,
        copy: `<button name="copy" ref="create" auth="create" t-enable="id" class="btn-primary btn-edit-group" t-click="copy">${'复制'.t()}</button>`,
        edit: `<button name="edit" auth="update" class="btn-primary btn-edit-group" t-enable="id" t-click="edit">${'编辑'.t()}</button>`,
        delete: `<button name="delete" ref="edit" t-enable="ids" class="btn-danger btn-edit-group" t-click="delete" confirm="${'确定删除?'.t()}">${'删除'.t()}</button>`,
        save: `<button name="save" auth="create|update" class="btn-primary btn-edit-group" t-click="save">${'保存'.t()}</button>`,
        reload: `<button name="reload" auth="read" position="after" t-click="reload">${'刷新'.t()}</button>`,
        import: `<button name="import" auth="importData" class="btn-default btn-edit-group" position="after" t-click="import">${'导入'.t()}</button>`,
        export: `<button name="export" auth="read" position="after" t-click="export">${'导出'.t()}</button>`,
    },
    /**
     * 初始化工具条
     */
    init() {
        const me = this;
        const dom = me.dom;
        const tbar = jmaa.utils.parseXML(me.arch).find('toolbar');
        me.editable = tbar.attr('t-editable');
        dom.empty();
        if (tbar.length == 0) return;
        me.initButtons(tbar);
        dom.unbind()
            .html(tbar.html())
            .off('click')
            .on('click', 'button[service]', function (e) {
                if (!me.design) {
                    me.serviceHandler(e, $(this));
                }
            })
            .on('click', '[t-click]', function (e) {
                if (!me.design) {
                    me.clickHandler(e, $(this));
                }
            })
            .on('keyup', '[t-enter]', function (e) {
                if (!me.design) {
                    me.enterHandler(e, $(this));
                }
            });
        me.update([]);
        me.onEvent("init", tbar.attr('on-init'));
        me.dom.triggerHandler('init', [me]);
    },
    enterHandler(e, el) {
        let me = this;
        if (e.keyCode == 13) {
            const method = el.attr('t-enter');
            const fn = new Function('return this.' + method).call(me.view, e, me.target, me.view);
            if (fn instanceof Function) {
                fn.call(me.view, e, me.target, me.view);
            }
        }
    },
    clickHandler(e, btn) {
        let me = this;
        e.preventDefault();
        e.stopPropagation();
        const click = btn.attr('t-click');
        const cfm = btn.attr('confirm');
        const gap = eval(btn.attr('gap') || 500);
        btn.attr('disabled', true);
        btn.attr('clicking', "1");
        if (cfm) {
            jmaa.msg.confirm({
                content: cfm,
                submit() {
                    const fn = new Function('return this.' + click).call(me.view, e, me.target, me.view);
                    if (fn instanceof Function) {
                        fn.call(me.view, e, me.target, me.view);
                    }
                },
            });
        } else {
            const fn = new Function('return this.' + click).call(me.view, e, me.target, me.view);
            if (fn instanceof Function) {
                fn.call(me.view, e, me.target, me.view);
            }
        }
        setTimeout(function () {
            if (btn.attr("clicking") === "1") {
                btn.attr('disabled', false);
                btn.removeAttr('clicking');
            }
        }, gap);
    },
    serviceHandler(e, btn) {
        let me = this;
        e.preventDefault();
        e.stopPropagation();
        const cfm = btn.attr('confirm');
        const gap = eval(btn.attr('gap') || 500);
        btn.attr('disabled', true);
        btn.attr('clicking', "1");
        if (cfm) {
            jmaa.msg.confirm({
                content: cfm,
                submit() {
                    !me.design && view.call(btn.attr('service'), me.target);
                },
            });
        } else {
            !me.design && view.call(btn.attr('service'), me.target);
        }
        setTimeout(function () {
            if (btn.attr("clicking") === "1") {
                btn.attr('disabled', false);
            }
        }, gap);
    },
    initButtons(tbar) {
        let me = this;
        let defaultBtns = me.getDefaultButtons(tbar, tbar.attr('buttons') || me.defaultButtons);
        tbar.prepend(defaultBtns[0]);
        tbar.append(defaultBtns[1]);
        tbar.children('button').each(function () {
            const btn = $(this);
            const name = btn.attr('name') || btn.attr('service') || btn.attr('t-click');
            const auth = btn.attr('auth') || name || '';
            const cls = btn.attr('class');
            let allow = me.auths === '@all';
            if (!allow) {
                $.each(auth.split('|'), function (idx, item) {
                    if (me.auths.indexOf(item) > -1) {
                        allow = true;
                        return true;
                    }
                });
            }
            if (allow) {
                if (!btn.hasClass('btn')) {
                    btn.addClass('btn');
                }
                if (!btn.hasClass('btn-flat')) {
                    btn.addClass('btn-flat');
                }
                if (!cls || cls.indexOf('btn-') == -1) {
                    btn.addClass(me.defaultButtonCss);
                }
                const label = btn.attr('label') || '';
                if (label) {
                    btn.text(label.t());
                }
                btn.attr('auth', auth);
                btn.attr('name', name || 'btn' + jmaa.nextId());
                if (!btn.attr('type')) {
                    btn.attr('type', 'button');
                }
                let visible = btn.attr('t-visible') || '';
                if (visible) {
                    btn.removeAttr('t-visible');
                    visible = ` t-visible="${visible}"`;
                }
                btn.replaceWith(`<div name="btn_group_${name}"${visible} class="btn-group">${btn.prop('outerHTML')}</div>`);
            } else {
                btn.replaceWith('');
            }
        });
        let after = tbar.children('[position=after]').remove();
        tbar.append(after);
        tbar.find('button[ref]').each(function () {
            const btn = $(this);
            const ref = btn.attr('ref');
            const group = tbar.find(`div[name=btn_group_${ref}]`);
            if (group.length > 0) {
                btn.attr('class', 'dropdown-item');
                const drop = group.find('.dropdown-menu');
                if (drop.length > 0) {
                    drop.append(btn.prop('outerHTML'));
                } else {
                    let cls = group.find('button[name=' + ref + ']').attr('class');
                    cls = cls.replace('disabled', '');
                    group.append('<button type="button" class="' + cls + ' dropdown-toggle dropdown-icon" data-toggle="dropdown"> </button>');
                    group.append('<div class="dropdown-menu" role="menu">' + btn.prop('outerHTML') + '</div>');
                }
                tbar.find(`div[name=btn_group_${btn.attr('name')}]`).remove();
            }
        });
    },
    evaluate(expr, data, parent) {
        let value = true;
        if (expr === 'id') {
            value = data.length == 1 && data[0].id;
        } else if (expr === 'ids') {
            value = data.length > 0 && data[0].id;
        } else if (expr !== 'parent') {
            if (expr.startsWith('ids:')) {
                if (data.length == 0) {
                    value = false;
                } else {
                    expr = expr.substring(4);
                    expr = expr.replace(/ and /gi, " && ");
                    for (let i = 0; i < data.length; i++) {
                        let d = data[i];
                        d.__test_active = new Function('parent', 'with(this) return ' + expr);
                        if (!d.__test_active(parent)) {
                            value = false;
                            break;
                        }
                    }
                }
            } else {
                if (data.length !== 1) {
                    value = false;
                } else {
                    if (expr.startsWith('id:')) {
                        expr = expr.substring(3);
                    }
                    expr = expr.replace(/ and /gi, " && ");
                    let d = data[0];
                    d.__test_active = new Function('parent', 'with(this) return ' + expr);
                    if (!d.__test_active(parent)) {
                        value = false;
                    }
                }
            }
        }
        return value;
    },
    /**
     * 更新工具条中按钮状态，根据t-enable计算
     * expr=== parent时，按钮状态只能有父级控制
     * @param data
     */
    update(data, parent) {
        this.updateEnable(data, parent);
        this.updateVisible(data, parent);
    },
    updateEditable(data, parent) {
        let me = this;
        if (me.editable && !me.evaluate(me.editable, data, parent)) {
            me.dom.find('button').attr('disabled', true);
            me.dom.find('button[auth=read]').attr('disabled', false);
            return false;
        }
        return true;
    },
    updateVisible(data, parent) {
        let me = this;
        me.dom.find('[t-visible]').each(function () {
            const btn = $(this);
            let expr = btn.attr('t-visible');
            let active = me.evaluate(expr, data, parent);
            if (active) {
                btn.removeClass('d-none');
            } else {
                btn.addClass('d-none');
            }
        });
    },
    updateEnable(data, parent) {
        let me = this;
        me.dom.find('button[t-enable]').each(function () {
            const btn = $(this);
            let expr = btn.attr('t-enable');
            btn.removeAttr("clicking");
            let active = me.evaluate(expr, data, parent);
            if (active) {
                btn.attr('disabled', false);
            } else {
                btn.attr('disabled', true);
            }
        });
    },
    /**
     * 获取默认按钮，可通过name覆盖默认按钮
     * @param tbar
     * @param btns
     * @returns {[string,string]}
     */
    getDefaultButtons(tbar, btns) {
        const me = this;
        let before = '';
        let after = '';
        const tpl = {};
        const addBtn = function (name) {
            const btn = tpl[name];
            if (btn) {
                if ($(btn).attr('position') == 'after') {
                    after += btn + '\n';
                } else {
                    before += btn + '\n';
                }
            }
        };
        const addTpl = function (name) {
            const btn = tbar.find(`[name=${name}]`);
            if (btn.length > 0) {
                let b = $(me.buttonsTpl[name]);
                if (b.length) {
                    var attrs = btn.prop('attributes');
                    $.each(attrs, function () {
                        b.attr(this.name, this.value);
                    });
                    tpl[name] = b.prop('outerHTML');
                } else {
                    tpl[name] = btn.prop('outerHTML');
                }
                btn.remove();
            } else {
                tpl[name] = me.buttonsTpl[name];
            }
        };
        for (const btn of btns.split('|')) {
            if (btn === 'default') {
                for (const defaultBtn of me.defaultButtons.split('|')) {
                    addTpl(defaultBtn);
                }
            } else {
                addTpl(btn);
            }
        }
        for (const btn of btns.split('|')) {
            if (btn === 'default') {
                for (const defaultBtn of me.defaultButtons.split('|')) {
                    addBtn(defaultBtn);
                }
            } else {
                addBtn(btn);
            }
        }
        return [before, after];
    },
});
