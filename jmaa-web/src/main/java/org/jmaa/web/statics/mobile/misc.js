$.fn.Toasts = function (_, opt) {
    $('body #toast').remove();
    let html = `<div id="toast" class="jui-toast ${opt.class}">
                <i class="toast-icon ${opt.icon}"><!----></i>
                <div class="toast-body">${opt.body}</div>
              </div>`;
    $('body').append(html);
    let to;
    $('body #toast').click(function () {
        clearTimeout(to);
        $('body #toast').remove();
    });
    //TODO 播放声音
    if (opt.autohide) {
        to = setTimeout(function () {
            $('body #toast').remove();
        }, opt.delay || 1000);
    }
};
$(function () {
    $(document).on('click', '[data-toggle=dropdown]', function () {
        $(this).next('.dropdown-menu').addClass('show');
    }).on('click', '.dropdown-menu', function(){
        $(this).removeClass('show');
    });
    $(document).on('mousedown', function (e) {
        if ($(e.target).closest('.dropdown-menu').length == 0) {
            $('.dropdown-menu').removeClass('show');
        }
    });
    jmaa.mask = function (msg) {
        if (msg === undefined) {
            $('.loading-mask').remove();
        } else {
            $('body').append(`<div class='loading-mask'><div class="body">${msg}</div></div>`);
        }
    };
})
