package com.doclab.doclab.dto;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public record DocumentDetailDTO(
        UUID id,
        String fileName,
        String fileType,
        String docType,
        LocalDateTime uploadDate,
        String status,
        String lastError,
        List<SummaryDTO> summaries,
        List<ExtractedFieldDTO> fields,
        boolean downloadable
) {}