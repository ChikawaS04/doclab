package com.doclab.doclab.service;

import com.doclab.doclab.dto.NlpEntity;
import com.doclab.doclab.dto.PythonNlpResponse;
import com.doclab.doclab.model.Document;
import com.doclab.doclab.model.ExtractedField;
import com.doclab.doclab.model.Summary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NlpMapperTest {

    private final NlpMapper mapper = new NlpMapper();

    @Test
    void maps_summary_and_fields() {
        // given
        Document d = new Document();
        d.setFileName("Davids Resume.pdf");

        PythonNlpResponse resp = new PythonNlpResponse();
        resp.setSummary("summary here");
        resp.setEntities(List.of(
                new NlpEntity("NAME", "David D."),
                new NlpEntity("EMAIL", "david@example.com")
        ));

        // when
        Summary s = mapper.toSummary(d, resp);
        List<ExtractedField> fields = mapper.toFields(d, resp);

        // then
        assertThat(s.getDocument()).isSameAs(d);
        assertThat(s.getTitle()).isEqualTo("Davids Resume");
        assertThat(s.getSummaryText()).isEqualTo("summary here");

        assertThat(fields).hasSize(2);
        assertThat(fields.get(0).getDocument()).isSameAs(d);
        assertThat(fields.get(0).getFieldName()).isEqualTo("NAME");
        assertThat(fields.get(0).getFieldValue()).isEqualTo("David D.");
        assertThat(fields.get(1).getFieldName()).isEqualTo("EMAIL");
    }

    @Test
    void title_falls_back_to_Untitled_when_no_filename() {
        Document d = new Document(); // fileName null
        PythonNlpResponse resp = new PythonNlpResponse();
        resp.setSummary("x");

        Summary s = mapper.toSummary(d, resp);
        assertThat(s.getTitle()).isEqualTo("Untitled");
    }
}
