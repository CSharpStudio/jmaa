//@ sourceURL=qsd_test_values_editor.js
jmaa.editor('qsd_test_values_editor', {
    getTpl: function () {
        return `<textarea id="${this.editorId}"></textarea>`;
    },
    init: function () {
        let me = this;
        // 设置属性
        me.editorId = `editor-${me.getId()}`;
        let dom = me.dom;
        let field = me.field;
        let parent = dom.parent();
        me.placeholder = dom.attr('placeholder');
        me.readOnly = eval(me.readOnly || dom.attr('readonly') || field.readonly);
        // 添加Dom
        me.dom.html(me.getTpl());
        // 处理相关事件
        me.codeMirrorEditor = CodeMirror.fromTextArea(document.getElementById(`${me.editorId}`), {
            lineNumbers: true,
            mode: 'text/plain',
            lineWrapping: true
        });
        // 验证方法外露
        me.validate = me.validateInput;
        // 覆盖原有方法
        let originalSetValue = me.codeMirrorEditor.setValue;
        me.codeMirrorEditor.setValue = function (value) {
            originalSetValue.call(this, value);
            this.refresh(); // 强制刷新编辑器状态
            me.validateInput();
        };
        me.codeMirrorEditor.on('change', me.validateInput);
        // 样式调整
        parent.css("height", "100%");
        //dom.css("height", "100%");
        let container = dom.find("#" + me.editorId);
        let codeMirror = container.parent().find(".CodeMirror-wrap");
        if (container.parents(".isMobile").length > 0) {
            codeMirror.css({
                "height": "100px",
            });
            codeMirror.find(".CodeMirror-scroll").css({
                "height": "100%",
                "max-height": "100px",
            });
        } else {
            codeMirror.css({
                "height": "340px",
            });
            codeMirror.css({
                "max-width": codeMirror.parent().width() + "px",
            }).find(".CodeMirror-scroll").css({
                "height": "100%",
                "max-height": "348px",
            });
        }

    },
    // 验证函数：验证输入格式
    validateText(text) {
        let me = this;
        text = text.trim();
        if (!text) {
            return false;
        }
        text = parseFloat(text)
        if (Number.isNaN(text)) {
            return false;
        }
        // 保证是数值
        if (!/^[-+]?\d*\.?\d+$/.test(text)) {
            return false;
        }
        if (me.owner.getData().limit_lower > text) {
            return false;
        }
        if (me.owner.getData().limit_upper < text) {
            return false;
        }
        return true;
    },
    validateInput: function () {
        let me = this;
        if (!me.codeMirrorEditor) {
            return;
        }
        let separators = /[,;\n]/;
        let total = 0, valid = 0, invalid = 0;

        me.codeMirrorEditor.getAllMarks().forEach(mark => mark.clear());
        let currentPos = {line: 0, ch: 0};
        let currentToken = '';

        for (let line = 0; line < me.codeMirrorEditor.lineCount(); line++) {
            let lineText = me.codeMirrorEditor.getLine(line);
            for (let ch = 0; ch <= lineText.length; ch++) {
                let char = lineText[ch];
                if (!char || separators.test(char)) {
                    if (currentToken) {
                        total++;
                        let isValid = me.validateText(currentToken);
                        if (isValid) {
                            valid++;
                        } else {
                            invalid++;
                            me.codeMirrorEditor.markText(
                                currentPos,
                                {line: line, ch: ch},
                                {className: 'error'}
                            );
                        }
                        currentToken = '';
                    }
                    currentPos = {line: line, ch: ch + 1};
                } else {
                    if (currentToken === '') {
                        currentPos = {line: line, ch: ch};
                    }
                    currentToken += char;
                }
            }
        }

    },
    // 值改变事件
    onValueChange: function (handler) {
        let me = this;
        // 若没有创建 富文本 编辑器 则不处理
        if (!me.codeMirrorEditor) {
            return false;
        }
        me.codeMirrorEditor.on('change', (delta, oldDelta, source) => {
            handler({delta, oldDelta, source}, me);
            me.validateInput();
            if (me.owner.view.updateResult) {
                me.owner.view.updateResult(null, me);
            }
        });
    },
    getValue: function () {
        let me = this;
        // 若没有创建 富文本 编辑器 则不处理
        if (!me.codeMirrorEditor) {
            return false;
        }
        // 获取当前编辑器所有文本(不带任何样式)
        return me.codeMirrorEditor.getValue();
    },
    setValue: function (v) {
        let me = this;
        me.codeMirrorEditor && me.codeMirrorEditor.setValue(v);
    },
    setReadonly: function (readOnly = false) {
        let me = this;
        if (!readOnly) {
            me.dom.css('border', '1px solid #5aaebb');
        } else {
            me.dom.css('border', '1px solid #ccc');
        }
        me.codeMirrorEditor && me.codeMirrorEditor.setOption("readOnly", readOnly);
    },
});
