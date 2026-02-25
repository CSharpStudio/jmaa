jmaa.column('boolean', {
    render() {
        return function (data) {
            if (data) {
                return '<label class="checked-column"><span>TRUE</span></label>';
            }
            return '<label class="unchecked-column"><span>FALSE</span></label>';
        }
    }
});
