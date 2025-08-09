package com.doclab.doclab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Document {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private java.util.UUID id;

    private String fileName;
    private String filePath;
    private String fileType;
    private String docType;
    private LocalDateTime uploadDate;
    private String status; // Pending, Processing, Completed, Failed
    private String titleGenerated;
    private boolean summaryGenerated;

    //Back-references
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedField> extractedFields = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Summary> summaries = new ArrayList<>();

    public List<Summary> getSummaries() { return summaries; }
    public void setSummaries(List<Summary> summaries) { this.summaries = summaries; }



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

    // Getters & setters(Alt + Insert in IntelliJ)
    public UUID getId() {return id; }
    public void setId(UUID id) {this.id = id; }

    public String getFileName() {return fileName; }
    public void setFileName(String fileName) {this.fileName = fileName; }

    public String getFilePath() {return filePath; }
    public void setFilePath(String filePath) {this.filePath = filePath; }

    public String getFileType() {return fileType; }
    public void setFileType(String fileType) {this.fileType = fileType; }

    public String getDocType() {return docType; }
    public void setDocType(String docType) {this.docType = docType; }

    public LocalDateTime getUploadDate() {return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) {this.uploadDate = uploadDate; }

    public String getStatus() {return status; }
    public void setStatus(String status) {this.status = status; }

    public String getTitleGenerated() {return titleGenerated; }
    public void setTitleGenerated(String titleGenerated) {this.titleGenerated = titleGenerated; }

    public boolean getSummaryGenerated() {return summaryGenerated; }
    public void setSummaryGenerated(boolean summaryGenerated) {this.summaryGenerated = summaryGenerated; }

    public List<ExtractedField> getExtractedFields() {return extractedFields; }
    public void setExtractedFields(List<ExtractedField> extractedFields) {this.extractedFields = extractedFields; }
}
