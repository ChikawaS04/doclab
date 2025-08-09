package com.doclab.doclab.repository;

import com.doclab.doclab.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SummaryRepository extends JpaRepository<Summary, UUID> {
    List<Summary> findByDocumentId(UUID documentId);
}
