package com.doclab.doclab.dto;

import com.doclab.doclab.model.Document;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentListItemDTO {
    private UUID id;
    private String fileName;
    private String fileType;
    private String docType;
    private LocalDateTime uploadDate;
    private String status;

    public DocumentListItemDTO() {}

    private DocumentListItemDTO(UUID id, String fileName, String fileType, String docType,
                                LocalDateTime uploadDate, String status) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.docType = docType;
        this.uploadDate = uploadDate;
        this.status = status;
    }

    public static DocumentListItemDTO from(Document d) {
        return new DocumentListItemDTO(
                d.getId(),
                d.getFileName(),
                d.getFileType(),
                d.getDocType(),
                d.getUploadDate(),
                d.getStatus()
        );
    }

    // getters only (list view is read-only)
    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public String getDocType() { return docType; }
    public LocalDateTime getUploadDate() { return uploadDate; }
    public String getStatus() { return status; }
}
