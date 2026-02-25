package org.jmaa.base.utils;

import cn.hutool.core.io.FileUtil;
import org.jmaa.sdk.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.UUID;

/**
 * 本地上传文件
 *
 * @author eric
 */
public class LocalFileServiceImpl implements FileService {
    private final static Logger logger = LoggerFactory.getLogger(LocalFileServiceImpl.class);

    private UploadConfig.LocalConfig config;

    private static final String JOIN_STR = "-";

    public LocalFileServiceImpl(UploadConfig.LocalConfig config) {
        this.config = config;
    }

    @Override
    public String getStorage() {
        return "LOCAL";
    }

    /**
     * 上传文件
     *
     * @param entity 参数对象
     * @return result
     */
    @Override
    public String uploadFile(UploadFile entity) {
        String key = entity.getFolder() + "/" + Utils.format(new Date(), "yyyyMMdd") + "/" + UUID.randomUUID() + JOIN_STR + entity.getFileName();
        File file = FileUtil.writeFromStream(new ByteArrayInputStream(entity.getFileData()), config.getUploadPath() + "/" + key);
        return file.getAbsolutePath();
    }

    @Override
    public String uploadFileForFolder(UploadFile entity) {
        String key = entity.getFolder() + "/" + entity.getFileName();
        File file = FileUtil.writeFromStream(new ByteArrayInputStream(entity.getFileData()), config.getUploadPath() + "/" + key);
        return file.getAbsolutePath();
    }

    /**
     * 删除文件
     *
     * @param entity 参数对象
     */
    @Override
    public void deleteFile(UploadFile entity) {
        FileUtil.del(entity.getFilePath());
    }

    /**
     * 获取文件
     *
     * @param entity 参数对象
     * @return 文件流，可能为空
     */
    @Override
    public byte[] getFileData(UploadFile entity) {
        String path = entity.getFilePath();
        if (Utils.isNotEmpty(path)) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    return Files.readAllBytes(file.toPath());
                } catch (Exception e) {
                    logger.error("文件获取出错! + {}", entity);
                }
            }
        }
        return null;
    }
}
