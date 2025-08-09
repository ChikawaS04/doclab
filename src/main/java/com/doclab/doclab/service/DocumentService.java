package com.doclab.doclab.service;

import com.doclab.doclab.dto.UploadRequest;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.repository.DocumentRepository;
import com.doclab.doclab.util.FileStorageUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileStorageUtil fileStorageUtil;

    public DocumentService(DocumentRepository documentRepository, FileStorageUtil fileStorageUtil) {
        this.documentRepository = documentRepository;
        this.fileStorageUtil = fileStorageUtil;
    }

    public Document save(UploadRequest req) throws IOException {
        var file = req.getFile();
        String storedPath = fileStorageUtil.store(file);

        Document d = new Document();
        d.setFileName(file.getOriginalFilename());
        d.setFileType(file.getContentType());
        d.setFilePath(storedPath);
        d.setDocType(req.getDocType());           // may be null for now
        d.setUploadDate(LocalDateTime.now());
        d.setStatus("Pending");                   // Option 1: simple
        d.setTitleGenerated(null);
        d.setSummaryGenerated(false);

        return documentRepository.save(d);
    }
}
