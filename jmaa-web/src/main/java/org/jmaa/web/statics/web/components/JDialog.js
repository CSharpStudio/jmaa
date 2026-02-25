/**
 * 对话框
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
     * @returns
     */
    getTpl() {
        return `<div class="modal fade" id="${this.id}">
                    <div class="modal-dialog ${this.sticky ? 'modal-sticky' : ''} ${this.css}">
                        <div class="modal-content ${this.bgClass}">
                        <div class="modal-header">
                            <h5 class="modal-title">${this.title}</h5>
                            <div class="modal-header-button">
                                <button class="dialog-top-button dialog-open" style="padding:.8rem 0.2rem .6rem 0.5rem;" role="btn-min">
                                     <i class="fa fa-chevron-down"></i>
                                 </button>
                            	${this.canMaximize ? '<button style="padding: .8rem 0.5rem .6rem 0.5rem" class="dialog-top-button" role="btn-max"><i class="fas fa-expand"></i></button>' : ''}
                            	<button type="button" style="padding: .8rem 1rem .6rem 0.2rem;margin-right: -10px;" class="dialog-top-button" data-dismiss="modal" aria-label="Close">
                            		<i class="fas fa-times"></i>
                            	</button>
							</div>
                        </div>
                        <div class="modal-body">
                            <p class="p-4">${this.loadingText}</p>\
                        </div>
                        <div class="modal-footer justify-content-between">
                            <div class="buttons-left">
                                <button type="button" class="btn btn-flat btn-default" data-dismiss="modal">${this.cancelText}</button>
                            </div>
                            <div class="buttons-right">
                                <button type="button" role="btn-submit" class="btn btn-flat btn-info">${this.submitText}</button>
                            </div>
                        </div>
                        </div>
                    </div>
                </div>`;
    },
    /**
     * 创建对话框实例
     *
     * @param {Object} opt 初始参数
     */
    __init__(opt) {
        const me = this;
        jmaa.utils.apply(true, me, opt);
        $('#' + me.id).remove();
        $(document.body).append(me.getTpl());
        me.dom = $('#' + me.id).modal({backdrop: false});
        me.body = me.dom.find('.modal-body');
        if (me.submit) {
            me.dom.on('click', '[role=btn-submit]', async function () {
                if (await me.submit(me)) {
                    me.dom.remove();
                }
            });
        } else {
            me.dom.find('[role=btn-submit]').hide();
        }
        me.init(me);
        $('#' + me.id).on('hide.bs.modal', function () {
            if (me.cancel) {
                me.cancel(me);
            }
            me.dom.remove();
        });
        if (this.canMaximize) {
            me.dom.on('click', '[role=btn-max]', function () {
                me.dom.find('.modal-dialog').toggleClass('modal-max');
            });
        }
        me.dom.on('click', '[role=btn-min]', function () {
            me.dom.find('.modal-dialog').toggleClass('dialog-close');
        }).on('click', '[t-click]', function (e) {
            const btn = $(this);
            if (btn.attr('disabled')) {
                return;
            }
            const click = btn.attr('t-click');
            const gap = eval(btn.attr('gap') || 500);
            btn.attr('disabled', true);
            btn.attr('clicking', "1");
            const fn = new Function('return this.' + click).call(me, e);
            if (fn instanceof Function) {
                fn.call(me, e, me);
            }
            setTimeout(function () {
                if (btn.attr("clicking") === "1") {
                    btn.attr('disabled', false);
                }
            }, gap);
        }).on('keyup', '[t-enter]', function (e) {
            if (e.keyCode == 13) {
                const enter = $(this).attr('t-enter');
                const fn = new Function('return this.' + enter).call(me, e);
                if (fn instanceof Function) {
                    fn.call(me, e, me);
                }
            }
        });
    },
    /**
     * 模板方法，初始化对话框内容的入口。
     */
    init: jmaa.emptyFn,
    /**
     * 更新对话框标题
     *
     * @param {String} title
     */
    updateTitle(title) {
        this.dom.find('.modal-title').html(title);
    },
    /**
     * 是否繁忙，繁忙时修改提交按钮为`请稍等`，并设置为禁用状态
     *
     * @param {Boolean} busy 是否繁忙
     */
    busy(busy) {
        if (busy) {
            this.dom.find('.buttons-right .btn').attr('disabled', true);
        } else {
            this.dom.find('.buttons-right .btn').attr('disabled', false);
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
 *      init: function(dialog){
 *          dialog.body.html('这是一个对话框');
 *      },
 *      submit: function(dialog){
 *          console.log('提交');
 *          dialog.close();
 *      }
 * })
 *
 * @param {Object} opt
 */
jmaa.showDialog = function (opt) {
    return jmaa.create('JDialog', opt);
};

/**
 * 显示遮罩信息
 *
 * @example
 * jmaa.mask('加载中');//显示
 * jmaa.mask();//移除
 * @param msg
 */
jmaa.mask = function (msg) {
    if (msg === undefined) {
        $('.loading-mask').remove();
    } else {
        $('body').append(`<div class='loading-mask'><div class="body">${msg}</div></div>`);
    }
};
