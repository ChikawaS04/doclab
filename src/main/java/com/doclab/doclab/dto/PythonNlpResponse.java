package com.doclab.doclab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PythonNlpResponse {
    private String title;
    private String summary;
    private NlpMeta meta;
    private List<NlpEntity> entities;   // always present (maybe "")

    public PythonNlpResponse() {}

    public PythonNlpResponse(String summary, NlpMeta meta, List<NlpEntity> entities) {
        this.summary = summary;
        this.meta = meta;
        this.entities = entities;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public NlpMeta getMeta() { return meta; }
    public void setMeta(NlpMeta meta) { this.meta = meta; }

    public List<NlpEntity> getEntities() { return entities; }
    public void setEntities(List<NlpEntity> entities) { this.entities = entities; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
