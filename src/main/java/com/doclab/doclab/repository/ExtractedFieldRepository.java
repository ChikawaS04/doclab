package com.doclab.doclab.repository;

import com.doclab.doclab.model.ExtractedField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExtractedFieldRepository extends JpaRepository<ExtractedField, UUID> {
    List<ExtractedField> findByDocumentId(UUID documentId);
    Optional<ExtractedField> findByDocumentIdAndFieldName(UUID documentId, String fieldName);
}
