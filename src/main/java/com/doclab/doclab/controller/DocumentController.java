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
import static com.doclab.doclab.util.UploadConstraints.ALLOWED_TYPES;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final long MAX_UPLOAD_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final int  MAX_FILENAME_LEN = 255;
    private static final int  MAX_DOCTYPE_LEN  = 64;

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

            // --- Basic presence check
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is required.");
            }

            // --- Size guard (413)
            if (file.getSize() > MAX_UPLOAD_BYTES) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("File too large. Max is 10MB.");
            }

            // --- Filename normalization & length guard
            // Only keep the leaf name (no path), and trim to safe length.
            String originalName = file.getOriginalFilename();
            String leaf = (originalName == null) ? "upload" : originalName.replace("\\", "/");
            int slash = leaf.lastIndexOf('/');
            if (slash >= 0) leaf = leaf.substring(slash + 1);
            if (leaf.length() > MAX_FILENAME_LEN) {
                leaf = leaf.substring(0, MAX_FILENAME_LEN);
            }

            // --- MIME allowlist (415)
            String mime = file.getContentType();
            if (mime == null || !ALLOWED_TYPES.contains(mime)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("Only PDF, DOCX, and TXT are allowed.");
            }

            // --- docType guard (optional field)
            String docType = req.getDocType();
            if (docType != null) {
                docType = docType.trim();
                if (docType.isEmpty()) docType = null;
                if (docType != null && docType.length() > MAX_DOCTYPE_LEN) {
                    docType = docType.substring(0, MAX_DOCTYPE_LEN);
                }
                // normalize back into the request so downstream save() persists the safe value
                req.setDocType(docType);
            }

            // (Optional) if you want to override the original filename we persist,
            // you can swap MultipartFile with a wrapper; for MVP we just persist
            // whatever MultipartFile reports, but we *do* use the normalized leaf
            // when we call Python (see process()).
            // 1) Save file + Document
            saved = documentService.save(req);

            // 2) Trigger NLP (may set status to FAILED and lastError if it blows up)
            //    Use the normalized leaf name for Python
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

            // If we at least created the Document row, return it (status likely FAILED)
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
                .map(doc -> {
                    DocumentDTO dto = DocumentDTO.from(doc);
                    // pre-wire a download link (endpoint will be added later)
                    String url = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                            .fromCurrentContextPath()
                            .path("/api/documents/{id}/download")
                            .buildAndExpand(doc.getId())
                            .toUriString();
                    dto.setDownloadUrl(url);
                    return ResponseEntity.ok(dto);
                })
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
