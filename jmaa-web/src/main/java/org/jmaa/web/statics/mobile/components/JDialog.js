/**
 * 对话框
 * 使用xml声明结构，支持模型的field
 * <pre>
 *     jmaa.showDialog({
 *          title:'对话框'.t(),
 *          init(dialog){
 *              dialog.body.html('hello');
 *          }
 *     });
 * </pre>
 */
jmaa.define('JDialog', {
    /**
     * 唯一标识
     */
    id: 'dialog-id',
    /**
     * 标题
     */
    title: '对话框'.t(),
    /**
     * 提示按钮文本
     */
    submitText: '确定'.t(),
    /**
     * 取消按钮文本
     */
    cancelText: '关闭'.t(),
    /**
     * 加载中文本
     */
    loadingText: '加载中'.t(),
    /**
     * 默认是最大的对话框，
     * 中等 css:'', 最小 css:'modal-sm' 全屏 'modal-max' 上下粘性定位'modal-sticky'
     */
    css: 'modal-xl',
    bgClass: '',
    /**
     * 是否显示全屏按钮  false 不显示  true 显示
     */
    canMaximize: true,
    /**
     * 是否展开body  false 不显示  true 显示
     */
    showBody: true,
    /**
     * 是否头部、底部粘住页面上下两边  false 不黏住  true 黏住
     */
    sticky: true,
    /**
     * 获取对话框模板
     */
    getTpl() {
        return `<div id="${this.id}" data-url="dialog-id" tabindex="0" class="ui-page ui-page-theme-a ui-dialog" style="display: block;">
                    <div role="dialog" class="ui-dialog-contain ui-overlay-shadow ui-corner-all">
                        <div data-role="header" role="banner" class="ui-header ui-bar-inherit">
                            <a role="close" href="#" class="ui-btn ui-corner-all ui-icon-delete ui-btn-icon-notext ui-btn-left">Close</a>
                            <h1 class="ui-title" role="heading" aria-level="1">${this.title}</h1>
                        </div>
                        <div data-role="content" class="ui-content" role="main">
                            <p>加载中</p>
                        </div>
                        <div data-role="footer" style="display: none" role="contentinfo" class="ui-footer ui-bar-inherit">
                            <div class="d-flex justify-content-between">
                                <div class="footer-left">
                                </div>
                                <div class="footer-right">
                                    <button type="button" role="btn-submit" class="ui-btn">${this.submitText}</button>
                                </div>
                            </div>
                        </div>
                  </div>
              </div>`;
    },
    /**
     * 创建对话框实例
     */
    __init__(opt) {
        const me = this;
        jmaa.utils.apply(true, me, opt);
        $('#' + me.id).remove();
        $(document.body).append(me.getTpl());
        me.dom = $('#' + me.id).show();
        me.body = me.dom.find('[data-role=content]');
        if (me.submit) {
            me.dom.find('[data-role=footer]').show();
            me.dom.on('click', '[role=btn-submit]', function () {
                if (me.submit(me)) {
                    me.dom.remove();
                }
            });
        } else {
            me.dom.find('[role=btn-submit]').remove();
        }
        me.init(me);
        me.dom.on('click', '[role=close]', function () {
            if (me.cancel) {
                me.cancel(me);
            }
            me.close();
        });
    },
    /**
     * 模板方法，初始化对话框内容的入口。
     */
    init: jmaa.emptyFn,
    /**
     * 更新对话框标题
     */
    updateTitle(title) {
        this.dom.find('.ui-title').html(title);
    },
    /**
     * 是否繁忙，繁忙时修改提交按钮为`请稍等`，并设置为禁用状态
     */
    busy(busy) {
        if (busy) {
            this.dom.find('[role=btn-submit]').html('请稍等'.t()).attr('disabled', true);
        } else {
            this.dom.find('[role=btn-submit]').html(this.submitText).attr('disabled', false);
        }
    },
    /**
     * 关闭对话框
     */
    close() {
        this.dom.remove();
    },
});

/**
 * 显示对话框
 *
 * @example
 * jmaa.showDialog({
 *      title: '提示',
 *      init(dialog){
 *          dialog.body.html('这是一个对话框');
 *      },
 *      submit(dialog){
 *          console.log('提交');
 *          dialog.close();
 *      }
 * })
 */
jmaa.showDialog = function (opt) {
    return jmaa.create('JDialog', opt);
};
