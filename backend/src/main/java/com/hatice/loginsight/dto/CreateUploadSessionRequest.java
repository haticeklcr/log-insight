package com.hatice.loginsight.dto;

public class CreateUploadSessionRequest {

    private String fileName;
    private long fileSize;

    public CreateUploadSessionRequest() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}