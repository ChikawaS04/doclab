package com.doclab.doclab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpEntity {
    private String label;
    private String text;

    public NlpEntity() {}
    public NlpEntity(String label, String text) { this.label = label; this.text = text; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}