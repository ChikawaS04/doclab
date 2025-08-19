package com.doclab.doclab.service;

import com.doclab.doclab.api.PageResponse;
import org.springframework.data.domain.Page;
import com.doclab.doclab.client.PythonApiClient;
import com.doclab.doclab.dto.DocumentDTO;
import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.repository.DocumentRepository;
import com.doclab.doclab.repository.ExtractedFieldRepository;
import com.doclab.doclab.repository.SummaryRepository;
import com.doclab.doclab.util.FileStorageUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.doclab.doclab.model.DocumentStatus.*;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageUtil fileStorageUtil;

    // NEW: Python client + repos
    private final PythonApiClient pythonApiClient;
    private final SummaryRepository summaryRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final NlpMapper nlpMapper;

    public DocumentService(
            DocumentRepository documentRepository,
            FileStorageUtil fileStorageUtil,
            PythonApiClient pythonApiClient,
            SummaryRepository summaryRepository,
            ExtractedFieldRepository extractedFieldRepository, NlpMapper nlpMapper
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
        // If you switched to enum, use: d.setStatus(DocumentStatus.UPLOADED);
        d.setStatus("UPLOADED");                  // string fallback

        return documentRepository.save(d);
    }

    /**
     * Calls Python /process and persists Summary + ExtractedFields for an already-saved Document.
     * Call this right after save(...) in your controller.
     */
    @Transactional
    public void process(Document document) {
        setStatus(document, PROCESSING);

        // Guard: file path must exist
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            document.setLastError("Missing file path for document");
            setStatus(document, FAILED);
            documentRepository.save(document);
            return;
        }

        var resource = new FileSystemResource(Path.of(document.getFilePath()));
        try {
            var resp = pythonApiClient.process(resource, document.getFileName());

            // --- Map using NlpMapper (title = filename without extension; summary fallback = "")
            var summary = nlpMapper.toSummary(document, resp);
            document.addSummary(summary);

            var fields = nlpMapper.toFields(document, resp);
            fields.forEach(document::addExtractedField);

            setStatus(document, PROCESSED);
            document.setLastError(null);
            documentRepository.save(document); // cascades children

        } catch (Exception ex) {
            String traceId = MDC.get("traceId"); // may be null outside controller
            String msg = (ex.getMessage() != null) ? ex.getMessage() : ex.getClass().getSimpleName();

            // keep within DB column (VARCHAR 2000)
            if (msg.length() > 1990) msg = msg.substring(0, 1990) + "...";

            setStatus(document, "FAILED");
            document.setLastError(msg);
            documentRepository.save(document);

            log.error("NLP processing failed traceId={} docId={} err={}", traceId, document.getId(), msg, ex);
            // MVP: do NOT rethrow so controller can return 200 w/ status=FAILED
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

    public PageResponse<DocumentDTO> list2(Pageable pageable) {
        Page<DocumentDTO> p = documentRepository.findAll(pageable).map(DocumentDTO::from);
        return new PageResponse<>(
                p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isLast()
        );
    }

    /*
    (Prep for Day 6)
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return documentRepository.findById(id);
    }
     */
}
