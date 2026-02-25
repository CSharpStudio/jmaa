jmaa.render('boolean', function (data) {
    if (data === null || data === undefined) {
        data = '';
    }
    if (data) {
        return '<i class="checked-column"/>';
    }
    return '<i class="unchecked-column"/>';
});
