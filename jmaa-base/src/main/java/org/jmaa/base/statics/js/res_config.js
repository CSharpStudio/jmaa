//@ sourceURL=res_config.js
jmaa.view({
    onFormInit: function () {
        let me = this;
        me.urlHash.id = 'res-config';
        window.location.hash = $.param(me.urlHash);
    },
});
