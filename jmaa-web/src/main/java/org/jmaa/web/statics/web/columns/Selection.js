jmaa.column('selection', {
    render() {
        let me = this;
        let tags = me.arch.attr('tags');
        if (tags) {
            tags = eval('(' + tags + ')');
        }
        let opt = me.arch.attr('options');
        if (opt) {
            me.options = eval("(" + opt + ")");
        } else {
            me.options = me.field.options || {};
        }

        return function (data) {
            let key = Array.isArray(data) ? data[0] : data;
            let text = me.options[key];
            if (text === undefined) {
                text = '';
            }
            if (tags) {
                let tag = tags[key] || tags['_'];
                return `<span class="tags bg-${tag}" data-value="${key}">${text}</span>`;
            }
            return `<span data-value="${key}">${text}</span>`;
        }
    }
});

jmaa.column('multi-selection', {
    render: function () {
        let me = this;
        let tags = me.arch.attr('tags');
        if (tags) {
            tags = eval('(' + tags + ')');
        }
        return function (data) {
            if (data === null || data === undefined) {
                return '';
            }
            let values = Array.isArray(data) ? data : data.split(',');
            let options = [];
            for (let key of values) {
                let text = me.field.options[key];
                if (text) {
                    if (tags) {
                        let tag = tags[key] || tags['_'];
                        options.push(`<span class="tags bg-${tag}" data-value="${key}">${text}</span>`);
                    } else {
                        options.push(text);
                    }
                }
            }
            if (tags) {
                return options.join('');
            }
            return `<span data-value="${data}">${options.join(', ')}</span>`;
        }
    }
})
