let render = function (data) {
    if (!data || !data.length) {
        data = '';
    }
    return data[1];
}
jmaa.render('selection', render);
jmaa.render('many2one', render);
