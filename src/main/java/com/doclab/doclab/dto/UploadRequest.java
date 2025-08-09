package com.doclab.doclab.dto;

import org.springframework.web.multipart.MultipartFile;

public class UploadRequest {
    private MultipartFile file;
    private String docType; // optional manual label

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
}
