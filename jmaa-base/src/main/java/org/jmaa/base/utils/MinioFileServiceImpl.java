package org.jmaa.base.utils;

import cn.hutool.core.io.IoUtil;
import org.jmaa.sdk.Utils;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.UUID;

/**
 * Minio服务器上传文件
 *
 * @author : eric
 **/
public class MinioFileServiceImpl implements FileService {
    private final static Logger logger = LoggerFactory.getLogger(MinioFileServiceImpl.class);

    private UploadConfig.MinioConfig config;

    private static final String JOIN_STR = "-";

    private MinioClient clint;

    public MinioFileServiceImpl(UploadConfig.MinioConfig config) {
        this.config = config;
        this.clint = MinioClient.builder().endpoint(config.getEndpoint()).credentials(config.getAccessKey(), config.getSecretKey()).build();
    }

    @Override
    public String getStorage() {
        return "MINIO";
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
        try {
            //判断存储桶是否已经存在，不存在的话创建
            boolean exists = clint.bucketExists(BucketExistsArgs.builder().bucket(config.getBucketName()).build());
            //如果存储桶不存在则要创建
            if (!exists) {
                clint.makeBucket(MakeBucketArgs.builder().bucket(config.getBucketName()).build());
            }
            clint.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(key)
                            .stream(new ByteArrayInputStream(entity.getFileData()), entity.getFileSize(), -1).build());
            return key;
        } catch (Exception e) {
            logger.error("文件上传服务器出错 + {}", key);
            return null;
        }
    }

    @Override
    public String uploadFileForFolder(UploadFile entity) {
        String key = entity.getFolder() + "/" + entity.getFileName();
        try {
            //判断存储桶是否已经存在，不存在的话创建
            boolean exists = clint.bucketExists(BucketExistsArgs.builder().bucket(config.getBucketName()).build());
            //如果存储桶不存在则要创建
            if (!exists) {
                clint.makeBucket(MakeBucketArgs.builder().bucket(config.getBucketName()).build());
            }
            clint.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucketName())
                            .object(key)
                            .stream(new ByteArrayInputStream(entity.getFileData()), entity.getFileSize(), -1).build());
            return key;
        } catch (Exception e) {
            logger.error("文件上传服务器出错 + {}", key);
            return null;
        }
    }

    /**
     * 删除文件
     *
     * @param entity 参数对象
     * @return result
     */
    @Override
    public void deleteFile(UploadFile entity) {
        String key = entity.getFilePath();
        try {
            clint.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(key)
                    .build());
        } catch (Exception e) {
            logger.error("文件删除出错! + {}", key);
        }
    }

    /**
     * 获取文件
     *
     * @param entity 参数对象
     * @return 文件流，可能为空
     */
    @Override
    public byte[] getFileData(UploadFile entity) {
        try {
            String key = entity.getFilePath();
            GetObjectResponse object = clint.getObject(GetObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(key)
                    .build());
            return IoUtil.readBytes(object);
        } catch (Exception e) {
            logger.error("文件获取出错! + {}", entity);
            return null;
        }
    }
}
