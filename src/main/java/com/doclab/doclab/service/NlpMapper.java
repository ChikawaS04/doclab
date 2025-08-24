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

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public Summary toSummary(Document doc, PythonNlpResponse resp) {
        Summary s = new Summary();
        s.setDocument(doc);

        // --- Prefer Python-provided title, else fallback to filename base
        String title = null;
        if (resp != null) {
            title = trimToNull(resp.getTitle());
        }

        if (title == null) {
            String fileName = doc.getFileName() == null ? "" : doc.getFileName();
            String base = fileName.contains(".")
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;
            title = base.isBlank() ? "Untitled" : base;
        }
        s.setTitle(title);

        // --- Summary text (never null, allow empty string)
        String summaryText = (resp == null || resp.getSummary() == null)
                ? ""
                : resp.getSummary();
        s.setSummaryText(summaryText);

        return s;
    }

    public List<ExtractedField> toFields(Document doc, PythonNlpResponse resp) {
        List<ExtractedField> out = new ArrayList<>();
        if (resp == null || resp.getEntities() == null) return out;

        resp.getEntities().forEach(e -> {
            String name  = trimToNull(e.getLabel());
            String value = trimToNull(e.getText());
            if (name == null || value == null) {
                // skip bad/blank entities so DB NOT NULLs aren’t violated
                return;
            }
            ExtractedField ef = new ExtractedField();
            ef.setDocument(doc);
            ef.setFieldName(name);
            ef.setFieldValue(value);
            ef.setPageNumber(null); // unknown in MVP
            out.add(ef);
        });

        return out;
    }
}
