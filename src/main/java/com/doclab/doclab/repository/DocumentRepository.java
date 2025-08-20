package com.doclab.doclab.repository;

import com.doclab.doclab.model.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // For detail view: fetch summaries and fields with the document
    @EntityGraph(attributePaths = {"extractedFields", "summaries"})
    Optional<Document> findWithAllById(UUID id);

    // For list/index view: sort by newest first
    List<Document> findAllByOrderByUploadDateDesc();
}
