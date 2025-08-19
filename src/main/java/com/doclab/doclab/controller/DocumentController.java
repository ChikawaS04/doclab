package com.doclab.doclab.controller;

import com.doclab.doclab.api.PageResponse;
import com.doclab.doclab.dto.DocumentDTO;
import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.service.DocumentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@Validated @ModelAttribute UploadRequest req) throws IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);

        Document saved = null;
        try {
            MultipartFile file = req.getFile();

            // 400
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is required.");
            }

            // 413
            if (file.getSize() > MAX_UPLOAD_BYTES) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("File too large. Max is 10MB.");
            }

            // filename normalize (keep if you want)
            String originalName = file.getOriginalFilename();
            String leaf = (originalName == null) ? "upload" : originalName.replace("\\", "/");
            int slash = leaf.lastIndexOf('/');
            if (slash >= 0) leaf = leaf.substring(slash + 1);
            if (leaf.length() > MAX_FILENAME_LEN) {
                leaf = leaf.substring(0, MAX_FILENAME_LEN);
            }

            // 415
            String mime = file.getContentType();
            if (mime == null || !ALLOWED_TYPES.contains(mime)) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("Only PDF, DOCX, and TXT are allowed.");
            }

            // docType guard (optional)
            String docType = req.getDocType();
            if (docType != null) {
                docType = docType.trim();
                if (docType.isEmpty()) docType = null;
                if (docType != null && docType.length() > MAX_DOCTYPE_LEN) {
                    docType = docType.substring(0, MAX_DOCTYPE_LEN);
                }
                req.setDocType(docType);
            }

            // save + process
            saved = documentService.save(req);
            documentService.process(saved);

            // 200 DTO
            DocumentDTO dto = DocumentDTO.from(saved);
            if ("PROCESSED".equalsIgnoreCase(dto.getStatus())) {
                log.info("Upload succeeded traceId={} docId={}", traceId, dto.getId());
            } else {
                log.warn("Upload finished with status={} traceId={} docId={}", dto.getStatus(), traceId, dto.getId());
            }
            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("Upload failed traceId={} docId={}", traceId, saved != null ? saved.getId() : "n/a", e);
            if (saved != null) {
                return ResponseEntity.ok(DocumentDTO.from(saved)); // post-save failure: still 200 with DTO
            }
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

    @GetMapping
    public ResponseEntity<PageResponse<DocumentDTO>> list(
            @PageableDefault(sort = "uploadDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.list2(pageable));
    }

}
