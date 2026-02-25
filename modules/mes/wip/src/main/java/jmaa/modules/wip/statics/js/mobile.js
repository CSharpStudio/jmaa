$(function () {
    let onDuty;
    let updateOnDuty = function (data) {
        let station = [];
        for (let row of data) {
            station.push(row.station_id[1]);
        }
        $('.user-on-duty').html(`<div style="overflow: hidden;max-width: 200px;white-space: nowrap;text-overflow: ellipsis;">${station.join(',')}</div>`);
    }
    $(document).on("pageshow", "#user", function (event) {
        jmaa.rpc({
            model: 'mfg.work_station_on_duty',
            method: 'loadOnDuty',
            args: {
                fields: ["station_id", "staff_id", "create_date"]
            },
            context: {
                usePresent: true,
            },
            onsuccess(r) {
                onDuty = r.data;
                updateOnDuty(onDuty);
            }
        });
    }).on('click', '[t-click=offDuty]', function () {
        let item = $(this);
        let id = item.attr('data-id');
        jmaa.rpc({
            model: 'mfg.work_station_on_duty',
            method: 'userOffDuty',
            args: {
                ids: [id]
            },
            onsuccess(r) {
                item.closest('.list-item').remove();
                onDuty.remove(item => item.id == id);
                jmaa.msg.show('操作成功'.t());
                updateOnDuty(onDuty);
            }
        });
    });
    $('.show-on-duty').click(function () {
        if (onDuty && onDuty.length) {
            jmaa.showDialog({
                title: '在岗'.t(),
                init(dialog) {
                    let html = [];
                    for (let row of onDuty) {
                        html.push(`<div class="list-item">
                                <div class="d-grid grid-template-columns-2">
                                    <div class="card-item grid-colspan-2 grid-rowspan-1">
                                        <span style="font-size:.875rem;font-weight:500;color:#7f7f7f;">${'员工：'.t()}</span>${row.staff_id[1]}
                                    </div>
                                    <div class="card-item grid-colspan-2 grid-rowspan-1">
                                        <span style="font-size:.875rem;font-weight:500;color:#7f7f7f;">${'上岗时间：'.t()}</span>${row.create_date}
                                    </div>
                                    <div style="position:absolute;right:20px">
                                        <button type="button" t-click="offDuty" data-id="${row.id}" class="ui-btn ui-mini">${'离岗'.t()}</button>
                                    </div>
                                </div>
                            </div>`);
                    }
                    dialog.body.css({
                        'background-color': '#f9f9f9',
                        'min-height': '50px'
                    }).html(`<div class="list-view">${html.join('')}</div>`);
                }
            });
        }
    })
});
