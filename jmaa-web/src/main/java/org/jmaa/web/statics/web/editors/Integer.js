jmaa.editor('integer', {
    css: 'e-number',
    getTpl() {
        let me = this;
        return `<input type="number" id="${me.getId()}"
                    ${!isNaN(me.min) ? ' min="' + me.min + '"' : ''}
                    ${!isNaN(me.max) ? ' max="' + me.max + '"' : ''}
                    ${!isNaN(me.step) ? ' step="' + me.step + '"' : ''}/>`;
    },
    decrementButton: '<i class="fa fa-minus"></i>',
    incrementButton: '<i class="fa fa-plus"></i>',
    wheelChange: false,
    init() {
        let me = this;
        let dom = me.dom;
        me.min = me.parseValue(me.nvl(dom.attr("min"), me.min, me.field.min));
        me.max = me.parseValue(me.nvl(dom.attr("max"), me.max, me.field.max));
        me.lt = me.parseValue(me.nvl(dom.attr("lt"), me.lt, me.field.lt));
        me.gt = me.parseValue(me.nvl(dom.attr("gt"), me.gt, me.field.gt));
        me.step = me.parseValue(me.nvl(dom.attr("step"), me.step));
        me.buttons = me.nvl(eval(dom.attr("buttons")), me.buttons, true);
        dom.html(me.getTpl());
        if (me.wheelChange) {
            dom.on('wheel', 'input', function (e) {
                me.onWheelChange(e);
                e.preventDefault();
            });
        }
        me.initSpinner(dom.find("input[type=number]"));
        if (!me.buttons) {
            dom.find('button').hide();
        }
    },
    onWheelChange(e) {
        let me = this;
        if (!me.readonly()) {
            let value = me.dom.find('input').val();
            value = me.parseValue(value);
            if (isNaN(value)) {
                return;
            }
            let step = isNaN(me.step) ? 1 : me.step;
            if (e.originalEvent.deltaY > 0 && (isNaN(me.min) || value > me.min)) {
                me.dom.find('input').val(value - step).trigger('change');
            } else if (e.originalEvent.deltaY < 0 && (isNaN(me.max) || value < me.max)) {
                me.dom.find('input').val(value + step).trigger('change');
            }
        }
    },
    initSpinner(el) {
        let me = this;
        el.inputSpinner({
            decrementButton: me.decrementButton,
            incrementButton: me.incrementButton,
            template: // the template of the input
                '<div>' +
                '<input type="text" inputmode="decimal" focusable style="text-align: ${textAlign}" class="form-control form-control-text-input"/>' +
                '<div class="input-suffix"><button class="btn btn-decrement" type="button">${decrementButton}</button>' +
                '<button class="btn btn-increment" type="button">${incrementButton}</button></div>' +
                '</div>'
        });
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('change', function (e) {
            handler(e, me);
        });
    },
    setReadonly(readonly) {
        this.dom.find('input').attr('disabled', readonly);
    },
    parseValue(value) {
        return parseInt(value);
    },
    getValue() {
        let me = this;
        let value = me.dom.find('input').val();
        value = me.parseValue(value);
        if (isNaN(value)) {
            return null;
        }
        if (!isNaN(me.min) && value < me.min) {
            return me.min;
        }
        if (!isNaN(me.max) && value > me.max) {
            return me.max;
        }
        return value;
    },
    setValue(value) {
        let me = this;
        value = me.parseValue(value);
        if (isNaN(value)) {
            value = null;
        }
        if (value !== this.getValue()) {
            me.dom.find('input').val(value === null ? '' : value);
            me.dom.trigger('change');
        }
    },
    valid() {
        const me = this;
        const val = this.getValue();
        if (val === null) {
            return;
        }
        if (!isNaN(me.lt) && val >= me.lt) {
            return '必须小于'.t() + me.lt;
        }
        if (!isNaN(me.gt) && val <= me.gt) {
            return '必须大于'.t() + me.gt;
        }
    },
});

jmaa.searchEditor('integer', {
    extends: "editors.integer",
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
