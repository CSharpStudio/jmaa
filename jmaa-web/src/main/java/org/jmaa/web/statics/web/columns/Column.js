jmaa.columns = {};
jmaa.column = function (name, define) {
    jmaa.columns[name] = jmaa.component('columns.' + name, define);
};
jmaa.column('default', {
    render() {
        let me = this;
        let type = me.field.type;
        return function (data) {
            if (data === null || data === undefined) {
                data = '';
            }
            if ((type === 'char' || type === 'text') && typeof data === 'string') {
                return `<span class="char char-column${data.length > 36 ? ' long-text' : ''}">${data}</span>`;
            }
            return data;
        };
    },
});
jmaa.column('text-column', {
    render: function () {
        let me = this;
        let type = me.field.type;
        return function (data) {
            if (data === null || data === undefined) {
                data = '';
            }
            if ((type === 'char' || type === 'text') && typeof data === 'string' && data.length > 36) {
                let escapedString = data.replace(/"/g, "'");
                return `<span style="display: none">${data}</span><input type='text' readonly="readonly" title="${data.replaceAll('"', "'")}" class="text-column" value="${escapedString}"/>`;
            }
            return data;
        };
    },
});
jmaa.column('integer', {
    render: function () {
        return function (data) {
            if (data === null || data === undefined) {
                data = '';
            }
            return `<div class="text-right">${data}</div>`;
        };
    },
});
jmaa.column('float', {
    extends: 'columns.integer'
});
