//@ sourceURL=feeding_log.js
jmaa.column('qty-editor', {
    render() {
        return function (data) {
            if(data > 0){
                return `<div class='text-success text-right'>${data}</div>`;
            }
            return `<div class='text-danger text-right'>${data}</div>`;
        }
    }
});
