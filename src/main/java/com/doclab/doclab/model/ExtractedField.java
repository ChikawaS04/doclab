package com.doclab.doclab.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
@NoArgsConstructor
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

    public ExtractedField(Document document, String fieldName, String fieldValue, Integer pageNumber) {
        this.document = document;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.pageNumber = pageNumber;
        this.createdAt = LocalDateTime.now();
    }
}
