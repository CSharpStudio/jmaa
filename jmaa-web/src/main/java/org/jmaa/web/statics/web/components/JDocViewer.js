jmaa.component('JDocViewer', {
    // 附件
    fileList: [],
    // 预览时全屏方式 默认'' 不全屏 0 只显示全屏按钮 1 只默认全屏预览 2 默认全屏显示和可全屏缩小
    fullScreenType: 2,
    // 轮播图切换时长  0 就是不自动轮播
    interval: 60 * 1000,
    // 切换轮播图时 视频是否自动播放 0 不自动 1 自动
    videoAutoPlay: 0,
    // 切换轮播图时 视频是否循环播放 0 不循环 1 循环
    videoLoop: 0,
    // 切换轮播图时 视频是否播放完再切换 0 直接切换 1 播放完再切换
    videoChangePlay: 0,
    // 全屏模式下 是否拉伸媒体文件 0 不拉伸 1 拉伸
    autoFitMediaOfScreen: 0,
    // 是否显示当前文件名 0 不显示 1 显示
    showFileName: 0,
    // 没文件时显示的内容
    emptyText: '',
    // 以下参数不可设置 组件自用
    // 要预览文件类型
    typeMap: {
        image: 'gif|jpg|jpeg|png|webp',
        video: 'mp4|avi|mkv|mov|wmv|webm|ogg|flv',
        pdf: 'pdf',
        doc: 'docx',
        excel: 'xlsx',
    },
    // 文件缓存信息
    fileCacheMap: {},
    // 当前显示索引
    activeIndex: 0,
    // 切换时间
    currentTime: Date.now(),
    currentTimer: null,
    bolbTypeMap: {
        pdf: 'application/pdf',
        doc: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    },
    getFileListTpl() {
        // 指标点
        let indicatorsLiDom = '';
        // 每项信息
        let ItemDom = '';
        const display = this.fileList.length > 0 ? '' : 'none';
        if (this.fileList.length > 0) {
            this.fileList.forEach((file, index) => {
                const activeClass = index === this.activeIndex ? 'active' : '';
                indicatorsLiDom += `<li data-target="#previewCarousel" data-slide-to="${index}" class="${activeClass}"></li>`;
                ItemDom += `
                <div class="carousel-item h-100 ${activeClass}">
                    <div id="preview-view-${file.id}" class="preview-view-content ${this.showFileName ? 'show-file-name' : ''}">
                       <div class="loading">加载中...</div>
                    </div>
                    ${this.showFileName ? `<div class="preview-view-file-name">${file.fileName}</div>` : ''}
                </div>
                `;
            });
        } else {
            indicatorsLiDom += `<li data-target="#previewCarousel" data-slide-to="0" class="active"></li>`;
            ItemDom += `
             <div class="carousel-item h-100 active">
                <div id="preview-view-0" class="preview-view-content ${this.showFileName ? 'show-file-name' : ''}">
                   <div class="loading">${this.emptyText}</div>
                </div>
             </div>
            `;
        }

        return `
            <div id="previewCarousel" class="carousel slide h-100" data-ride="carousel">
              <ol class="carousel-indicators" style="display: ${display}">
                ${indicatorsLiDom}
              </ol>
              <div class="carousel-inner h-100">
                ${ItemDom}
              </div>
              <button class="carousel-control-prev" type="button" data-target="#previewCarousel" data-slide="prev" style="display: ${display}">
                <span class="carousel-control-prev-icon" aria-hidden="true"></span>
                <span class="sr-only">Previous</span>
              </button>
              <button class="carousel-control-next" type="button" data-target="#previewCarousel" data-slide="next" style="display: ${display}">
                <span class="carousel-control-next-icon" aria-hidden="true"></span>
                <span class="sr-only">Next</span>
              </button>
            </div>
        `;
    },
    getTpl() {
        return `
             <div class="preview-view-opacity preview-close ${[1, 2].includes(this.fullScreenType) ? 'fullscreen' : ''}">
                  <div id="preview-view" class="preview-view">
                      ${this.getFileListTpl()}
                  </div>
                   <div class="preview-view-button preview-close">
                   	${[0, 2].includes(this.fullScreenType) ? `<button class="fullscreen" role="btn-fullscreen"><i class="fas ${this.fullScreenType === 2 ? 'fa-compress' : 'fa-expand'}"></i></button>` : ''}
                   	<button type="button" class="close preview-close" role="btn-close" data-dismiss="modal" aria-label="Close">
                   		<span class="preview-close" aria-hidden="true">&times;</span>
                   	</button>
		    </div>
              </div>
        `;
    },
    init() {
        if (!this.dom) {
            this.dom = $(this.getTpl());
            // 点击弹窗内容 关闭弹窗
            const me = this;
            // 设置关闭事件
            this.dom.on('click', '[role=btn-close]', function (event) {
                me.exitFullscreen();
                me.remove();
            });
            // 设置全屏事件
            if ([0, 2].includes(this.fullScreenType)) {
                this.dom.on('click', '[role=btn-fullscreen]', function (event) {
                    event.stopPropagation();
                    event.preventDefault();
                    // 切换全部按钮
                    const className = me.dom.attr('class');
                    if (className.indexOf('fullscreen') > -1) {
                        me.dom.removeClass('fullscreen');
                        me.children[0].className = 'fa fa-expand';
                    } else {
                        me.dom.addClass('fullscreen');
                        me.children[0].className = 'fa fa-compress';
                    }
                    // 更新表格渲染
                    const activeIndex = me.activeIndex;
                    const fileInfo = me.fileList[activeIndex];
                    const previewInfo = me.fileCacheMap[fileInfo.id];
                    if (previewInfo.previewFileType === 'excel') {
                        setTimeout(function () {
                            // 重新刷新表格
                            previewInfo.previewObj.xs.sheet.reload();
                        }, 0);
                    }
                    // 处理拉伸适配
                    me.handleAutoFitMedia(me.dom, className.indexOf('fullscreen') > -1);
                });
            }
            $(window.parent.document.body).append(this.dom);
            this.initPreviewFile();
        } else {
            $(window.parent.document.body).append(this.dom);
            this.initPreviewFile();
        }
        // 打开浏览器全屏
        this.enterFullscreen();
    },
    // 新增预览文件
    initPreviewFile() {
        // 添加预览文件Dom
        this.dom.find('#preview-view').html(this.getFileListTpl());
        // 绑定相关事件
        this.initPreviewEvent();
        // 关联预览相关事件
        this.initPreviewView();
        // 异步后 初始化视频一些参数配置
        setTimeout(() => {
            this.handleAutoFitMedia(this.dom, [1, 2].includes(this.fullScreenType));
            const videoDom = this.dom.find('video');
            if (videoDom.length > 0) {
                videoDom.attr({
                    loop: Boolean(this.videoLoop),
                });
                if (this.videoAutoPlay) {
                    const currentVideoDom = this.dom.find('.carousel-item.active video');
                    if (currentVideoDom.length > 0) {
                        currentVideoDom[0].play();
                    }
                }
            }
        }, 0);
    },
    // 预览文件相关事件
    initPreviewEvent() {
        const previewViewOpacityDom = this.dom;
        const previewCarouselDom = previewViewOpacityDom.find('#previewCarousel');
        // 开启定时轮播
        this.handleCarousel(previewCarouselDom);
        const me = this;
        previewCarouselDom.off();
        // 监听轮播图滚动事件
        previewCarouselDom.on('slide.bs.carousel', function (event) {
            // 暂停所有视频
            const allVideoDom = previewCarouselDom.find('video');
            allVideoDom.each((index, videoDom) => {
                videoDom.pause();
            });
            // 设置自动播放 找到播放
            if (me.videoAutoPlay) {
                setTimeout(() => {
                    const children = event.relatedTarget.children;
                    if (children.length > 0 && children[0].tagName === 'VIDEO') {
                        children[0].play();
                    }
                }, 0);
            }
            // 更新轮播索引
            const activeIndex = event.to;
            me.activeIndex = activeIndex;
            // 获取文件预览信息
            const fileInfo = me.fileList[activeIndex];
            const previewInfo = me.fileCacheMap[fileInfo.id];
            if (previewInfo.previewFileType === 'excel') {
                setTimeout(function () {
                    // 重新刷新表格渲染 防止表格显示有问题
                    previewInfo.previewObj.xs.sheet.reload();
                }, 0);
            }
        });
    },
    // 定时轮播
    handleCarousel(previewCarouselDom) {
        clearTimeout(this.currentTimer);
        if (!this.interval) {
            return false;
        }
        this.currentTimer = setTimeout(() => {
            const nowTime = Date.now();
            if (nowTime - this.currentTime >= this.interval) {
                if (this.videoChangePlay) {
                    const currentVideoDom = previewCarouselDom.find('.carousel-item.active video');
                    if (currentVideoDom.length > 0 && currentVideoDom[0].paused === false) {
                        this.handleCarousel(previewCarouselDom);
                        return false;
                    }
                }
                this.currentTime = nowTime;
                previewCarouselDom.carousel('next');
            }
            this.handleCarousel(previewCarouselDom);
        }, 1000);
    },
    // 全屏模型下 视频是否宽高比自适应
    handleAutoFitMedia(previewViewOpacityDom, isFullscreen) {
        if (!this.autoFitMediaOfScreen) {
            return false;
        }
        setTimeout(() => {
            const allMediaDom = previewViewOpacityDom.find('video,img');
            allMediaDom.each((index, mediaDom) => {
                if (isFullscreen) {
                    const { videoWidth, videoHeight, offsetWidth, offsetHeight } = mediaDom;
                    const width = mediaDom.tagName === 'VIDEO' ? videoWidth : offsetWidth;
                    const height = mediaDom.tagName === 'VIDEO' ? videoHeight : offsetHeight;
                    mediaDom.style.width = width >= height ? '100%' : 'auto';
                    mediaDom.style.height = height >= width ? '100%' : 'auto';
                } else {
                    mediaDom.style.width = 'auto';
                    mediaDom.style.height = 'auto';
                }
            });
        }, 150);
    },
    // 关联预览文件视图 先序循环方式进行加载
    initPreviewView: async function () {
        const currentIndex = this.activeIndex;
        let loadFileNum = 0;
        let marginNum = 1;
        const totalNum = this.fileList.length;
        while (loadFileNum !== totalNum) {
            if (loadFileNum === 0) {
                await this.loadPreviewFile(currentIndex);
                loadFileNum++;
            }

            const leftNum = currentIndex - marginNum;
            const currentToLeft = leftNum > -1 ? leftNum : totalNum + leftNum;
            if (loadFileNum !== totalNum) {
                await this.loadPreviewFile(currentToLeft);
                loadFileNum++;
            }

            const rightNum = currentIndex + marginNum;
            const currentToRight = rightNum < totalNum ? rightNum : rightNum - totalNum;
            if (loadFileNum !== totalNum) {
                await this.loadPreviewFile(currentToRight);
                loadFileNum++;
            }
            marginNum++;
        }
    },
    // 文件加载逻辑
    loadPreviewFile: async function (fileIndex) {
        const previewViewOpacityDom = this.dom;
        const fileInfo = this.fileList[fileIndex];
        let previewInfo = this.fileCacheMap[fileInfo.id];
        // 没有缓存 计算后缓存
        if (!previewInfo) {
            const requestUrl = this.getFilePath(fileInfo);
            const typeMap = this.typeMap;
            let previewFileType;
            for (const typeKey in typeMap) {
                const currentTypeReg = new RegExp('(' + typeMap[typeKey] + ')$', 'i');
                if (currentTypeReg.test(fileInfo.type)) {
                    previewFileType = typeKey;
                }
            }
            previewInfo = {
                requestUrl,
                previewFileType,
                srcUrl: ['image', 'video'].includes(previewFileType) ? requestUrl : undefined,
            };
            this.fileCacheMap[fileInfo.id] = previewInfo;
        }
        // 没有文件缓存 请求后处理再缓存
        if (!previewInfo.srcUrl) {
            await fetch(previewInfo.requestUrl)
                .then((res) => {
                    return res.arrayBuffer();
                })
                .then((res) => {
                    const bolb = new Blob([res], { type: this.bolbTypeMap[previewInfo.previewFileType] });
                    previewInfo.srcUrl = URL.createObjectURL(bolb);
                });
        }
        // 加载文件
        const pageViewDom = previewViewOpacityDom.find('#preview-view-' + fileInfo.id);
        pageViewDom.html('');
        switch (previewInfo.previewFileType) {
            case 'image':
                pageViewDom.html(`<img src="${previewInfo.srcUrl}" alt="${fileInfo.fileName}" style="max-width: 100%; max-height: 100%; object-fit: cover" />`);
                break;
            case 'video':
                pageViewDom.html(`<video src="${previewInfo.srcUrl}" controls style="max-width: 100%; height: 95%; object-fit: cover" />`);
                break;
            case 'pdf':
                pageViewDom.html(`<iframe src="${previewInfo.srcUrl}" style="width: 100%; height: 100%;" />`);
                break;
            case 'doc':
                {
                    const jsPreviewDocx = window.jsPreviewDocx.init(pageViewDom[0], {
                        // 默认和文档样式类的类名/前缀
                        className: 'docx',
                        // 启用围绕文档内容渲染包装器
                        inWrapper: true,
                        // 禁止页面渲染宽度
                        ignoreWidth: false,
                        // 禁止页面渲染高度
                        ignoreHeight: false,
                        // 禁止字体渲染
                        ignoreFonts: false,
                        // 在分页符上启用分页
                        breakPages: true,
                        // 禁用lastRenderedPageBreak元素的分页
                        ignoreLastRenderedPageBreak: false,
                        // 启用实验性功能（制表符停止计算）
                        experimental: false,
                        // 如果为真，xml声明将在解析之前从xml文档中删除
                        trimXmlDeclaration: true,
                        // 如果为true，图像，字体等将转换为base 64 URL，否则为URL。使用了createObjectURL
                        useBase64URL: false,
                        // 包括用于chrome, edge等的MathML多边形。
                        useMathMLPolyfill: false,
                        // 支持实验性地呈现文档更改(插入/删除)
                        showChanges: false,
                        // 启用额外的日志记录
                        debug: false,
                    });
                    previewInfo.previewObj = jsPreviewDocx;
                    jsPreviewDocx.preview(previewInfo.srcUrl);
                }
                break;
            case 'excel':
                {
                    const jsPreviewExcel = window.jsPreviewExcel.init(pageViewDom[0], {
                        // excel最少渲染多少列
                        minColLength: 15,
                        // 在默认渲染的列表宽度上再加20px宽
                        widthOffset: 20,
                        // 在默认渲染的列表高度上再加20px高
                        heightOffset: 20,
                    });
                    previewInfo.previewObj = jsPreviewExcel;
                    jsPreviewExcel.preview(previewInfo.srcUrl);
                }
                break;
            default:
                break;
        }
    },
    // 清空预览文件
    removePreviewFile() {
        if (!this.dom) {
            return false;
        }
        this.dom.find('#preview-view').html('');
        // 删除预览对象 防止内存泄露
        for (const fileId in this.fileCacheMap) {
            const fileInfo = this.fileCacheMap[fileId];
            if (fileInfo.previewObj) {
                fileInfo.previewObj.destroy();
                delete fileInfo.previewObj;
            }
        }
    },
    // 关闭显示预览文件
    remove() {
        this.dom.remove();
    },
    // 设置数据源
    setValue(value) {
        this.fileList = [];
        if (Array.isArray(value) && value.length > 0) {
            this.activeIndex = 0;
            this.fileList = value;
            this.removePreviewFile();
            this.initPreviewFile();
        } else {
            this.removePreviewFile();
        }
    },
    // 浏览器全屏模式
    enterFullscreen() {
        const element = window.parent.document.documentElement; // 获取整个文档的根元素
        if (element.requestFullscreen) {
            element.requestFullscreen();
        } else if (element.mozRequestFullScreen) {
            element.mozRequestFullScreen();
        } else if (element.webkitRequestFullscreen) {
            element.webkitRequestFullscreen();
        } else if (element.msRequestFullscreen) {
            element.msRequestFullscreen();
        }
    },
    // 浏览器退出全屏模式
    exitFullscreen() {
        const parentDocDom = window.parent.document;
        if (parentDocDom.exitFullscreen) {
            parentDocDom.exitFullscreen();
        } else if (parentDocDom.mozCancelFullScreen) {
            parentDocDom.mozCancelFullScreen();
        } else if (parentDocDom.webkitExitFullscreen) {
            parentDocDom.webkitExitFullscreen();
        } else if (parentDocDom.msExitFullscreen) {
            parentDocDom.msExitFullscreen();
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
});
