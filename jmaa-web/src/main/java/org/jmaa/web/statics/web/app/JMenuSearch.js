jmaa.component('JMenuSearch', {
    searchResultCss: 'sidebar-search-results',
    notFoundText: '找不到数据'.t(),
    arrowSign: '->',
    minLength: 1,
    highlightName: true,
    highlightPath: true,
    highlightClass: 'text-light',
    placeholder: '请输入'.t(),
    maxResults: 10,
    init() {
        const me = this;
        if (me.dom.next('.' + me.searchResultCss).length === 0) {
            me.dom.after('<div class="' + me.searchResultCss + '"><div class="list-group"></div></div>');
        }
        me.results = me.dom.parent().find('.' + me.searchResultCss);
        me._addNotFound();
        me.dom
            .on('keyup', 'input', function (event) {
                if (event.keyCode == 38) {
                    event.preventDefault();
                    me.results.find('.list-group').children().last().focus();
                    return;
                }

                if (event.keyCode == 40) {
                    event.preventDefault();
                    me.results.find('.list-group').children().first().focus();
                    return;
                }

                setTimeout(function () {
                    me.search();
                }, 200);
            })
            .on('click', '.btn-sidebar', function (event) {
                event.preventDefault();
                me.toggle();
            })
            .find('input')
            .attr('placeholder', me.placeholder);
        me.results.on('click', '.list-group-item', function (event) {
            me.close();
        });
    },
    search() {
        const me = this;
        const searchValue = me.dom.find('input').val().toLowerCase();
        if (searchValue.length < this.minLength) {
            me.results.find('.list-group').empty();
            me._addNotFound();
            me.close();
            return;
        }
        const searchResults = me.items.filter(function (item) {
            for (let i = 0; i < item.keys.length; i++) {
                if (item.keys[i].includes(searchValue)) {
                    return true;
                }
            }
        });
        searchResults.sort(function (x, y) {
            if (x.name.includes(searchValue)) {
                if (y.name.includes(searchValue)) {
                    return x.name.length - y.name.length;
                }
                return -1;
            }
            return x.name.length - y.name.length;
        });
        const endResults = $(searchResults.slice(0, me.maxResults));
        me.results.find('.list-group').empty();

        if (endResults.length === 0) {
            me._addNotFound();
        } else {
            endResults.each(function (i, result) {
                me.results.find('.list-group').append(me._renderItem(escape(result.name), encodeURI(result.url), result.path, result.id));
            });
        }
        me.open();
    },
    open() {
        const me = this;
        me.dom.parent().addClass('sidebar-search-open');
        me.dom.find('.btn i').removeClass('fa-search').addClass('fa-times');
    },
    close() {
        const me = this;
        me.dom.parent().removeClass('sidebar-search-open');
        me.dom.find('.btn i').removeClass('fa-times').addClass('fa-search');
    },
    toggle() {
        const me = this;
        if (me.dom.parent().hasClass('sidebar-search-open')) {
            me.close();
        } else {
            me.search();
        }
    },
    _addNotFound() {
        this.results.find('.list-group').append(this._renderItem(this.notFoundText, '#', []));
    },
    _renderItem(name, link, path, id) {
        const me = this;
        path = path.join(' ' + me.arrowSign + ' ');
        name = unescape(name);
        link = decodeURI(link);
        if (me.highlightName || me.highlightPath) {
            const searchValue = me.dom.find('input').val().toLowerCase();
            const regExp = new RegExp(searchValue, 'gi');
            if (me.highlightName) {
                name = name.replace(regExp, function (str) {
                    return `<strong class="${me.highlightClass}">${str}</strong>`;
                });
            }
            if (me.highlightPath) {
                path = path.replace(regExp, function (str) {
                    return `<strong class="${me.highlightClass}">${str}</strong>`;
                });
            }
        }
        const groupItemElement = $('<a/>', {
            href: decodeURIComponent(link),
            class: 'list-group-item',
        }).attr('data-id', id);
        const searchTitleElement = $('<div/>', {
            class: 'search-title',
        }).html(name);
        const searchPathElement = $('<div/>', {
            class: 'search-path',
        }).html(path);
        groupItemElement.append(searchTitleElement).append(searchPathElement);
        return groupItemElement;
    },
    setData(d) {
        const me = this;
        const addSub = function (m, p) {
            if (m.sub) {
                $.each(m.sub, function () {
                    const m = d[this];
                    const item = {};
                    item.id = this;
                    item.name = m.name;
                    item.path = p.path.concat(p.name);
                    item.keys = p.keys.concat([item.name.toLowerCase(), Pinyin.GetQP(item.name), Pinyin.GetJP(item.name)]);
                    if (m.url) {
                        item.url = m.url;
                        me.items.push(item);
                    } else {
                        item.parent = this;
                    }
                    addSub(m, item);
                });
            }
        };
        me.items = [];
        $.each(d.root, function () {
            const m = d[this];
            const item = {};
            item.name = m.name;
            item.path = [];
            item.keys = [item.name.toLowerCase(), Pinyin.GetQP(item.name), Pinyin.GetJP(item.name)];
            item.id = this;
            item.parent = '';
            if (m.url) {
                item.url = m.url;
                me.items.push(item);
            }
            addSub(m, item);
        });
    },
});
