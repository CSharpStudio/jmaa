//@ sourceURL=res_lang.js
jmaa.view({
    openLang: function () {
        let me = this;
        if (top.window.app) {
            top.app.ws.openTab("语言".t(), jmaa.web.getTenantPath() + "/view#model=res.lang&views=grid,form");
        } else {
            window.open(jmaa.web.getTenantPath() + "/view#model=res.lang&views=grid,form");
        }
    }
});
