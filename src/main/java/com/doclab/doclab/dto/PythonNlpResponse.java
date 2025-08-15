package com.doclab.doclab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PythonNlpResponse {
    private String summary;
    private NlpMeta meta;
    private List<NlpEntity> entities;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public NlpMeta getMeta() { return meta; }
    public void setMeta(NlpMeta meta) { this.meta = meta; }

    public List<NlpEntity> getEntities() { return entities; }
    public void setEntities(List<NlpEntity> entities) { this.entities = entities; }        // always present (maybe "")
}