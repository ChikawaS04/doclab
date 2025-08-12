package com.doclab.doclab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "document",
        indexes = {
                @Index(name = "idx_document_status", columnList = "status"),
                @Index(name = "idx_document_upload_date", columnList = "uploadDate")
        }
)
public class Document {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)             // server path; never expose via DTO
    private String filePath;

    @Column(nullable = true)
    private String fileType;              // e.g., application/pdf

    @Column(nullable = true)
    private String docType;               // business type, optional

    @Column(nullable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();

    @Column(nullable = false)             // "UPLOADED","PROCESSING","PROCESSED","FAILED"
    private String status;

    // Back-references
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedField> extractedFields = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Summary> summaries = new ArrayList<>();

    public Document() {}

    public Document(String fileName, String filePath, String fileType, String docType, String status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.docType = docType;
        this.status = status;
        this.uploadDate = LocalDateTime.now();
    }

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public List<ExtractedField> getExtractedFields() { return extractedFields; }
    private void setExtractedFields(List<ExtractedField> extractedFields) { this.extractedFields = extractedFields; }


    public List<Summary> getSummaries() { return summaries; }
    private void setSummaries(List<Summary> summaries) { this.summaries = summaries; }

    public void addExtractedField(ExtractedField ef) {
        if (ef == null) return;
        extractedFields.add(ef);
        ef.setDocument(this);
    }

    public void removeExtractedField(ExtractedField ef) {
        if (ef == null) return;
        extractedFields.remove(ef);
        ef.setDocument(null); // orphanRemoval=true will delete on flush
    }

    public void addSummary(Summary s) {
        if (s == null) return;
        summaries.add(s);
        s.setDocument(this);
    }

    public void removeSummary(Summary s) {
        if (s == null) return;
        summaries.remove(s);
        s.setDocument(null);
    }
}
