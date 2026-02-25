package org.jmaa.base.utils;

import org.jmaa.sdk.Utils;
import org.jmaa.sdk.tools.PathUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


/**
 * @author 上传文件配置
 */
@Configuration
@ConfigurationProperties(prefix = "upload")
public class UploadConfig {
    /**
     * 上传类型 LOCAL MINIO BD
     */
    private String config;

    /**
     * oss配置
     */
    private OssConfig oss;

    /**
     * 腾讯云配置
     */
    private CosConfig cos;

    /**
     * 七牛云配置
     */
    private QncConfig qnc;

    /**
     * 本地配置
     */
    private LocalConfig local;

    /**
     * minio配置
     */
    private MinioConfig minio;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public OssConfig getOss() {
        return oss;
    }

    public void setOss(OssConfig oss) {
        this.oss = oss;
    }

    public CosConfig getCos() {
        return cos;
    }

    public void setCos(CosConfig cos) {
        this.cos = cos;
    }

    public QncConfig getQnc() {
        return qnc;
    }

    public void setQnc(QncConfig qnc) {
        this.qnc = qnc;
    }

    public LocalConfig getLocal() {
        return local;
    }

    public void setLocal(LocalConfig local) {
        this.local = local;
    }

    public MinioConfig getMinio() {
        return minio;
    }

    public void setMinio(MinioConfig minio) {
        this.minio = minio;
    }

    public static class OssConfig {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
        private String publicUrl;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }
    }

    public static class CosConfig {
        private String region;
        private String secretId;
        private String secretKey;
        private String bucketName;
        private String publicUrl;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }
    }

    public static class QncConfig {
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String publicUrl;
        private String privateUrl;

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }

        public String getPrivateUrl() {
            return privateUrl;
        }

        public void setPrivateUrl(String privateUrl) {
            this.privateUrl = privateUrl;
        }
    }

    public static class LocalConfig {
        private String uploadPath;
        private String publicUrl;

        public String getUploadPath() {
            if (Utils.isEmpty(uploadPath)) {
                uploadPath = PathUtils.combine(System.getProperty("user.dir"), "data/files");
            } else if (uploadPath.startsWith("./")) {
                uploadPath = PathUtils.combine(System.getProperty("user.dir"), uploadPath.substring(1));
            }
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }
    }

    public static class MinioConfig {
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String endpoint;
        private String publicUrl;

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public void setPublicUrl(String publicUrl) {
            this.publicUrl = publicUrl;
        }
    }
}
