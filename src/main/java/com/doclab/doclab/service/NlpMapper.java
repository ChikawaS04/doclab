package com.doclab.doclab.service;

import com.doclab.doclab.dto.PythonNlpResponse;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.model.ExtractedField;
import com.doclab.doclab.model.Summary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NlpMapper {

    public Summary toSummary(Document doc, PythonNlpResponse resp) {
        Summary s = new Summary();
        s.setDocument(doc);

        String fileName = doc.getFileName() == null ? "" : doc.getFileName();
        String base = fileName.contains(".")
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;
        s.setTitle(base.isBlank() ? "Untitled" : base);

        s.setSummaryText(resp.getSummary() == null ? "" : resp.getSummary());
        return s;
    }

    public List<ExtractedField> toFields(Document doc, PythonNlpResponse resp) {
        List<ExtractedField> out = new ArrayList<>();
        if (resp.getEntities() == null) return out;

        resp.getEntities().forEach(e -> {
            ExtractedField ef = new ExtractedField();
            ef.setDocument(doc);
            ef.setFieldName(e.getLabel());
            ef.setFieldValue(e.getText());
            ef.setPageNumber(null); // unknown in MVP
            out.add(ef);
        });
        return out;
    }
}
