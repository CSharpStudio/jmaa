//@ sourceURL=tenant.js
jmaa.column('tenant_link', {
    render: function () {
        const me = this;
        return function (data, type, row) {
            let link = window.location.origin + "/" + data;
            return `<a href="${link}" target="_blank">${data?data:0}</a>`;
        };
    },
});
