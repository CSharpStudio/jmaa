(function (factory) {
    'use strict';
    factory(jQuery, window, document);
})(function ($, window, document, undefined) {
    'use strict';

    const NAME = 'JFrame';
    const SELECTOR_DATA_TOGGLE_CLOSE = '[data-widget="jui-frame-close"]';
    const SELECTOR_DATA_TOGGLE_HOME = '[data-widget="jui-frame-home"]';
    const SELECTOR_DATA_TOGGLE_SCROLL_LEFT = '[data-widget="jui-frame-scrollleft"]';
    const SELECTOR_DATA_TOGGLE_SCROLL_RIGHT = '[data-widget="jui-frame-scrollright"]';
    const SELECTOR_DATA_TOGGLE_FULLSCREEN = '[data-widget="jui-frame-fullscreen"]';
    const SELECTOR_CONTENT_WRAPPER = '.content-wrapper';
    const SELECTOR_CONTENT_IFRAME = SELECTOR_CONTENT_WRAPPER + ' iframe';
    const SELECTOR_TAB_NAV = SELECTOR_CONTENT_WRAPPER + '.jui-frame-mode .nav';
    const SELECTOR_TAB_NAVBAR_NAV = SELECTOR_CONTENT_WRAPPER + '.jui-frame-mode .navbar-nav';
    const SELECTOR_TAB_NAVBAR_NAV_ITEM = SELECTOR_TAB_NAVBAR_NAV + ' .nav-item';
    const SELECTOR_TAB_NAVBAR_NAV_LINK = SELECTOR_TAB_NAVBAR_NAV + ' .nav-link';
    const SELECTOR_TAB_CONTENT = SELECTOR_CONTENT_WRAPPER + '.jui-frame-mode .tab-content';
    const SELECTOR_TAB_EMPTY = SELECTOR_TAB_CONTENT + ' .tab-empty';
    const SELECTOR_TAB_LOADING = SELECTOR_TAB_CONTENT + ' .tab-loading';
    const SELECTOR_TAB_PANE = SELECTOR_TAB_CONTENT + ' .tab-pane';
    const SELECTOR_SIDEBAR_MENU_ITEM = '.main-sidebar .nav-item > a.nav-link';
    const SELECTOR_SIDEBAR_SEARCH_ITEM = '.sidebar-search-results .list-group-item';
    const SELECTOR_HEADER_MENU_ITEM = '.main-header .nav-item a.nav-link';
    const SELECTOR_HEADER_DROPDOWN_ITEM = '.main-header a.dropdown-item';
    const CLASS_NAME_IFRAME_MODE$1 = 'jui-frame-mode';
    const CLASS_NAME_FULLSCREEN_MODE = 'jui-frame-mode-fullscreen';
    const Default = {
        onTabClick: function onTabClick(item) {
            return item;
        },
        onTabChanged: function onTabChanged(item) {
            return item;
        },
        onTabCreated: function onTabCreated(item) {
            return item;
        },
        onTabClosing: function onTabClosing(item, callback) {
            callback();
        },
        autoIframeMode: true,
        autoItemActive: false,
        autoShowNewTab: true,
        autoDarkMode: false,
        allowDuplicates: false,
        allowReload: true,
        loadingScreen: true,
        useNavbarItems: true,
        scrollOffset: 40,
        scrollBehaviorSwap: false,
        iconMaximize: 'fa-expand',
        iconMinimize: 'fa-compress',
    };

    const JFrame = (function () {
        function JFrame(element, config) {
            this._config = config;
            this._element = element;

            this._init();
        } // Public

        const _proto = JFrame.prototype;

        _proto.onTabClick = function onTabClick(item) {
            this._config.onTabClick(item);
        };

        _proto.onTabChanged = function onTabChanged(item) {
            this._config.onTabChanged(item);
        };

        _proto.onTabClosing = function onTabClosing(item, callback) {
            return this._config.onTabClosing(item, callback);
        };

        _proto.onTabCreated = function onTabCreated(item) {
            this._config.onTabCreated(item);
        };

        _proto.createTab = function createTab(title, link, uniqueName, autoOpen) {
            const _this = this;

            let tabId = 'panel-' + uniqueName;
            let navId = 'tab-' + uniqueName;

            if (this._config.allowDuplicates) {
                tabId += '-' + Math.floor(Math.random() * 1000);
                navId += '-' + Math.floor(Math.random() * 1000);
            }

            const newNavItem = $(unescape(escape('<li class="nav-item" role="presentation"><a href="#" class="btn-jui-frame-close" data-widget="jui-frame-close" data-type="only-this"><i class="fas fa-times"></i></a><a class="nav-link" data-toggle="row" id="' + navId + '" href="#' + tabId + '" role="tab" aria-controls="' + tabId + '" aria-selected="false">' + title + '</a></li>')));
            $(SELECTOR_TAB_NAVBAR_NAV).append(newNavItem);
            $(SELECTOR_TAB_NAVBAR_NAV).children().last()[0].scrollIntoView();
            $('.tab-pane').removeClass('active show')
            const newTabItem = '<div class="tab-pane fade" id="' + tabId + '" role="tabpanel" aria-labelledby="' + navId + '"><iframe src="' + link + '"></iframe></div>';
            $(SELECTOR_TAB_CONTENT).append(unescape(escape(newTabItem)));
            newNavItem[0].scrollIntoView();
            if (autoOpen) {
                if (this._config.loadingScreen) {
                    const $loadingScreen = $(SELECTOR_TAB_LOADING);
                    $loadingScreen.fadeIn();
                    $(tabId + ' iframe').ready(function () {
                        if (typeof _this._config.loadingScreen === 'number') {
                            _this.switchTab('#' + navId);

                            setTimeout(function () {
                                $loadingScreen.fadeOut();
                            }, _this._config.loadingScreen);
                        } else {
                            _this.switchTab('#' + navId);

                            $loadingScreen.fadeOut();
                        }
                    });
                } else {
                    this.switchTab('#' + navId);
                }
            }

            this.onTabCreated($('#' + navId));
        };

        _proto.openTab = function (title, link, uniqueName, autoOpen) {
            if (link === '#' || link === '' || link === undefined) {
                return;
            }
            if (autoOpen === void 0) {
                autoOpen = this._config.autoShowNewTab;
            }
            if (!uniqueName) {
                let encode = function (str) {
                    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
                    const base = chars.length;
                    const encoder = new TextEncoder();
                    const bytes = encoder.encode(str);
                    let num = 0n;
                    for (const byte of bytes) {
                        num = num * 256n + BigInt(byte);
                    }
                    if (num === 0n) return chars[0];
                    let result = '';
                    while (num > 0n) {
                        const remainder = num % BigInt(base);
                        result = chars[Number(remainder)] + result;
                        num = num / BigInt(base);
                    }
                    return result;
                }
                uniqueName = encode(title);
            }
            let exists = false;
            $(SELECTOR_TAB_NAVBAR_NAV)
                .children()
                .each(function (item) {
                    if ($(this).text() === unescape(escape(title))) {
                        exists = true;
                    }
                });
            if (exists) {
                let navId = "#tab-" + uniqueName;
                this.switchTab(navId);
                this.onTabClosing($(navId).parent(), function () {
                    let tabId = $(navId).attr('href');
                    $(tabId + " iframe").attr('src', link);
                });
            } else {
                this.createTab(title, link, uniqueName, autoOpen);
            }
        };

        _proto.openTabSidebar = function openTabSidebar(item, autoOpen) {
            let $item = $(item).clone();
            if ($item.attr('href') === undefined) {
                $item = $(item).parents('a').clone();
            }
            $item.find('.right, .search-path').remove();
            let title = $item.find('p').text();
            if (title === '') {
                title = $item.text();
            }
            const link = $item.attr('href');
            const target = $item.attr('target');
            if (target == 'blank') {
                const newWindow = window.open('_blank');
                newWindow.location = link;
                return;
            }
            this.openTab(title, link, $item.attr('data-id'), autoOpen);
        };

        _proto.switchTab = function switchTab(item, reload) {
            const _this2 = this;

            if (reload === void 0) {
                reload = false;
            }

            const $item = $(item);
            const tabId = $item.attr('href');
            $(SELECTOR_TAB_EMPTY).hide();

            if (reload) {
                const $loadingScreen = $(SELECTOR_TAB_LOADING);

                if (this._config.loadingScreen) {
                    $loadingScreen.show(0, function () {
                        $(tabId + ' iframe')
                            .attr('src', $(tabId + ' iframe').attr('src'))
                            .ready(function () {
                                if (_this2._config.loadingScreen) {
                                    if (typeof _this2._config.loadingScreen === 'number') {
                                        setTimeout(function () {
                                            $loadingScreen.fadeOut();
                                        }, _this2._config.loadingScreen);
                                    } else {
                                        $loadingScreen.fadeOut();
                                    }
                                }
                            });
                    });
                } else {
                    $(tabId + ' iframe').attr('src', $(tabId + ' iframe').attr('src'));
                }
            }

            $(SELECTOR_TAB_NAVBAR_NAV + ' .active')
                .tab('dispose')
                .removeClass('active');

            this._fixHeight();

            $item.tab('show');
            let li = $item.parents('li').addClass('active')[0];
            li && li.scrollIntoView();
            this.onTabChanged($item);

            if (this._config.autoItemActive) {
                this._setItemActive($(tabId + ' iframe').attr('src'));
            }
        };

        _proto.removeActiveTab = function removeActiveTab(type, element) {
            const me = this;
            const activeTab = function ($navItemParent, navItemIndex) {
                if ($(SELECTOR_TAB_CONTENT).children().length == $(SELECTOR_TAB_EMPTY + ', ' + SELECTOR_TAB_LOADING).length) {
                    $(SELECTOR_TAB_EMPTY).show();
                    me.onTabChanged('home');
                } else {
                    const prevNavItemIndex = navItemIndex - 1;
                    me.switchTab($navItemParent.children().eq(prevNavItemIndex).find('a.nav-link'));
                }
            }
            if (type == 'all') {
                let items = $(SELECTOR_TAB_NAVBAR_NAV_ITEM);
                const remove = function ($navItem) {
                    const $navItemParent = $navItem.parent();
                    const navItemIndex = $navItem.index();
                    const tabId = $navItem.find('.nav-link').attr('aria-controls');
                    $navItem.remove();
                    $('#' + tabId).remove();
                    activeTab($navItemParent, navItemIndex);
                }
                for (let item of items) {
                    let $navItem = $(item);
                    this.onTabClosing($navItem, () => remove($navItem));
                }
            } else if (type == 'all-other') {
                let items = $(SELECTOR_TAB_NAVBAR_NAV_ITEM + ':not(.active)');
                const remove = function ($navItem) {
                    const $navItemParent = $navItem.parent();
                    const navItemIndex = $navItem.index();
                    const tabId = $navItem.find('.nav-link').attr('aria-controls');
                    $navItem.remove();
                    $('#' + tabId).remove();
                    activeTab($navItemParent, navItemIndex);
                }
                for (let item of items) {
                    let $navItem = $(item);
                    this.onTabClosing($navItem, () => remove($navItem));
                }
            } else if (type == 'only-this') {
                const $navClose = $(element);
                const $navItem = $navClose.parent('.nav-item');
                const remove = function () {
                    const $navItemParent = $navItem.parent();
                    const navItemIndex = $navItem.index();
                    const tabId = $navClose.siblings('.nav-link').attr('aria-controls');
                    $navItem.remove();
                    $('#' + tabId).remove();
                    activeTab($navItemParent, navItemIndex);
                }
                this.onTabClosing($navItem, remove);
            } else {
                const $navItem = $(SELECTOR_TAB_NAVBAR_NAV_ITEM + '.active');
                const me = this;
                const remove = function () {
                    const $navItemParent = $navItem.parent();
                    const navItemIndex = $navItem.index();
                    $navItem.remove();
                    $(SELECTOR_TAB_PANE + '.active').remove();
                    activeTab($navItemParent, navItemIndex);
                }
                this.onTabClosing($navItem, remove);
            }
        };

        _proto.toggleFullscreen = function toggleFullscreen() {
            if ($('body').hasClass(CLASS_NAME_FULLSCREEN_MODE)) {
                $(SELECTOR_DATA_TOGGLE_FULLSCREEN + ' i')
                    .removeClass(this._config.iconMinimize)
                    .addClass(this._config.iconMaximize);
                $('body').removeClass(CLASS_NAME_FULLSCREEN_MODE);
                $(SELECTOR_TAB_EMPTY + ', ' + SELECTOR_TAB_LOADING).height('100%');
                $(SELECTOR_CONTENT_WRAPPER).height('100%');
                $(SELECTOR_CONTENT_IFRAME).height('100%');
            } else {
                $(SELECTOR_DATA_TOGGLE_FULLSCREEN + ' i')
                    .removeClass(this._config.iconMaximize)
                    .addClass(this._config.iconMinimize);
                $('body').addClass(CLASS_NAME_FULLSCREEN_MODE);
            }

            $(window).trigger('resize');

            this._fixHeight(true);
        }; // Private

        _proto._init = function _init() {
            const usingDefTab = $(SELECTOR_TAB_CONTENT).children().length > 2;

            this._setupListeners();

            this._fixHeight(true);

            if (usingDefTab) {
                const $el = $('' + SELECTOR_TAB_PANE).first(); // eslint-disable-next-line no-console

                console.log($el);
                const uniqueName = $el.attr('id').replace('panel-', '');
                const navId = '#tab-' + uniqueName;
                this.switchTab(navId, true);
            }
        };

        _proto._initFrameElement = function _initFrameElement() {
            if (window.frameElement && this._config.autoIframeMode) {
                const $body = $('body');
                $body.addClass(CLASS_NAME_IFRAME_MODE$1);

                if (this._config.autoDarkMode) {
                    $body.addClass('dark-mode');
                }
            }
        };

        _proto._navScroll = function _navScroll(offset) {
            const leftPos = $(SELECTOR_TAB_NAVBAR_NAV).scrollLeft();
            $(SELECTOR_TAB_NAVBAR_NAV).animate(
                {
                    scrollLeft: leftPos + offset,
                },
                250,
                'linear',
            );
        };

        _proto._setupListeners = function _setupListeners() {
            const _this3 = this;

            $(window).on('resize', function () {
                setTimeout(function () {
                    _this3._fixHeight();
                }, 1);
            });

            if ($(SELECTOR_CONTENT_WRAPPER).hasClass(CLASS_NAME_IFRAME_MODE$1)) {
                $(document).on('click', SELECTOR_SIDEBAR_MENU_ITEM + ', ' + SELECTOR_SIDEBAR_SEARCH_ITEM, function (e) {
                    e.preventDefault();

                    _this3.openTabSidebar(e.target);
                });

                if (this._config.useNavbarItems) {
                    $(document).on('click', SELECTOR_HEADER_MENU_ITEM + ', ' + SELECTOR_HEADER_DROPDOWN_ITEM, function (e) {
                        e.preventDefault();

                        _this3.openTabSidebar(e.target);
                    });
                }
            }
            $(document).on('click', SELECTOR_TAB_NAVBAR_NAV_LINK, function (e) {
                e.preventDefault();

                _this3.onTabClick(e.target);

                _this3.switchTab(e.target);
            });
            $(document).on('click', SELECTOR_DATA_TOGGLE_CLOSE, function (e) {
                e.preventDefault();
                let target = e.target;

                if (target.nodeName == 'I') {
                    target = e.target.offsetParent;
                }
                // _this3.removeActiveTab(target.attributes['data-type'] ? target.attributes['data-type'].nodeValue : null, target);
                const type = $(this).attr('data-type');
                _this3.removeActiveTab(type, target);
            });
            $(document).on('click', SELECTOR_DATA_TOGGLE_HOME, function (e) {
                e.preventDefault();
                $(SELECTOR_SIDEBAR_MENU_ITEM + ', ' + SELECTOR_HEADER_DROPDOWN_ITEM).removeClass('active');
                $(SELECTOR_TAB_NAVBAR_NAV + ' .active').removeClass('active');
                $(SELECTOR_TAB_PANE).removeClass('active');
                $(SELECTOR_TAB_EMPTY).show();
                _this3.onTabChanged('home');
            });
            $(document).on('click', SELECTOR_DATA_TOGGLE_FULLSCREEN, function (e) {
                e.preventDefault();

                _this3.toggleFullscreen();
            });
            let mousedown = false;
            let mousedownInterval = null;
            $(document).on('mousedown', SELECTOR_DATA_TOGGLE_SCROLL_LEFT, function (e) {
                e.preventDefault();
                clearInterval(mousedownInterval);
                let scrollOffset = _this3._config.scrollOffset;

                if (!_this3._config.scrollBehaviorSwap) {
                    scrollOffset = -scrollOffset;
                }

                mousedown = true;

                _this3._navScroll(scrollOffset);

                mousedownInterval = setInterval(function () {
                    _this3._navScroll(scrollOffset);
                }, 250);
            });
            $(document).on('mousedown', SELECTOR_DATA_TOGGLE_SCROLL_RIGHT, function (e) {
                e.preventDefault();
                clearInterval(mousedownInterval);
                let scrollOffset = _this3._config.scrollOffset;

                if (_this3._config.scrollBehaviorSwap) {
                    scrollOffset = -scrollOffset;
                }

                mousedown = true;

                _this3._navScroll(scrollOffset);

                mousedownInterval = setInterval(function () {
                    _this3._navScroll(scrollOffset);
                }, 250);
            });
            $(document).on('click', SELECTOR_DATA_TOGGLE_SCROLL_LEFT + "," + SELECTOR_DATA_TOGGLE_SCROLL_RIGHT, function (e) {
                e.preventDefault();
            });
            $(document).on('mouseup', function () {
                if (mousedown) {
                    mousedown = false;
                    clearInterval(mousedownInterval);
                    mousedownInterval = null;
                }
            });
        };

        _proto._setItemActive = function _setItemActive(href) {
            $(SELECTOR_SIDEBAR_MENU_ITEM + ', ' + SELECTOR_HEADER_DROPDOWN_ITEM).removeClass('active');
            $(SELECTOR_HEADER_MENU_ITEM).parent().removeClass('active');
            const $headerMenuItem = $(SELECTOR_HEADER_MENU_ITEM + '[href$="' + href + '"]');
            const $headerDropdownItem = $(SELECTOR_HEADER_DROPDOWN_ITEM + '[href$="' + href + '"]');
            const $sidebarMenuItem = $(SELECTOR_SIDEBAR_MENU_ITEM + '[href$="' + href + '"]');
            $headerMenuItem.each(function (i, e) {
                $(e).parent().addClass('active');
            });
            $headerDropdownItem.each(function (i, e) {
                $(e).addClass('active');
            });
            $sidebarMenuItem.each(function (i, e) {
                $(e).addClass('active');
                $(e).parents('.nav-treeview').prevAll('.nav-link').addClass('active');
            });
        };

        _proto._fixHeight = function _fixHeight(tabEmpty) {
            if (tabEmpty === void 0) {
                tabEmpty = false;
            }

            if ($('body').hasClass(CLASS_NAME_FULLSCREEN_MODE)) {
                const windowHeight = $(window).height();
                const navbarHeight = $(SELECTOR_TAB_NAV).outerHeight();
                $(SELECTOR_TAB_EMPTY + ', ' + SELECTOR_TAB_LOADING + ', ' + SELECTOR_CONTENT_IFRAME).height(windowHeight - navbarHeight);
                $(SELECTOR_CONTENT_WRAPPER).height(windowHeight);
            } else {
                const contentWrapperHeight = parseFloat($(SELECTOR_CONTENT_WRAPPER).css('height'));

                const _navbarHeight = $(SELECTOR_TAB_NAV).outerHeight();

                if (tabEmpty == true) {
                    setTimeout(function () {
                        $(SELECTOR_TAB_EMPTY + ', ' + SELECTOR_TAB_LOADING).height(contentWrapperHeight - _navbarHeight);
                    }, 50);
                } else {
                    $(SELECTOR_CONTENT_IFRAME).height(contentWrapperHeight - _navbarHeight);
                }
            }
        };
        return JFrame;
    })();

    $.fn[NAME] = function (config) {
        const _options = $.extend({}, Default, config);
        const plugin = new JFrame($(this), _options);
        return plugin;
    };
});
