package com.doclab.doclab.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column
    private String fileType;

    @Column
    private String docType;

    @Column(nullable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();

    // "UPLOADED","PROCESSING","PROCESSED","FAILED"
    @Column(nullable = false)
    private String status;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedField> extractedFields = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Summary> summaries = new ArrayList<>();

    // --- Constructors ---
    public Document() {
        // JPA
    }

    // Optional convenience ctor
    public Document(String fileName, String filePath, String fileType, String docType, String status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.docType = docType;
        this.status = status;
        this.uploadDate = LocalDateTime.now();
    }

    // --- Relationship helpers ---
    public void addExtractedField(ExtractedField ef) {
        if (ef == null) return;
        extractedFields.add(ef);
        ef.setDocument(this);
    }

    public void removeExtractedField(ExtractedField ef) {
        if (ef == null) return;
        extractedFields.remove(ef);
        ef.setDocument(null);
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

    // --- Getters & Setters ---
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

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public List<ExtractedField> getExtractedFields() { return extractedFields; }
    public void setExtractedFields(List<ExtractedField> extractedFields) { this.extractedFields = extractedFields; }

    public List<Summary> getSummaries() { return summaries; }
    public void setSummaries(List<Summary> summaries) { this.summaries = summaries; }

    // --- equals & hashCode (by id) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;
        Document that = (Document) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    // --- toString (avoid collections to prevent recursion/huge logs) ---
    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", docType='" + docType + '\'' +
                ", uploadDate=" + uploadDate +
                ", status='" + status + '\'' +
                ", lastError=" + (lastError != null ? "'" + preview(lastError) + "'" : null) +
                '}';
    }

    private static String preview(String s) {
        return s.length() > 120 ? s.substring(0, 117) + "..." : s;
    }
}
