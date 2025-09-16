package com.doclab.doclab.service;

import com.doclab.doclab.api.PageResponse;
import com.doclab.doclab.client.PythonApiClient;
import com.doclab.doclab.dto.*;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.model.ExtractedField;
import com.doclab.doclab.model.Summary;
import com.doclab.doclab.repository.DocumentRepository;
import com.doclab.doclab.repository.ExtractedFieldRepository;
import com.doclab.doclab.repository.SummaryRepository;
import com.doclab.doclab.util.FileStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.doclab.doclab.model.DocumentStatus.FAILED;
import static com.doclab.doclab.model.DocumentStatus.PROCESSED;
import static com.doclab.doclab.model.DocumentStatus.PROCESSING;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final FileStorageUtil fileStorageUtil;
    private final PythonApiClient pythonApiClient;
    private final SummaryRepository summaryRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final NlpMapper nlpMapper;

    // --- Explicit constructor (replaces @RequiredArgsConstructor) ---
    public DocumentService(
            DocumentRepository documentRepository,
            FileStorageUtil fileStorageUtil,
            PythonApiClient pythonApiClient,
            SummaryRepository summaryRepository,
            ExtractedFieldRepository extractedFieldRepository,
            NlpMapper nlpMapper
    ) {
        this.documentRepository = documentRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.pythonApiClient = pythonApiClient;
        this.summaryRepository = summaryRepository;
        this.extractedFieldRepository = extractedFieldRepository;
        this.nlpMapper = nlpMapper;
    }

    /** Saves the uploaded file + Document row only (no NLP). */
    public Document save(UploadRequest req) throws IOException {
        var file = req.getFile();
        String storedPath = fileStorageUtil.store(file); // should be absolute; if not, resolve to absolute later

        Document d = new Document();
        d.setFileName(file.getOriginalFilename());
        d.setFileType(file.getContentType());
        d.setFilePath(storedPath);
        d.setDocType(req.getDocType());           // may be null for now
        d.setUploadDate(LocalDateTime.now());
        d.setStatus("UPLOADED");                  // string fallback

        return documentRepository.save(d);
    }

    /**
     * Calls Python /process and persists Summary + ExtractedFields for an already-saved Document.
     * Call this right after save(...) in your controller.
     */
    @Transactional
    public void process(Document document) {
        // Always work with a managed entity
        final UUID id = document.getId();
        Document managed = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));

        log.info("PROC start id={} status={}", managed.getId(), managed.getStatus());

        // Status → PROCESSING
        setStatus(managed, PROCESSING);

        // Guard: file path must exist
        if (managed.getFilePath() == null || managed.getFilePath().isBlank()) {
            managed.setLastError("Missing file path for document");
            setStatus(managed, FAILED);
            documentRepository.save(managed);
            documentRepository.flush();
            log.info("PROC end (no path) id={} status={}", managed.getId(), managed.getStatus());
            return;
        }

        var resource = new FileSystemResource(Path.of(managed.getFilePath()));
        try {
            log.info("PROC call Python id={}", managed.getId());
            // Update document type with Python API detection result
            var resp = pythonApiClient.process(resource, managed.getFileName());
            if (resp.getMeta() != null && resp.getMeta().getDocType() != null) {
                managed.setDocType(resp.getMeta().getDocType());
            }

            var mappedSummary = nlpMapper.toSummary(managed, resp); // may be null
            var mappedFields  = nlpMapper.toFields(managed, resp);  // may be null

            String title = (mappedSummary != null ? mappedSummary.getTitle() : null);
            String text  = (mappedSummary != null ? mappedSummary.getSummaryText() : null);
            var triples  = (mappedFields == null ? List.<FieldTriple>of()
                    : mappedFields.stream()
                    .map(f -> new FieldTriple(f.getFieldName(), f.getFieldValue(), f.getPageNumber()))
                    .toList());

            saveAnalysis(managed.getId(), title, text, triples);

            // Finalize
            setStatus(managed, PROCESSED);
            managed.setLastError(null);
            documentRepository.save(managed);
            documentRepository.flush();

            log.info("PROC end (OK) id={} status={}", managed.getId(), managed.getStatus());

        } catch (Exception ex) {
            String traceId = MDC.get("traceId");
            String msg = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();
            if (msg.length() > 1990) msg = msg.substring(0, 1990) + "...";

            setStatus(managed, FAILED);
            managed.setLastError(msg);
            documentRepository.save(managed);
            documentRepository.flush();

            log.error("NLP processing failed traceId={} docId={} err={}", traceId, managed.getId(), msg, ex);
        }
    }

    /** Helper that supports either enum or String status fields. */
    private void setStatus(Document document, String statusName) {
        document.setStatus(statusName);   // Document.status is a String
        documentRepository.save(document);
    }

    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public Optional<Document> findById(UUID id) {
        return documentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Document> findAllSorted() {
        return documentRepository.findAllByOrderByUploadDateDesc();
    }

    public PageResponse<DocumentDTO> list2(Pageable pageable, String q) {
        if (q == null || q.isBlank()) {
            var p = documentRepository.findAll(pageable).map(DocumentDTO::from);
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isLast());
        }

        // If q is UUID → exact id match
        try {
            UUID id = UUID.fromString(q.trim());
            return documentRepository.findById(id)
                    .map(DocumentDTO::from)
                    .map(dto -> new PageResponse<>(List.of(dto), pageable.getPageNumber(), pageable.getPageSize(), 1, 1, true))
                    .orElseGet(() -> new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0, true));
        } catch (IllegalArgumentException ignore) {
            // not a UUID → fall through to name/type search
        }

        var p = documentRepository
                .findByFileNameContainingIgnoreCaseOrDocTypeContainingIgnoreCase(q, q, pageable)
                .map(DocumentDTO::from);

        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isLast());
    }


    @Transactional(readOnly = true)
    public DocumentDetailDTO getDetail(UUID id) {
        Document doc = documentRepository.findWithSummariesById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        return toDetailDTO(doc);
    }

    @Transactional(readOnly = true)
    public File resolveFile(UUID id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) return null;
        File f = new File(doc.getFilePath());
        return (f.exists() && f.isFile()) ? f : null;
    }

    private DocumentDetailDTO toDetailDTO(Document doc) {
        boolean downloadable = doc.getFilePath() != null && !doc.getFilePath().isBlank();

        List<SummaryDTO> summaries = (doc.getSummaries() == null) ? List.of() :
                doc.getSummaries().stream()
                        .map(s -> new SummaryDTO(s.getTitle(), s.getSummaryText()))
                        .collect(Collectors.toList());

        List<ExtractedFieldDTO> fields = (doc.getExtractedFields() == null) ? List.of() :
                doc.getExtractedFields().stream()
                        .map(f -> new ExtractedFieldDTO(f.getFieldName(), f.getFieldValue(), f.getPageNumber()))
                        .collect(Collectors.toList());

        return new DocumentDetailDTO(
                doc.getId(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getDocType(),
                doc.getUploadDate(),
                doc.getStatus(),
                doc.getLastError(),
                summaries,
                fields,
                downloadable
        );
    }

    // Optional helper input for extracted fields with page numbers
    public static record FieldTriple(String name, String value, Integer pageNumber) {}

    // ---- MAIN method: accepts title, summaryText, and fields with page numbers
    @Transactional
    public void saveAnalysis(UUID documentId, String title, String summaryText, List<FieldTriple> fields) {
        var doc = documentRepository.findWithSummariesById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Append a new Summary (always safe)
        var newSummary = new Summary(doc, title, summaryText);
        doc.addSummary(newSummary);

        // Replace fields
        var existing = new java.util.ArrayList<>(doc.getExtractedFields());
        for (var ef : existing) doc.removeExtractedField(ef);

        if (fields != null) {
            for (var f : fields) {
                String name  = (f.name()  == null ? null : f.name().trim());
                String value = (f.value() == null ? null : f.value().trim());
                if (name == null || name.isEmpty() || value == null || value.isEmpty()) {
                    log.warn("Skipping empty field for doc {} -> name='{}' value='{}'", documentId, name, value);
                    continue;
                }
                var ef = new ExtractedField(doc, name, value, f.pageNumber());
                doc.addExtractedField(ef);
            }
        }

        documentRepository.save(doc);
        documentRepository.flush();
    }

    @Transactional
    public void delete(UUID id) {
        // Remove summaries + fields (cascade should handle if JPA mappings are correct)
        documentRepository.deleteById(id);
    }

}
