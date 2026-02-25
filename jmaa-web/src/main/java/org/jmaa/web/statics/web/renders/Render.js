jmaa.renders = {};
jmaa.render = function (name, render) {
    jmaa.renders[name] = render;
};
jmaa.render('default', function (data) {
    if (data === null || data === undefined) {
        data = '';
    }
    return data;
});

jmaa.render('rownum', function (data) {
    if (data === null || data === undefined) {
        data = '';
    }
    return `<div style="margin-left:auto"><label>#${data}</label></div>`;
});

jmaa.render('rownum-check', function (data, row) {
    if (data === null || data === undefined) {
        data = '';
    }
    let key = jmaa.nextId();
    return `<div class="e-check rownum" style="margin-left:auto">
                <input id="l-${key}" type="checkbox" value="${row.id}">
                <label for="l-${key}">#${data}</label>
            </div>`;
});
