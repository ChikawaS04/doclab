package com.doclab.doclab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;
    private String fileType;
    private String docType;

    private LocalDateTime uploadDate;

    private String status; // Pending, Processing, Completed, Failed

    private String titleGenerated;

    private boolean summaryGenerated;

    // Constructors
    public Document() {}

    public Document(String fileName, String filePath, String fileType, String docType,
                    String status, String titleGenerated, boolean summaryGenerated) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.docType = docType;
        this.uploadDate = LocalDateTime.now();
        this.status = status;
        this.titleGenerated = titleGenerated;
        this.summaryGenerated = summaryGenerated;
    }

    // Getters and Setters (Alt + Insert in IntelliJ)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitleGenerated() {
        return titleGenerated;
    }

    public void setTitleGenerated(String titleGenerated) {
        this.titleGenerated = titleGenerated;
    }

    public boolean isSummaryGenerated() {
        return summaryGenerated;
    }

    public void setSummaryGenerated(boolean summaryGenerated) {
        this.summaryGenerated = summaryGenerated;
    }
}
