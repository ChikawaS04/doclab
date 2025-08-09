package com.doclab.doclab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "summary",
        indexes = {
                @Index(name = "idx_summary_document_id", columnList = "document_id")
        }
)
public class Summary {

    @Id
    @GeneratedValue
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, columnDefinition = "UUID")
    private Document document;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(name = "summary_text", nullable = false)
    private String summaryText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Summary() {}

    public Summary(Document document, String title, String summaryText) {
        this.document = document;
        this.title = title;
        this.summaryText = summaryText;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & setters(Alt + Insert in IntelliJ)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
