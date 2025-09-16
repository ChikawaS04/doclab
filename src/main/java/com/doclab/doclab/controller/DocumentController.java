package com.doclab.doclab.controller;

import com.doclab.doclab.api.PageResponse;
import com.doclab.doclab.dto.DocumentDTO;
import com.doclab.doclab.dto.DocumentDetailDTO;
import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.service.DocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.doclab.doclab.util.UploadConstraints.ALLOWED_TYPES;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final long MAX_UPLOAD_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final int  MAX_FILENAME_LEN = 255;
    private static final int  MAX_DOCTYPE_LEN  = 64;

    private final DocumentService documentService;
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

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

            // filename normalize (optional)
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

            // 🔁 Re-fetch fresh state and return the rich detail DTO
            final UUID savedId = saved.getId();
            var detail = documentService.getDetail(savedId); // includes status, lastError, summaries, fields

            if ("PROCESSED".equalsIgnoreCase(detail.status())) {
                log.info("Upload succeeded traceId={} docId={}", traceId, detail.id());
            } else {
                log.warn("Upload finished with status={} traceId={} docId={} err={}",
                        detail.status(), traceId, detail.id(), detail.lastError());
            }
            return ResponseEntity.ok(detail);

        } catch (Exception e) {
            log.error("Upload failed traceId={} docId={}", traceId, saved != null ? saved.getId() : "n/a", e);
            if (saved != null) {
                // Post-save failure: still return lean DTO so client can retry or poll detail endpoint
                return ResponseEntity.ok(DocumentDTO.from(saved));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed. traceId=" + traceId);
        } finally {
            MDC.clear();
        }
    }


    // --- GET /api/documents/{id} (DETAIL) ---
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDTO> getDetail(@PathVariable UUID id) {
        DocumentDetailDTO detail = documentService.getDetail(id); // maps summaries + fields
        return ResponseEntity.ok(detail);
    }

    // --- GET /api/documents/{id}/download ---
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        File file = documentService.resolveFile(id);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        DocumentDetailDTO dto = documentService.getDetail(id);
        Resource resource = new FileSystemResource(file);

        // Use real MIME if available; fallback to octet-stream
        MediaType mediaType;
        try {
            String mt = java.nio.file.Files.probeContentType(file.toPath());
            mediaType = (mt != null ? MediaType.parseMediaType(mt) : MediaType.APPLICATION_OCTET_STREAM);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpHeaders headers = new HttpHeaders();
        // IMPORTANT: inline -> allows in-browser preview (<embed>)
        headers.setContentDisposition(
                ContentDisposition.inline().filename(dto.fileName(), java.nio.charset.StandardCharsets.UTF_8).build()
        );
        headers.setContentType(mediaType);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    // --- GET /api/documents (PAGED LIST) ---
    @GetMapping
    public ResponseEntity<PageResponse<DocumentDTO>> list(
            @PageableDefault(sort = "uploadDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "q", required = false) String q
    ) {
        return ResponseEntity.ok(documentService.list2(pageable, q));
    }

    // --- DELETE /api/documents/{id} ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
