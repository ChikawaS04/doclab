package com.doclab.doclab.service;

import com.doclab.doclab.dto.PythonNlpResponse;
import com.doclab.doclab.dto.NlpEntity;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.model.ExtractedField;
import com.doclab.doclab.model.Summary;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NlpMapper {

    // ---------------------------
    // helpers
    // ---------------------------
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);
    private static final Pattern MONEY_RX   = Pattern.compile("\\$?\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?");
    private static final Pattern PERCENT_RX = Pattern.compile("\\d+(?:\\.\\d+)?\\s?%");

    private static String normalizeMoney(String s) {
        if (s == null) return null;
        Matcher m = MONEY_RX.matcher(s);
        if (m.find()) {
            String raw = m.group().replaceAll("[,$\\s]", "");
            try {
                double d = Double.parseDouble(raw);
                return USD.format(d);
            } catch (Exception ignored) {}
        }
        return s.startsWith("$") ? s : "$" + s.replaceAll("\\s+", "");
    }

    private static String normalizePercent(String s) {
        if (s == null) return null;
        Matcher m = PERCENT_RX.matcher(s);
        if (m.find()) return m.group().replaceAll("\\s+", "") + " per annum";
        if (s.matches("\\d+(?:\\.\\d+)?")) return s + "% per annum";
        return s;
    }

    // naive label scrapers from summary text like "Lender: Acme Bank."
    private static String extractAfterLabel(String text, String label) {
        if (text == null) return null;
        Pattern p = Pattern.compile("\\b" + Pattern.quote(label) + "\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String val = m.group(1).trim();
            int cut = Math.min(indexOrEnd(val, '\n'), indexOrEnd(val, '.'));
            return val.substring(0, cut).trim();
        }
        return null;
    }

    private static String extractByKeyphrase(String text, String key) {
        if (text == null) return null;
        Pattern p = Pattern.compile("\\b" + Pattern.quote(key) + "\\b\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String val = m.group(1).trim();
            int cut = Math.min(indexOrEnd(val, '\n'), indexOrEnd(val, '.'));
            return val.substring(0, cut).trim();
        }
        return null;
    }

    private static int indexOrEnd(String s, char ch) {
        int i = s.indexOf(ch);
        return i < 0 ? s.length() : i;
    }

    private static void putIfAbsent(Map<String, String> map, String key, String val) {
        if (val == null) return;
        map.putIfAbsent(key, val);
    }

    // ---------------------------
    // Summary (kept from your version)
    // ---------------------------
    public Summary toSummary(Document doc, PythonNlpResponse resp) {
        Summary s = new Summary();
        s.setDocument(doc);

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

        String summaryText = (resp == null || resp.getSummary() == null) ? "" : resp.getSummary();
        s.setSummaryText(summaryText);

        return s;
    }

    // ---------------------------
    // Fields enrichment (NEW)
    // ---------------------------
    public List<ExtractedField> toFields(Document doc, PythonNlpResponse resp) {
        List<ExtractedField> out = new ArrayList<>();
        if (resp == null) return out;

        // First, collect canonical labels → value (dedupe by keeping first)
        Map<String, String> byLabel = new LinkedHashMap<>();
        final String summary = resp.getSummary();

        // 1) Parse explicit summary lines if they exist
        putIfAbsent(byLabel, "LENDER",                  trimToNull(extractAfterLabel(summary, "Lender")));
        putIfAbsent(byLabel, "BORROWER",                trimToNull(extractAfterLabel(summary, "Borrower")));
        putIfAbsent(byLabel, "EFFECTIVE_DATE",          trimToNull(extractAfterLabel(summary, "Effective Date")));
        putIfAbsent(byLabel, "GOVERNING_LAW",           trimToNull(extractByKeyphrase(summary, "Governing Law")));
        putIfAbsent(byLabel, "GRACE_PERIOD",            trimToNull(extractByKeyphrase(summary, "Grace Period")));
        putIfAbsent(byLabel, "DEFAULT_NOTICE_PERIOD",   trimToNull(extractByKeyphrase(summary, "Default Notice Period")));
        putIfAbsent(byLabel, "LENDER_ADDRESS",          trimToNull(extractByKeyphrase(summary, "Lender Address")));
        putIfAbsent(byLabel, "BORROWER_LOCATION",       trimToNull(extractByKeyphrase(summary, "Borrower Location")));

        // 2) Heuristic classification from generic NER
        List<NlpEntity> entities = resp.getEntities();
        if (entities != null) {
            for (NlpEntity e : entities) {
                String ner = trimToNull(e.getLabel()); // e.g., ORG/GPE/DATE/MONEY/PERCENT/PERSON
                String txt = trimToNull(e.getText());
                if (ner == null || txt == null) continue;

                String candidate = null;
                switch (ner.toUpperCase(Locale.ROOT)) {
                    case "MONEY": {
                        String money = normalizeMoney(txt);
                        // Prefer explicit mentions from summary text to disambiguate
                        String s = summary == null ? "" : summary.toLowerCase(Locale.ROOT);
                        if (s.contains("late fee") && !byLabel.containsKey("LATE_FEE")) {
                            candidate = "LATE_FEE"; txt = money; break;
                        }
                        if (s.contains("maximum additional debt") && !byLabel.containsKey("MAXIMUM_ADDITIONAL_DEBT")) {
                            candidate = "MAXIMUM_ADDITIONAL_DEBT"; txt = money; break;
                        }
                        if (!byLabel.containsKey("PRINCIPAL_AMOUNT")) {
                            candidate = "PRINCIPAL_AMOUNT"; txt = money;
                        }
                        break;
                    }
                    case "PERCENT": {
                        candidate = "INTEREST_RATE";
                        txt = normalizePercent(txt);
                        break;
                    }
                    case "DATE": {
                        if (!byLabel.containsKey("EFFECTIVE_DATE")) {
                            candidate = "EFFECTIVE_DATE";
                        }
                        break;
                    }
                    case "GPE": {
                        if (!byLabel.containsKey("GOVERNING_LAW")) {
                            candidate = "GOVERNING_LAW";
                        }
                        break;
                    }
                    // You could attempt ORG/PERSON → LENDER/BORROWER guesses,
                    // but we already try to parse those explicitly from summary lines.
                    default:
                        break;
                }

                if (candidate != null) {
                    byLabel.putIfAbsent(candidate, txt);
                }
            }
        }

        // 3) Build ExtractedField entities
        for (var entry : byLabel.entrySet()) {
            String label = entry.getKey();
            String value = entry.getValue();
            if (value == null) continue;

            ExtractedField ef = new ExtractedField();
            ef.setDocument(doc);
            ef.setFieldName(label);
            ef.setFieldValue(value);
            ef.setPageNumber(null); // unknown in MVP
            out.add(ef);
        }

        return out;
    }
}
