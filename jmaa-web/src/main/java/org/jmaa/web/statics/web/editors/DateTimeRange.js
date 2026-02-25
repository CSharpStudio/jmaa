jmaa.editor('datetime-range', {
    extends: "editors.date-range",
    format: 'YYYY-MM-DD HH:mm:ss',
    timePicker: true,
});
jmaa.searchEditor('datetime-range', {
    extends: 'editors.datetime-range',
    getCriteria: function () {
        let me = this;
        if (this.dom.find('input').val()) {
            return ['&', [me.name, '>=', me.startDate], [me.name, '<=', me.endDate]];
        }
        return [];
    },
    getText: function () {
        let me = this,
            text = me.dom.find('input').val();
        if (text) {
            return [me.startDate + "至".t() + me.endDate];
        }
        return "";
    },
});
