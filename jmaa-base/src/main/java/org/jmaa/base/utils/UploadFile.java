package org.jmaa.base.utils;

/**
 * 上传文件对象
 *
 * @author eric
 */
public class UploadFile {
    public UploadFile() { }

    public UploadFile(String filePath) {
        this.filePath = filePath;
    }

    public UploadFile(String fileName, String folder, byte[] fileData) {
        this.fileName = fileName;
        this.folder = folder;
        this.fileData = fileData;
    }

    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 存储路径
     */
    private String filePath;
    /**
     * 目录
     */
    private String folder;
    /**
     * 如果是db类型保存文件字节
     */
    private byte[] fileData;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileSize() {
        return fileData == null ? 0 : fileData.length;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
