package org.jmaa.base.utils;


/**
 * 文件上传接口
 *
 * @author eric
 */
public interface FileService {

    /**
     * 获取存储方式
     */
    String getStorage();

    /**
     * 上传文件
     */
    String uploadFile(UploadFile entity);

    /**
     * 上传文件
     */
    String uploadFileForFolder(UploadFile entity);

    /**
     * 删除文件
     */
    void deleteFile(UploadFile entity);

    /**
     * 获取文件
     *
     * @return 文件流，可能为空
     */
    byte[] getFileData(UploadFile entity);
}
