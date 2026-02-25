jmaa.column('tree-column', {
    render() {
        return function (data, type, row) {
            return `<span style="margin-left: ${row.$depth * 30}px">${data}</span>`;
        }
    }
});
