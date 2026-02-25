jmaa.editor('datetime', {
    extends: "editors.date",
    format: 'YYYY-MM-DD HH:mm:ss',
    icon: 'fa-calendar',
});

jmaa.searchEditor('datetime', {
    extends: "editors.datetime",
    getCriteria() {
        const val = this.getValue();
        if (val) {
            return [[this.name, '=', val]];
        }
        return [];
    },
    getText() {
        return this.getValue();
    },
});
