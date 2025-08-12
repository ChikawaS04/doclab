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
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    // --- POST /api/documents/upload ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@Validated @ModelAttribute UploadRequest req) throws IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);                    // <-- add traceId for the whole request
        try {
            MultipartFile file = req.getFile();
            // (your existing validations...)

            Document saved = documentService.save(req);
            documentService.process(saved);             // no signature change needed

            log.info("Upload processed ok traceId={} docId={}", traceId, saved.getId());
            return ResponseEntity.ok(DocumentDTO.from(saved));

        } catch (Exception e) {
            log.error("Upload failed traceId={}", traceId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed. traceId=" + traceId);
        } finally {
            MDC.clear();
        }
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
