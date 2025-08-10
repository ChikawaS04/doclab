package com.doclab.doclab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PythonNlpResponse {
    private String docType;          // "pdf" | "docx" | "txt"
    private List<NlpEntity> entities;
    private Integer pages;
    private NlpMeta meta;
    private String summary;          // always present (may be "")
}