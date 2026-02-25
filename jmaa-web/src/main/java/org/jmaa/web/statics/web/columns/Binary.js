jmaa.column('binary', {
    render() {
        return function (data) {
            if ($.isArray(data)) {
                let files = [];
                let path = jmaa.web.getTenantPath();
                for (let d of data) {
                    files.push(`<a href="${path}/attachment/${d.id}">${d.name} (${Utils.getFileSize(d.size)})</a>\r`);
                }
                return `<div style="white-space: pre-line">${files.join('')}</div>`;
            }
            return data;
        };
    },
});
