package com.doclab.doclab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpMeta {
    private String filename;
    private Long size;
}
