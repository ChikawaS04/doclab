package com.doclab.doclab.service;

import com.doclab.doclab.model.Document;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "python.api.base=http://localhost:5000" // match WireMock
})
@SpringBootTest
class DocumentServiceIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private DocumentService documentService;

    private WireMockServer wireMockServer;

    @BeforeEach
    void startStub() {
        wireMockServer = new WireMockServer(options().port(5000));
        wireMockServer.start();
    }

    @AfterEach
    void stopStub() {
        if (wireMockServer != null) wireMockServer.stop();
    }

    @Test
    void process_success() throws Exception {
        String json = """
        {
          "summary":"This is a short summary extracted from the document.",
          "meta":{"docType":"resume","model":"mini-nlp"},
          "entities":[
            {"label":"NAME","text":"Jane Doe"},
            {"label":"EMAIL","text":"jane@example.com"}
          ]
        }
        """;
        wireMockServer.stubFor(post(urlEqualTo("/process")).willReturn(okJson(json)));

        Path tmp = Files.createTempFile("doclab-test-", ".pdf");
        Files.writeString(tmp, "dummy");

        Document doc = new Document();
        doc.setFileName("resume.pdf");
        doc.setFilePath(tmp.toString());

        documentService.process(doc);

        assertThat(doc.getStatus()).isEqualTo("PROCESSED");
        assertThat(doc.getLastError()).isNull();
        assertThat(doc.getSummaries()).isNotEmpty();
    }

    @Test
    void process_failure500() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/process"))
                .willReturn(serverError().withBody("Internal Error")));

        Path tmp = Files.createTempFile("doclab-test-", ".pdf");
        Files.writeString(tmp, "dummy");

        Document doc = new Document();
        doc.setFileName("resume.pdf");
        doc.setFilePath(tmp.toString());

        documentService.process(doc);

        assertThat(doc.getStatus()).isEqualTo("FAILED");
        assertThat(doc.getLastError()).isNotBlank();
    }
}
