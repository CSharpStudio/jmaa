jmaa.editor('binary', {
    css: 'e-binary',
    size: 10, // 大小 默认10mb
    async: true, // 异步上传
    previewType: {
        image: /gif|jpg|jpeg|png|webp|jfif/i,
        video: /mp4|avi|mkv|mov|wmv|webm|ogg|flv/i,
        pdf: /pdf/i,
        doc: /docx|xlsx/i,
    },
    getTpl() {
        let me = this;
        return `<div id="${this.getId()}">
                    <div class="file-list">
                        <button type="button" class="btn btn-primary btn-upload">
                            <i class="fa fa-upload"></i>
                        </button>
                    </div>
                    <input type="file" style="display:none" name="file"${me.limit > 1 ? ' multiple' : ''}/>
                </div>`;
    },
    getItemTpl(item) {
        let me = this;
        if (me.previewType.image.test(item.type) && (item.id || item.dataId || item.data)) {
            let src = '';
            if (item.id || item.dataId) {
                src = jmaa.web.getTenantPath() + `/attachment/${item.id || item.dataId}`;
            } else if (item.data) {
                src = item.data.indexOf('base64') > -1 ? item.data : 'data:image/png;base64,' + item.data;
            }
            return `<div class="file-item file-image" id="${item.itemId}" title="${item.name}" data-id="${item.id || ''}">
                    <img src="${src}">
                    <div class="tbar">
                        <a class="btn-delete"><span class="fa fa-trash"></span></a>
                        <a class="btn-download"><span class="fa fa-download"></span></a>
                        <a class="btn-preview"><span class="fa fa-eye"></span></a>
                    </div>
                </div>`;
        }
        return `<div class="file-item" id="${item.itemId}" title="${item.name}" data-id="${item.id || ''}">
                    <div class="icon ${item.type}${item.type.length > 3 ? ' sm-text' : ''}">${item.type}</div>
                    <div class="content">
                        <div class="header">${item.name}</div>
                        <div class="footer">
                            <div>
                                <span class="size">${Utils.getFileSize(item.size)}</span>
                                <div class="progress progress-xs">
                                    <span class="progress-bar"></span>
                                </div>
                            </div>
                            <div class="tbar">
                                <a class="btn-delete"><span class="fa fa-trash"></span></a>
                                <a class="btn-download"><span class="fa fa-download"></span></a>
                                ${me.canPreview(item.type) ? '<a class="btn-preview"><span class="fa fa-eye"></span></a>' : ''}
                            </div>
                        </div>
                    </div>
                </div>`;
    },
    init() {
        let me = this;
        let dom = me.dom;
        me.dirtyValue = [];
        me.data = {};
        me.size = me.nvl(eval(dom.attr('size')), me.size);
        me.limit = me.nvl(eval(dom.attr('limit')), me.limit, me.field.limit);
        me.attachment = me.field.attachment;
        me.async = me.attachment && me.nvl(!eval(dom.attr('sync')), me.async);
        dom.html(me.getTpl()).on("click", '.btn-upload', function () {
            if (!me.design) {
                dom.find('input[type=file]').click();
            }
        }).on("click", '.btn-download', function () {
            me.download($(this).closest('.file-item'));
        }).on("click", '.btn-delete', function () {
            me.deleteFile($(this).closest('.file-item'));
        }).on("click", '.btn-preview', function () {
            me.preview($(this).closest('.file-item'));
        }).on('change', 'input', function () {
            me.open(this.files);
        })
    },
    open(files) {
        let me = this;
        let tasks = [];
        for (let file of files) {
            let size = file.size;
            if (size > 1024 * 1024 * me.size) { //1M
                let error = "上传文件不能超过".t() + me.size + "M";
                me.dom.next('.invalid-feedback').html(error).show();
                return;
            }
            let item = {
                name: file.name,
                type: file.name.split('.').pop().toLowerCase(),
                size: file.size,
            }
            if (me.async) {
                me.addFile(item);
                me.dom.find(`#${item.itemId}`).addClass('loading');
                tasks.push(new Promise((resolve, reject) => {
                    let data = new FormData();
                    data.append('file', file);
                    if (me.owner && me.owner.dataId) {
                        data.append('res_id', me.owner.dataId);
                        data.append('res_model', me.owner.model);
                        data.append('res_field', me.name);
                    } else {
                        me.dirtyValue.push(item);
                    }
                    me.upload({
                        data,
                        progress: function (e) {
                            if (e.lengthComputable) {
                                let percent = ((e.loaded / e.total) * 100).toFixed(0);
                                me.dom.find(`#${item.itemId} .progress-bar`).css('width', percent + "%");
                            }
                        },
                        success: function (rs) {
                            if (!rs.error) {
                                resolve();
                                item.dataId = rs.result.id;
                                me.dom.find(`#${item.itemId}`).attr('data-id', item.dataId).removeClass('loading');
                                if (me.previewType.image.test(item.type)) {
                                    let tpl = me.getItemTpl(item);
                                    me.dom.find(`#${item.itemId}`).addClass('file-image').html($(tpl).html());
                                }
                            } else if (rs.error.code === 7100) {
                                top.window.location.reload();// 未授权，刷新转跳到登录
                            }
                        },
                        error: function (rs) {
                            reject();
                            jmaa.msg.error({data: {error: rs.responseText, status: rs.status}});
                        }
                    });
                }));
            } else {
                tasks.push(new Promise((resolve, reject) => {
                    let reader = new FileReader();
                    reader.readAsDataURL(file);
                    reader.onload = function (e) {
                        item.data = e.target.result;
                        me.addFile(item);
                        me.dirtyValue.push(item);
                        resolve();
                    };
                }));
            }
        }
        if (tasks.length) {
            me.loading = true;
            Promise.all(tasks).then((res) => {
                me.dom.find('input').val('');
                me.loading = false;
                me.dom.trigger('valueChange');
            });
        }
    },
    upload(options) {
        $.ajax({
            url: jmaa.web.getTenantPath() + '/attachment/upload',
            type: 'POST',
            data: options.data,
            processData: false,
            contentType: false,
            xhr: function () {
                // 获取ajaxSettings中的xhr对象，为它的upload属性绑定progress事件的处理函数
                let xhr = $.ajaxSettings.xhr();
                if (xhr.upload) {
                    xhr.upload.addEventListener('progress', options.progress, false);
                }
                return xhr;
            },
            success: options.success,
            error: options.error,
        });
    },
    download(item) {
        let me = this;
        let itemId = item.attr('id');
        let file = me.data[itemId];
        if (file.data) {
            let blob = Utils.createBlob(file.data);
            let link = document.createElement('a');
            link.href = Utils.createObjectURL(blob); // 创建下载的链接
            link.download = file.name; // 下载后文件名
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(link.href); // 释放掉blob对象
        } else if (file.id || file.dataId) {
            window.open(jmaa.web.getTenantPath() + '/attachment/' + (file.id || file.dataId));
        }
    },
    canPreview(type) {
        let me = this;
        for (let types of Object.values(me.previewType)) {
            if (types.test(type)) {
                return true;
            }
        }
        return false;
    },
    preview(item) {
        let me = this;
        let id = item.attr('id');
        let file = me.data[id];
        let opt = {
            name: file.name
        };
        let createIFrame = function (src) {
            let height = $(window).height() - 200;
            return `<iframe src="${src}" style="width: 100%; height: ${height}px;"></iframe>`
        }
        if (me.previewType.image.test(file.type)) {
            if (file.id || file.dataId) {
                opt.src = jmaa.web.getTenantPath() + `/attachment/${file.id || file.dataId}`;
            } else if (file.data) {
                opt.src = file.data.indexOf('base64') > -1 ? file.data : 'data:image/png;base64,' + file.data;
            }
            opt.init = dialog => {
                dialog.body.html(`<div class="e-image-preview"><img src="${opt.src}"/></div>`);
            };
            me.previewDialog(opt);
        } else if (me.previewType.pdf.test(file.type)) {
            opt.init = dialog => {
                if (file.id || file.dataId) {
                    fetch(jmaa.web.getTenantPath() + `/attachment/${file.id || file.dataId}`)
                        .then((res) => {
                            return res.arrayBuffer();
                        })
                        .then((buffer) => {
                            const blob = new Blob([buffer], {type: 'application/pdf'});
                            dialog.body.html(createIFrame(Utils.createObjectURL(blob)));
                        });
                } else if (file.data) {
                    let blob = Utils.createBlob(file.data, 'application/pdf');
                    dialog.body.html(createIFrame(Utils.createObjectURL(blob)));
                }
            };
            me.previewDialog(opt);
        } else if (me.previewType.video.test(file.type)) {
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
        } else if (me.previewType.doc.test(file.type)) {
            if (file.id || file.dataId) {
                opt.src = jmaa.web.getTenantPath() + `/attachment/${file.id || file.dataId}`;
            } else if (file.data) {
                let blob = Utils.createBlob(file.data);
                opt.src = Utils.createObjectURL(blob);
            }
            opt.init = dialog => {
                dialog.body.html(createIFrame(`/web/org/jmaa/web/statics/plugins/js-preview/viewer.html?type=${file.type}&src=${opt.src}`));
            };
            me.previewDialog(opt);
        }
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
    deleteFile(item) {
        let me = this;
        let dataId = item.attr('data-id');
        let itemId = item.attr('id');
        if (dataId) {
            me.dirtyValue.push({delete: dataId});
            me.dirtyValue.remove(item => item.id == dataId);
        } else {
            me.dirtyValue.remove(item => item.itemId == itemId);
        }
        delete me.data[itemId];
        item.remove();
        me.dom.trigger('valueChange');
    },
    addFile(item) {
        let me = this;
        item.itemId = `file-${jmaa.nextId()}`;
        me.data[item.itemId] = item;
        let tpl = me.getItemTpl(item);
        me.dom.find('.btn-upload').before(tpl);
        me.dom.trigger('valueChange');
    },
    onValueChange(handler) {
        let me = this;
        me.dom.on('valueChange', function (e) {
            if (Object.keys(me.data).length >= me.limit) {
                me.dom.find('.btn-upload').hide();
            } else {
                me.dom.find('.btn-upload').show();
            }
            handler(e, me);
        });
    },
    valid() {
        let me = this;
        if (Object.keys(me.data).length > me.limit) {
            return '文件数量不能超过'.t() + me.limit;
        }
        if (me.loading) {
            return '正在上传中，请稍等';
        }
    },
    setReadonly(readonly) {
        let me = this;
        if (readonly) {
            me.dom.find('.btn-upload').attr('disabled', true);
        } else {
            me.dom.find('.btn-upload').removeAttr('disabled');
        }
    },
    getDirtyValue() {
        let me = this;
        if (me.attachment) {
            return me.dirtyValue;
        }
        return me.getValue();
    },
    getValue() {
        let me = this;
        return Object.values(me.data);
    },
    setValue(value) {
        let me = this;
        me.dirtyValue = [];
        me.data = {};
        me.dom.find('.file-item').remove();
        me.dom.find('.btn-upload').show();
        if (value) {
            if (!Array.isArray(value)) {
                value = [value];
            }
            for (let item of value) {
                me.addFile(item);
            }
        }
    },
});
