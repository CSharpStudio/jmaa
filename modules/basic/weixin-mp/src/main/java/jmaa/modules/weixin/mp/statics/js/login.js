//@ sourceURL=wx_login.js
$(function () {
    $(".wxmp-link").on('click', function () {
        $('.wxmp-qr').removeClass('d-none');
        return false;
    });
    $(document).on('click', function (e) {
        if (!$(e.target).closest('.wxmp-qr img').length) {
            $('.wxmp-qr').addClass('d-none');
        }
    });
})
