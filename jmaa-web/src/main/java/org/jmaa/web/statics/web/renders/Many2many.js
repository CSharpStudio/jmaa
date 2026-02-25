jmaa.render('many2many', function (data) {
    if (data && data.length) {
        let value = [];
        for(let d of data){
            value.push(d[1]);
        }
        return value.join(',');
    }
    return '';
});
