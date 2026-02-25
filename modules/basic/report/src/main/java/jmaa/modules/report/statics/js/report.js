//@ sourceURL=YiReport.js
jmaa.component('JReport', {
    getTpl() {
        return `<div class="jui-report">
                <div class="search"></div>
                <div class="btn-row">
                    <div class="toolbar">
                        <button type="button" class="btn btn-secondary btn-flat btn-query">${'查询'.t()}</button>
                        <button type="button" class="btn btn-default btn-flat btn-reset" title="${'重置查询条件'.t()}">${'重置'.t()}</button>
                        <div class="btn-group">
                            <button name="export" type="button" class="btn btn-default btn-flat btn-export">${'导出'.t()}</button>
                            <button type="button" class="btn-edit-group btn btn-flat btn-default dropdown-toggle dropdown-icon" data-toggle="dropdown"></button>
                            <div class="dropdown-menu" role="menu">
                                <button type="button" ref="export" class="dropdown-item btn-export-all">${'导出所有'.t()}</button>
                            </div>
                        </div>
                    </div>
                    <div class="pager"></div>
                </div>
                <div class="grid"></div>
            </div>`;
    },
    init() {
        let me = this;
        me.dom.html(me.getTpl());
        let code = decodeURI(window.location.hash.substring(1));
        jmaa.rpc({
            model: 'rpt.report',
            module: 'report',
            method: 'loadReport',
            args: {
                code,
            },
            onsuccess(r) {
                me.initReport(r.data);
            }
        });
    },
    initReport(data) {
        let me = this;
        me.reportId = data.id;
        me.reportName = data.name;
        let arch = jmaa.utils.parseXML(data.content);
        let script = arch.find('script').html();
        if (script) {
            eval(script);
        }
        let search = arch.find('search');
        me.search = me.dom.find('.search').JForm({
            arch: `<form>${search.html()}</form>`,
            view: me,
        });
        let defaultValue = me.search.getDefaultValue();
        me.search.setData(defaultValue);
        if (!search.find('editor').length) {
            me.dom.find('.search,.btn-reset').addClass('d-none');
        }
        me.pager = new JPager({
            dom: me.dom.find('.pager'),
            noCounting: true,
            limit: 100,
            limitChange(e, pager) {
                if (me.grid) {
                    me.grid.limit = pager.limit;
                }
            },
            pageChange(e, pager) {
                me.grid.load();
            },
        });
        let grid = arch.find('grid');
        let onInit = grid.find('on-init').html();
        if (onInit) {
            onInit = new Function('grid', onInit);
        }
        let onLoad = grid.find('on-load').html();
        if (onLoad) {
            onLoad = new Function('grid', onLoad);
        }
        me.grid = me.dom.find('.grid').JGrid({
            pager: me.pager,
            dataset: grid.attr('dataset'),
            arch: grid.prop('outerHTML'),
            vid: data.id,
            customizable: true,
            on: {
                init: function (e, grid) {
                    if (onInit) {
                        onInit(grid);
                    }
                },
                load: function (e, grid) {
                    if (onLoad) {
                        onLoad(grid);
                    }
                }
            },
            ajax(grid, callback) {
                me.searchData(grid, callback);
            }
        });
        me.dom.on('click', '.btn-query', function () {
            let btn = $(this);
            btn.attr('disabled', true);
            me.grid.load();
            setTimeout(() => btn.attr('disabled', false), 500);
        }).on('click', '.btn-export', function () {
            let btn = $(this);
            btn.attr('disabled', true);
            me.exportReport();
            setTimeout(() => btn.attr('disabled', false), 500);
        }).on('click', '.btn-export-all', function () {
            let btn = $(this);
            btn.attr('disabled', true);
            me.exportReport(true);
            setTimeout(() => btn.attr('disabled', false), 500);
        }).on('click', '.btn-reset', function () {
            let btn = $(this);
            btn.attr('disabled', true);
            me.search.create({});
            setTimeout(() => btn.attr('disabled', false), 500);
        });
        $(window).on('resize', function () {
            me.updateSize();
        });
        me.updateSize();
    },
    updateSize() {
        let me = this;
        let height = $(window).height() - me.dom.find('.search').height() - me.dom.find('.btn-row').height();
        me.dom.find('.grid').height(height - 14);
    },
    searchData(grid, callback) {
        let me = this;
        let data = me.search.getSubmitData();
        jmaa.rpc({
            url: jmaa.web.getTenantPath() + "/report/query",
            params: {
                id: me.reportId,
                params: data,
                dataset: grid.dataset,
                offset: grid.pager.getOffset(),
                limit: grid.pager.getLimit(),
            },
            onsuccess(r) {
                if (r.data.values.length > 0) {
                    let len = grid.pager.getOffset() + r.data.values.length;
                    if (r.data.hasNext === false) {
                        grid.pager.update({to: len, next: false, total: len});
                    } else {
                        grid.pager.update({to: len, next: true});
                    }
                } else {
                    grid.pager.noData();
                }
                callback({data: r.data.values});
            }
        });
    },
    exportReport(all) {
        let me = this;
        if (me.grid) {
            let exportAction = function (table) {
                $.fn.DataTable.ext.buttons.excelHtml5.action.call(
                    table.buttons(),
                    null,
                    table,
                    {},
                    {
                        header: true,
                        footer: true,
                        extension: '.xlsx',
                        filename: me.reportName,
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
            }
            if (all) {
                jmaa.mask('加载中...'.t());
                $('<div></div>').JGrid({
                    arch: me.grid.arch,
                    dataset: me.grid.dataset,
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
                    vid: me.grid.vid,
                    ajax(grid, callback, data, settings) {
                        me.grid.ajax(grid, function (data) {
                            callback(data);
                            jmaa.mask();
                            exportAction(grid.table);
                        }, data, settings);
                    }
                });
            } else {
                exportAction(me.grid.table);
            }
        }
    }
});
jmaa.editor('lookup', {
    extends: 'editors.many2many-tags',
    searchRelated(callback, fields) {
        let me = this;
        let dataset = me.dom.attr('dataset');
        jmaa.rpc({
            url: jmaa.web.getTenantPath() + "/report/query",
            params: {
                id: me.view.reportId,
                params: {keyword: me.keyword},
                dataset,
                limit: me.limit,
                offset: me.offset,
            },
            onsuccess(r) {
                callback(r);
            }
        });
    },
    getDirtyValue() {
        return this.getValue();
    }
});
jmaa.column('datetime', {
    render() {
        let me = this;
        let format = me.arch.attr('format') || 'yyyy-MM-DD HH:mm:ss';
        return function (data) {
            if (data === null || data === undefined) {
                return '';
            }
            return moment(data).format(format);
        };
    },
});
jmaa.column('date', {
    render() {
        let me = this;
        let format = me.arch.attr('format') || 'yyyy-MM-DD';
        return function (data) {
            if (data === null || data === undefined) {
                return '';
            }
            return moment(data).format(format);
        };
    },
});
jmaa.editor('date-range', {
    extends: 'editors.date-range',
    getRawValue() {
        let me = this;
        if (me.dom.find('input').val()) {
            return [me.startDate, me.endDate + ' 23:59:59'];
        }
        return [];
    },
});
jmaa.editor('datetime-range', {
    extends: 'editors.datetime-range',
    getRawValue() {
        let me = this;
        if (me.dom.find('input').val()) {
            return [me.startDate, me.endDate];
        }
        return [];
    },
});
jmaa.editor('multi-selection', {
    extends: 'editors.multi-selection',
    getRawValue() {
        return this.selected;
    },
});
jmaa.editor('boolean', {
    extends: 'editors.boolean',
    allowNull: true,
});
