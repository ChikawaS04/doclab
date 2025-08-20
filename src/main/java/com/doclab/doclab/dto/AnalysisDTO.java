package com.doclab.doclab.dto;


import java.util.List;


public record AnalysisDTO(
        String title,
        SummaryDTO summary,
        List<ExtractedFieldDTO> fields
) {}