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
// --- POST /api/documents/upload ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@Validated @ModelAttribute UploadRequest req) throws IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        Document saved = null; // keep in outer scope so we can return DTO even if processing fails
        try {
            MultipartFile file = req.getFile();

            // Basic validations (same allowlist you already defined)
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is required.");
            }
            if (file.getContentType() == null || !ALLOWED.contains(file.getContentType())) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("Only PDF, DOCX, and TXT are allowed.");
            }

            // 1) Save file + Document
            saved = documentService.save(req);

            // 2) Trigger NLP (may set status to FAILED and lastError if it blows up)
            documentService.process(saved);

            // 3) Status-aware logging + DTO response
            DocumentDTO dto = DocumentDTO.from(saved);
            if ("PROCESSED".equalsIgnoreCase(dto.getStatus())) {
                log.info("Upload succeeded traceId={} docId={}", traceId, dto.getId());
            } else {
                log.warn("Upload finished with status={} traceId={} docId={}",
                        dto.getStatus(), traceId, dto.getId());
            }
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("Upload failed traceId={} docId={}", traceId, saved != null ? saved.getId() : "n/a", e);

            // If we at least created the Document row, return it (status likely FAILED) so the client has a payload
            if (saved != null) {
                return ResponseEntity.ok(DocumentDTO.from(saved));
            }
            // Otherwise, we failed before save()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed. traceId=" + traceId);
        } finally {
            MDC.clear();
        }
    }

    // --- GET /api/documents ---

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getById(@PathVariable UUID id) {
        return documentService.findById(id)
                .map(doc -> ResponseEntity.ok(DocumentDTO.from(doc)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentDTO>> list() {
        var docs = documentService.findAllSorted()
                .stream()
                .map(DocumentDTO::from)
                .toList();
        return ResponseEntity.ok(docs);
    }
    // (Optional) simple JSON error for uncaught exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Request failed: " + ex.getMessage());
    }
}
