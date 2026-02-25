jmaa.component("JCompany", {
    init() {
        let me = this, companies = (jmaa.web.cookie('ctx_company_ids') || '').split(',');
        jmaa.rpc({
            model: "rbac.user",
            method: "getUserCompanies",
            args: {},
            onsuccess(r) {
                me.data = r.data;
                $.each(me.data, function () {
                    this.check = companies.indexOf(this.id) > -1;
                });
                me.initCompany();
            }
        });
        me.dom.next().on('click', '[role=button]', function () {
            let cid = $(this).attr('data-id');
            jmaa.rpc({
                model: "rbac.user",
                method: "updateUserCompany",
                args: {companyId: cid},
                onsuccess(r) {
                    $.each(me.data, function () {
                        let c = this;
                        if (c.main) {
                            c.check = false;
                        }
                        c.main = c.id === cid;
                    });
                    me.initCompany();
                    window.location.reload();
                }
            });
        });
        me.dom.next().on('change', '[role=select-company]', function () {
            let ckb = $(this), cid = ckb.attr('data-id');
            $.each(me.data, function () {
                let c = this;
                if (c.id == cid) {
                    c.check = ckb.is(':checked');
                }
            });
            me.updateCookies();
        });
    },
    updateCookies() {
        let me = this, companies = [];
        $.each(me.data, function () {
            let c = this;
            if (c.main) {
                jmaa.web.cookie('ctx_company', c.id);
                env.company = {id: c.id, name: c.present};
                localStorage.setItem('company_info', JSON.stringify(env.company));
                companies.splice(0, 0, c.id);
            } else if (c.check) {
                companies.push(c.id);
            }
        });
        jmaa.web.cookie('ctx_company_ids', companies.join());
    },
    initCompany() {
        let me = this, html = '';
        if (me.data.length > 0) {
            $.each(me.data, function () {
                let c = this;
                if (c.main) {
                    me.dom.html(c.present).attr('data-id', c.id);
                } else {
                    html += `<div class="dropdown-item d-flex p-0">
                        <div class="border-right border-info">
                            <span class="btn p-2">
                                <input type="checkbox" ${(c.check ? 'checked' : '')} data-id="${c.id}" role="select-company"/>
                            </span>
                        </div>
                        <div role="button" data-id="${c.id}" class="dropdown-item pl-2">${c.present}</div></div>`;
                }
            });
            if (html) {
                me.dom.next().html(html);
            } else {
                me.dom.next().hide();
            }
            me.dom.show();
        }
        me.updateCookies();
    }
});
