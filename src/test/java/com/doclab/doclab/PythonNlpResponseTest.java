package com.doclab.doclab;

import com.doclab.doclab.dto.PythonNlpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

class PythonNlpResponseTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test
    void deserialize_sample_success() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/nlp/sample-success.json")) {
            PythonNlpResponse resp = om.readValue(in, PythonNlpResponse.class);

            assertThat(resp).isNotNull();
            assertThat(resp.getSummary()).isEqualTo("This is a short summary extracted from the document.");
            assertThat(resp.getMeta()).isNotNull();
            assertThat(resp.getMeta().getDocType()).isEqualTo("resume");
            assertThat(resp.getEntities()).hasSize(2);
            assertThat(resp.getEntities().get(0).getLabel()).isEqualTo("NAME");
            assertThat(resp.getEntities().get(0).getText()).isEqualTo("Jane Doe");
        }
    }
}
