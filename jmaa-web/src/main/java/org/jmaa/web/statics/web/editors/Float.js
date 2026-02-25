jmaa.editor('float', {
    extends: "editors.integer",
    getTpl() {
        let me = this;
        me.decimals = parseInt(me.dom.attr('decimals') || me.decimals || 2);
        return `<input type="number" id="${this.getId()}" data-decimals="${me.decimals}" focusable
                    ${!isNaN(me.min) ? ' min="' + me.min + '"' : ''}
                    ${!isNaN(me.max) ? ' max="' + me.max + '"' : ''}
                    ${!isNaN(me.step) ? ' step="' + me.step + '"' : ''}/>`;
    },
    parseValue(value) {
        let me = this;
        value = parseFloat(value);
        return isNaN(value) ? value : parseFloat(value.toFixed(me.decimals));
    },
    setAttr(attr, value) {
        let me = this;
        if (attr.startsWith('data-')) {
            const key = attr.slice(5)
            me[key] = value
        }
        me.dom.find('#' + me.getId()).attr(attr, value);
    }
});

jmaa.searchEditor('float', {
    extends: "editors.float",
    getCriteria() {
        let value = this.getValue();
        if (value !== null) {
            return [[this.name, '=', value]];
        }
        return [];
    },
    getText() {
        return this.getValue();
    },
});
