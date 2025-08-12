package com.doclab.doclab.controller;

import com.doclab.doclab.dto.DocumentDTO;
import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // --- POST /api/documents/upload ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@Validated @ModelAttribute UploadRequest req) throws IOException {
        MultipartFile file = req.getFile();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required.");
        }
        if (file.getContentType() == null || !ALLOWED.contains(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Only PDF, DOCX, and TXT are allowed.");
        }

        // 1) Save file + Document
        Document saved = documentService.save(req);

        // 2) Trigger NLP + persistence of results
        documentService.process(saved);  // sync MVP

        // 3) Return minimal, frontend-safe DTO
        return ResponseEntity.ok(DocumentDTO.from(saved));
    }

    // --- GET /api/documents ---
    @GetMapping
    public List<DocumentDTO> list() {
        // simple list for Phase 3; pagination can come in Phase 4
        return documentService.findAll()
                .stream()
                .map(DocumentDTO::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getById(@PathVariable UUID id) {
        return documentService.findById(id)
                .map(doc -> ResponseEntity.ok(DocumentDTO.from(doc)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // (Optional) simple JSON error for uncaught exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Request failed: " + ex.getMessage());
    }
}
