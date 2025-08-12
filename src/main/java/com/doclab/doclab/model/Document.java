package com.doclab.doclab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

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
@Getter @Setter
@NoArgsConstructor // JPA needs this
@ToString(exclude = {"extractedFields", "summaries"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Document {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    @EqualsAndHashCode.Include
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

    @Column(nullable = false) // "UPLOADED","PROCESSING","PROCESSED","FAILED"
    private String status;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExtractedField> extractedFields = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Summary> summaries = new ArrayList<>();

    // Optional convenience ctor (keep if you use it)
    public Document(String fileName, String filePath, String fileType, String docType, String status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.docType = docType;
        this.status = status;
        this.uploadDate = LocalDateTime.now();
    }

    // Relationship helpers (keep as-is)
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
}
