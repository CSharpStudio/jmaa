jmaa.column('image', {
    showPreview(imgUrl) {
        const mask = `<div id='mask' style="width: 100%;height: 100%;background-color: gray; position: fixed; top: 0; opacity:1;z-index: 3999">
                    <img src='${imgUrl}' alt='加载失败' style='position:absolute;top:0;right:0;left:0;bottom:0;margin:auto;width: 44%'></img>
                <div>`;
        const maskDom = $('#mask');
        if (maskDom.length !== 0) {
            maskDom.css('display', 'block');
            $('img').attr('src', imgUrl);
        } else {
            $('html body').append(mask);
            $('#mask').on('click', function () {
                $(this).remove();
            });
        }
    },
    // 获取当前图片地址
    getFilePath(data) {
        if (data && typeof data === 'object') {
            if (!data.insert && data.id) {
                return jmaa.web.getTenantPath() + `/attachment/${data.id}`;
            } else if (data.content) {
                return data.content;
            } else if (data.file && typeof data.file === 'string') {
                if (!data.file.startsWith('data:image')) {
                    return 'data:image/png;base64,' + data.file;
                } else {
                    return data.file;
                }
            }
            return data;
        } else if (data && !data.startsWith('data:image')) {
            return 'data:image/png;base64,' + data;
        }
        return data;
    },
    render() {
        const me = this;
        me.owner.dom.on('click', '.column-image', function () {
            const img = $(this);
            me.showPreview(img.attr('src'));
        });
        return function (data, type, row) {
            let html = '';
            if(data){
                const imgArr = Array.isArray(data) ? data : [data];
                for (const item of imgArr) {
                    if(item.delete || item.isDelete){
                        continue;
                    }
                    const imgUrl = me.getFilePath(item);
                    html += `<img class="column-image" style="max-width: 40px;max-height: 40px" src="${imgUrl}">`;
                }
            }
            return html;
        };
    },
});
