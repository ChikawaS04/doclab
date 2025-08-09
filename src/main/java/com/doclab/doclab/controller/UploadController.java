package com.doclab.doclab.controller;

import com.doclab.doclab.dto.DocumentDTO;
import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.repository.DocumentRepository;
import com.doclab.doclab.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class UploadController {

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public UploadController(DocumentService documentService, DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    // Accepts multipart/form-data with fields: file, docType (optional)
    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<DocumentDTO> upload(@ModelAttribute UploadRequest request) throws Exception {
        Document saved = documentService.save(request);
        return ResponseEntity.ok(new DocumentDTO(saved));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartError(MultipartException ex) {
        return ResponseEntity.badRequest().body("Upload error: " + ex.getMessage());
    }

    /*
    // Get all documents (will use pagination later)
    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAll() {
        List<DocumentDTO> docs = documentRepository.findAll()
                .stream()
                .map(DocumentDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(docs);
    }

    // Get a single document by ID
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getById(@PathVariable UUID id) {
        return documentRepository.findById(id)
                .map(doc -> ResponseEntity.ok(new DocumentDTO(doc)))
                .orElse(ResponseEntity.notFound().build());
    }
    */
}
