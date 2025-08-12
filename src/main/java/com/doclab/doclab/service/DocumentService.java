package com.doclab.doclab.service;

import com.doclab.doclab.client.PythonApiClient;
import com.doclab.doclab.dto.PythonNlpResponse;
import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.model.DocumentStatus;
import com.doclab.doclab.model.ExtractedField;
import com.doclab.doclab.model.Summary;
import com.doclab.doclab.repository.DocumentRepository;
import com.doclab.doclab.repository.ExtractedFieldRepository;
import com.doclab.doclab.repository.SummaryRepository;
import com.doclab.doclab.util.FileStorageUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageUtil fileStorageUtil;

    // NEW: Python client + repos
    private final PythonApiClient pythonApiClient;
    private final SummaryRepository summaryRepository;
    private final ExtractedFieldRepository extractedFieldRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            FileStorageUtil fileStorageUtil,
            PythonApiClient pythonApiClient,
            SummaryRepository summaryRepository,
            ExtractedFieldRepository extractedFieldRepository
    ) {
        this.documentRepository = documentRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.pythonApiClient = pythonApiClient;
        this.summaryRepository = summaryRepository;
        this.extractedFieldRepository = extractedFieldRepository;
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
        setStatus(document, "PROCESSING");

        var resource = new FileSystemResource(Path.of(document.getFilePath()));
        try {
            var resp = pythonApiClient.process(resource, document.getFileName());

            // Summary (always present; empty string allowed)
            String text = resp.getSummary() == null ? "" : resp.getSummary();
            String inferredTitle = (document.getFileName() != null && !document.getFileName().isBlank())
                    ? document.getFileName() : "Untitled";

            Summary sum = new Summary();
            sum.setTitle(inferredTitle);
            sum.setSummaryText(text);
            document.addSummary(sum);  // <-- helper keeps both sides in sync

            // Extracted fields
            if (resp.getEntities() != null) {
                resp.getEntities().forEach(e -> {
                    ExtractedField ef = new ExtractedField();
                    ef.setFieldName(e.getLabel());
                    ef.setFieldValue(e.getText());
                    ef.setPageNumber(null); // for now
                    document.addExtractedField(ef); // <-- helper
                });
            }

            setStatus(document, "PROCESSED");
            documentRepository.save(document); // <-- one save; cascade persists children

        } catch (Exception ex) {
            setStatus(document, "FAILED");
            try { document.getClass().getMethod("setLastError", String.class).invoke(document, ex.getMessage()); }
            catch (Exception ignore) {}
            documentRepository.save(document);
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
}
