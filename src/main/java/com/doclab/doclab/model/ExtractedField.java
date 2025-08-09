package com.doclab.doclab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "extracted_field",
        indexes = {
                @Index(name = "idx_extracted_field_document_id", columnList = "document_id"),
                @Index(name = "idx_extracted_field_field_name", columnList = "field_name")
        }
)
public class ExtractedField {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, columnDefinition = "UUID")
    private Document document;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Lob
    @Column(name = "field_value", nullable = false)
    private String fieldValue;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ExtractedField() {}

    public ExtractedField(Document document, String fieldName, String fieldValue, Integer pageNumber) {
        this.document = document;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.pageNumber = pageNumber;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
