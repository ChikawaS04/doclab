package com.doclab.doclab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "summary",
        indexes = {
                @Index(name = "idx_summary_document_id", columnList = "document_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
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

    public Summary(Document document, String title, String summaryText) {
        this.document = document;
        this.title = title;
        this.summaryText = summaryText;
        this.createdAt = LocalDateTime.now();
    }
}
