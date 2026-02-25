package org.jmaa.base.utils;

import org.jmaa.sdk.tools.SpringUtils;

/**
 * @author eric
 * 业务工厂类
 */
public class FileServiceFactory {
    public static FileService build() {
        UploadConfig uploadConfig = SpringUtils.getBean(UploadConfig.class);
        String type = uploadConfig.getConfig();
        return build(type, uploadConfig);
    }

    public static FileService build(UploadConfig uploadConfig) {
        return build(uploadConfig.getConfig(), uploadConfig);
    }

    static FileService build(String type, UploadConfig uploadConfig) {
        if ("MINIO".equals(type)) {
            return new MinioFileServiceImpl(uploadConfig.getMinio());
        }
        return new LocalFileServiceImpl(uploadConfig.getLocal());
    }

    public static FileService build(String type) {
        UploadConfig uploadConfig = SpringUtils.getBean(UploadConfig.class);
        return build(type, uploadConfig);
    }
}
