package com.doclab.doclab.dto;

import com.doclab.doclab.model.Document;
import java.time.LocalDateTime;
import java.util.UUID;

public class DocumentDTO {
    private UUID id;
    private String fileName;
    private String filePath;
    private String fileType;
    private String docType;
    private LocalDateTime uploadDate;
    private String status;
    private String titleGenerated;
    private boolean summaryGenerated;

    public DocumentDTO() {}

    public DocumentDTO(Document d) {
        this.id = d.getId();
        this.fileName = d.getFileName();
        this.filePath = d.getFilePath();
        this.fileType = d.getFileType();
        this.docType = d.getDocType();
        this.uploadDate = d.getUploadDate();
        this.status = d.getStatus();
        this.titleGenerated = d.getTitleGenerated();
        this.summaryGenerated = d.getSummaryGenerated();
    }

    // Getters & setters(Alt + Insert in IntelliJ)

    public UUID getId() {return id; }
    public void setId(UUID id) {this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitleGenerated() { return titleGenerated; }
    public void setTitleGenerated(String titleGenerated) { this.titleGenerated = titleGenerated; }

    public boolean getSummaryGenerated() { return summaryGenerated; }
    public void setSummaryGenerated(boolean summaryGenerated) { this.summaryGenerated = summaryGenerated; }
}
