jmaa.editor('media', {
    css: 'e-image',
    size: 10, //大小限制，单位M
    maxWidth: 100, //图片显示最大宽度
    maxHeight: 100, //图片显示最大高度
    imageType: /gif|jpg|jpeg|png|webp|jfif/i,
    compress: true,//是否开启图片压缩
    compressWidth: 1280, //压缩图片最大宽度
    compressHeight: 1024, //压缩图片最大高度
    quality: 1,//图片压缩系数0-1之间,大于1默认为1
    placeholder: '/web/org/jmaa/web/statics/img/placeholder.png',
    videoSrc: '/web/org/jmaa/web/statics/img/video.png',
    accept: 'image/*,video/*',
    getImageTpl(image) {
        let me = this;
        return `<li id="${image.itemId}" title="${image.name}" class="image-item" data-id="${image.id || ''}">
                    <img src="${image.src}" alt="${image.name}" style="max-width:${me.maxWidth}px; max-height:${me.maxHeight || me.maxWidth}px;">
                    <div class="hide tbar">
                        <a class="btn-delete"><span class="fa fa-trash"></span></a>
                        <a class="btn-edit"><span class="fa fa-pencil-alt"></span></a>
                        <a class="btn-preview"><span class="fa fa-search-plus"></span></a>
                    </div>
                </li>`;
    },
    getTpl() {
        let me = this;
        return `<div id="${this.getId()}">
                    <ul class="image-list">
                        <li class="image-placeholder">
                            <img class="placeholder" src="${me.placeholder}" alt="" style="max-width:${me.maxWidth}px; max-height:${me.maxHeight || me.maxWidth}px;min-width:80px; min-height:80px;">
                            <div class="hide tbar">
                                <a class="btn-edit"><span class="fa fa-pencil-alt"></span></a>
                            </div>
                        </li>
                    </ul>
                    <input type="file" style="display:none" name="file" accept="${me.accept}"${me.limit > 1 ? ' multiple' : ''}/>
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.size = me.nvl(eval(dom.attr('size')), me.size);
        me.compress = me.nvl(eval(dom.attr('compress')), me.compress);
        me.quality = me.nvl(eval(dom.attr('quality')), me.quality);
        me.maxWidth = me.nvl(eval(dom.attr('max-width') || dom.attr('width')), me.field.maxWidth, me.maxWidth);
        me.maxHeight = me.nvl(eval(dom.attr('max-height') || dom.attr('height')), me.field.maxHeight, me.maxHeight);
        me.compressWidth = me.nvl(eval(dom.attr('compress-width')), me.compressWidth);
        me.compressHeight = me.nvl(eval(dom.attr('compress-height')), me.compressHeight);
        me.limit = me.nvl(eval(dom.attr('limit')), me.limit, me.field.limit);
        me.dirtyValue = [];
        me.data = {};
        dom.html(me.getTpl()).on('mouseover', '.image-list li', function () {
            if (!me.readonly()) {
                $(this).find('.tbar').removeClass('hide');
            }
        }).on('mouseleave', '.image-list li', function () {
            $(this).find('.tbar').addClass('hide');
        }).on("click", '.image-list img', function () {
            if (!me.design && $(this).hasClass('placeholder') && !me.readonly()) {
                me.editId = $(this).closest('li').attr('id');
                dom.find('input[type=file]').click();
            }
        }).on("dblclick", '.image-list img', function () {
            if (!$(this).hasClass('placeholder') && !me.design) {
                me.preview($(this).closest('li'));
            }
        }).on("click", '.btn-edit', function () {
            if (!me.design) {
                me.editId = $(this).closest('li').attr('id');
                dom.find('input[type=file]').click();
            }
        }).on("click", '.btn-delete', function () {
            me.deleteItem($(this).closest('li'));
        }).on("click", '.btn-preview', function () {
            me.preview($(this).closest('li'));
        }).on('change', 'input', function () {
            me.open(this.files);
        });
    },
    open(files) {
        let me = this;
        let itemId = me.editId;
        for (let file of files) {
            let reader = new FileReader();
            reader.readAsDataURL(file);
            let image = new Image();
            let loadImage = function (data) {
                let item = {
                    itemId,
                    data,
                    src: file.type.startsWith('image') ? data : me.videoSrc,
                    name: file.name,
                    type: file.name.split('.').pop().toLowerCase(),
                }
                me.updateItem(item);
                if (itemId) {
                    let dataId = me.dom.find(`#${itemId}`).attr('data-id');
                    if (dataId) {
                        me.dirtyValue.push({delete: dataId});
                    }
                }
                me.dirtyValue.push(item);
                itemId = undefined;
            }
            reader.onload = function (e) {
                let data = e.target.result;
                //图片压缩
                if (me.compress && file.type.startsWith('image')) {
                    image.src = data;
                    image.onload = function () {
                        let canvas = document.createElement("canvas");
                        let ctx = canvas.getContext("2d");
                        let scale = 1;
                        let w = Math.max(image.width, image.height);
                        let h = Math.min(image.width, image.height);
                        if (w > me.compressWidth || h > me.compressHeight) {
                            scale = Math.min(me.compressWidth / w, me.compressHeight / h);
                        }
                        canvas.width = image.width * scale;
                        canvas.height = image.height * scale;
                        ctx.drawImage(this, 0, 0, canvas.width, canvas.height);
                        data = canvas.toDataURL(file.type, me.quality);
                        loadImage(data);
                    }
                } else {
                    let size = file.size;
                    if (size > 1024 * 1024 * me.size) {
                        let error = "上传文件不能超过".t() + me.size + "M";
                        return me.dom.next('.invalid-feedback').html(error).show();
                    }
                    loadImage(data);
                }
            };
        }
        me.dom.find('input').val('');
    },
    deleteItem(item) {
        let me = this;
        let dataId = item.attr('data-id');
        if (dataId) {
            me.dirtyValue.push({delete: dataId});
        }
        let id = item.attr('id');
        me.dirtyValue.remove(item => item.itemId == id);
        delete me.data[id];
        item.remove();
        me.dom.trigger('valueChange');
    },
    updateItem(item) {
        let me = this;
        if (item.itemId) {
            me.dom.find(`#${item.itemId} img`).attr({'src': item.src, alt: item.name});
        } else {
            item.itemId = `img-${jmaa.nextId()}`;
            let tpl = me.getImageTpl(item);
            me.dom.find('.image-placeholder').before(tpl);
        }
        me.data[item.itemId] = item;
        me.dom.trigger('valueChange');
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            if (me.dom.find('.image-item').length >= me.limit) {
                me.dom.find('.image-placeholder').hide();
            } else {
                me.dom.find('.image-placeholder').show();
            }
            handler(e, me);
        });
    },
    previewDialog(opt) {
        let me = this;
        jmaa.showDialog({
            title: '查看:'.t() + opt.name,
            id: 'preview-dialog',
            init(dialog) {
                opt.init(dialog);
            }
        });
    },
    preview(item) {
        let me = this;
        let id = item.attr('id');
        let file = me.data[id];
        let opt = {
            name: file.name
        };
        if (me.imageType.test(file.type)) {
            if (file.id || file.dataId) {
                opt.src = jmaa.web.getTenantPath() + `/attachment/${file.id || file.dataId}`;
            } else if (file.data) {
                opt.src = file.data.indexOf('base64') > -1 ? file.data : 'data:image/png;base64,' + file.data;
            }
            opt.init = dialog => {
                dialog.body.html(`<div class="e-image-preview"><img src="${opt.src}"/></div>`);
            };
            me.previewDialog(opt);
        } else {
            if (file.id || file.dataId) {
                opt.src = jmaa.web.getTenantPath() + `/attachment/${file.id || file.dataId}`;
            } else if (file.data) {
                let blob = Utils.createBlob(file.data, 'video/' + file.type);
                opt.src = Utils.createObjectURL(blob);
            }
            opt.init = dialog => {
                dialog.body.addClass('d-flex')
                dialog.body.html(`<video src="${opt.src}" controls style="margin:auto;max-width: 100%; height: 95%; object-fit: cover"></video>`);
                dialog.body.find('video')[0].play();
            };
            me.previewDialog(opt);
        }
    },
    valid() {
        let me = this;
        if (me.dom.find('.image-item').length > me.limit) {
            return '文件数量不能超过'.t() + me.limit;
        }
    },
    getDirtyValue() {
        let me = this;
        return me.dirtyValue;
    },
    getValue() {
        let me = this;
        return Object.values(me.data);
    },
    setValue(value) {
        let me = this;
        me.dirtyValue = [];
        me.data = {};
        me.dom.find('.image-item').remove();
        me.dom.find('.image-placeholder').show();
        if (value) {
            if (!Array.isArray(value)) {
                value = [value];
            }
            for (let item of value) {
                if (me.imageType.test(item.type)) {
                    if (item.id) {
                        item.src = jmaa.web.getTenantPath() + `/attachment/${item.id}`;
                    } else if (item.data) {
                        item.src = 'data:image/png;base64,' + item.data;
                    }
                } else {
                    item.src = me.videoSrc;
                }
                me.updateItem(item);
            }
        }
    },
});
jmaa.editor('image', {
    extends: 'editors.media',
    accept: 'image/*',
});
jmaa.editor('video', {
    extends: 'editors.media',
    accept: 'video/*',
});
