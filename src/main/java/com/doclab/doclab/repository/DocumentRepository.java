package com.doclab.doclab.repository;

import com.doclab.doclab.model.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Detail view: fetch ONE collection eagerly to avoid MultipleBagFetchException
    @EntityGraph(attributePaths = {"summaries"})
    Optional<Document> findWithSummariesById(UUID id);

    // List/index view: newest first
    List<Document> findAllByOrderByUploadDateDesc();

    Page<Document> findByFileNameContainingIgnoreCaseOrDocTypeContainingIgnoreCase(
            String fileName, String docType, Pageable pageable
    );
}
