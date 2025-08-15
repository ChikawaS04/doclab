package com.doclab.doclab.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NlpMeta {
    // JSON field is "docType"
    @JsonProperty("docType")            // primary name
    @JsonAlias({"type"})                // accept "type" if Python sends that
    private String docType;

    private String model;

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
