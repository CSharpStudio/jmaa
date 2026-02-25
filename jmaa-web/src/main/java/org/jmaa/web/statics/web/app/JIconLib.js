jmaa.component('JIconLib', {
    selector: '[data-widget=icon-lib]',
    lib: '/web/org/jmaa/web/statics/plugins/fontawesome-free/css/all.min.css?etag=5.15.3',
    getTpl() {
        return `<div class="modal fade" id="modal-icon">
                    <div class="modal-dialog modal-sticky modal-xl">
                        <div class="modal-content">
                        <div class="modal-header">
                            <h4 class="modal-title">${'图标库'.t()}</h4>
                            <button type="button" style="margin-top: -14px;margin-right: -10px;" class="dialog-top-button" data-dismiss="modal" aria-label="Close">
                                <i class="fas fa-times"></i>
                            </button>
                        </div>
                        <div class="modal-body p-3">
                        </div>
                        <div class="modal-footer justify-content-between">
                            <button type="button" class="btn btn-default" data-dismiss="modal">${'关闭'.t()}</button>
                        </div>
                        </div>
                    </div>
                </div>`;
    },
    init() {
        const me = this;
        $(document).on('click', me.selector, function () {
            me.show();
        });
    },
    show() {
        const me = this;
        console.log(12333);
        $('#modal-icon').remove();
        $(document.body).append(me.getTpl());
        const modal = $('#modal-icon');
        modal.modal({ backdrop: false });
        $('#modal-icon').on('hidden.bs.modal', function () {
            $(this).remove();
        });
        $.ajax({
            url: me.lib,
            type: 'GET',
            dataType: 'text',
            success(rs) {
                let html = '<div style="grid-template-columns: repeat(6, 1fr);display: grid;">';
                $.each(rs.match(/fa-[\w\-]+:before/gi), function () {
                    const n = this.replace(':before', '');
                    html += '<div class="mr-2"><i class="fa ' + n + ' mr-1"></i>' + n + '</div>';
                });
                html += '</div>';
                modal.find('.modal-body').html(html);
            },
            error(rs) {
                console.log(rs);
            },
        });
    },
});
