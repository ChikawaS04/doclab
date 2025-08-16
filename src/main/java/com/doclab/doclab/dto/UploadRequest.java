package com.doclab.doclab.dto;

import jakarta.validation.constraints.NotNull;               // <-- add
import org.springframework.web.multipart.MultipartFile;

public class UploadRequest {

    @NotNull(message = "file is required")                  // <-- add
    private MultipartFile file;

    private String docType; // optional manual label

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
}
