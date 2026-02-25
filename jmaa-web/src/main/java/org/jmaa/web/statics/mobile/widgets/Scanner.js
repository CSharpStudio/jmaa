jmaa.widget('scanner', {
    init() {
        let me = this;
        me.dom.find('input').attr('name', 'scan-' + jmaa.nextId());
        me.dom.find('.scan-icon').html('<i/>');
        me.dom.addClass('scan-input').on('click', '.scan-icon', function () {
            me.scan();
            me.dom.find('input').focus();
        }).on('click', '.clear-input', function () {
            me.dom.find('input').val('').focus();
        });
    },
    getValue() {
        let me = this;
        return me.dom.find('input').val();
    },
    setValue(value) {
        let me = this;
        me.dom.find('input').val(value).trigger('change');
    },
    showMask() {
        let me = this;
        $('body').append(`<div id="scanner-mask" class="scan-mask">
                <div class="btn-close"></div>
                <div class="scan-line">
                    <div class="scan-line-out">
                        <div class="scan-line-in"></div>
                    </div>
                </div>
                <video id="video" width="100%" height="100%" class="scan-video" autoplay playsinline></video>
                <div id="sourceSelectPanel" style="display: none">
                    <label for="sourceSelect"></label>
                    <select id="sourceSelect"></select>
                </div>
            </div>`);
        return $('body').find('#scanner-mask');
    },
    scanByZXing() {
        let me = this;
        let mask = me.showMask();
        mask.on('click', '.btn-close', function () {
            codeReader.reset();
            mask.remove();
        });
        try {
            let constraints = {
                video: {
                    width: {ideal: 1280},  // 理想宽度
                    height: {ideal: 720},  // 理想高度
                    facingMode: "environment"  // 优先后置摄像头（"user"为前置）
                }
            };
            let getUserMedia = function (constraints, success, error) {
                if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                    navigator.mediaDevices.getUserMedia(constraints).then(success).catch(error);
                } else if (navigator.webkitGetUserMedia) { // Chrome < 50
                    navigator.webkitGetUserMedia(constraints, success, error);
                } else if (navigator.mozGetUserMedia) { // Firefox 旧版本
                    navigator.mozGetUserMedia(constraints, success, error);
                } else if (navigator.getUserMedia) { // 更旧的标准
                    navigator.getUserMedia(constraints, success, error);
                } else {
                    error(new Error('浏览器不支持摄像头访问'));
                }
            }
            getUserMedia(constraints, stream => {
                let video = $('.scan-mask #video')[0];
                video.srcObject = stream;
            }, err => {
                jmaa.msg.error('无法访问摄像头:' + err)
                console.error('无法访问摄像头:', err);  // 处理错误（如用户拒绝）
            });
        } catch (e) {
            jmaa.msg.error('发生错误:' + e)
        }
        let codeReader = new ZXing.BrowserMultiFormatReader();
        codeReader.decodeFromVideoDevice(undefined, 'video', (result, err) => {
            if (result) {
                codeReader.reset();
                mask.remove();
                me.dom.find('input').val(result.text).trigger({
                    type: 'keyup',
                    keyCode: 13,
                    which: 13 // 13是回车键的键码
                }).trigger('change');
            }
            if (err && !(err instanceof ZXing.NotFoundException)) {
                console.error(err);
            }
        });
    },
    scan() {
        let me = this;
        if (top.NativeJS) {
            let name = me.dom.find('input').attr('name');
            let iframe = "";
            if (window.frameElement) {
                if (!window.frameElement.id) {
                    window.frameElement.id = "iframe" + new Date().getTime();
                }
                iframe = window.frameElement.id;
            }
            top.NativeJS.scanBarcode(iframe + "#" + name);
        } else {
            me.scanByZXing();
        }
    }
});
$(function () {
    if (top.NativeJS) {
        window.onBarcodeScanHandle = function (key, result) {
            $(`.scan-input input[name='${key}']`).val(result).trigger({
                type: 'keyup',
                keyCode: 13,
                which: 13 // 13是回车键的键码
            }).trigger('change');
        }
        top.onBarcodeScan = function (key, result) {
            let keys = key.split('#');
            let w = window, iframe;
            if (keys[0] && (iframe = top.document.getElementById(keys[0]))) {
                w = iframe.contentWindow;
            }
            w.onBarcodeScanHandle && w.onBarcodeScanHandle(keys[1], result);
        }
    }
});
