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

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
public class UploadController {

    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "text/plain"
    );

    private final DocumentService documentService;

    public UploadController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Accepts multipart/form-data bound to UploadRequest (file + optional docType). */
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

        // 1) Save metadata + file to disk
        Document saved = documentService.save(req);

        // 2) Process via Python (synchronous MVP)
        documentService.process(saved);

        // 3) Return a DTO (adjust mapping if you already have a mapper)
        DocumentDTO dto = DocumentDTO.from(saved); // or map inline if you don't have this
        return ResponseEntity.ok(dto);
    }
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