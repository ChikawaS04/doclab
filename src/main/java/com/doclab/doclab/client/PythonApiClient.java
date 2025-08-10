package com.doclab.doclab.client;

import com.doclab.doclab.dto.PythonNlpResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class PythonApiClient {

    private final WebClient nlpWebClient;

    public PythonApiClient(WebClient nlpWebClient) {
        this.nlpWebClient = nlpWebClient;
    }

    /**
     * Sends a file to Python /process and returns the parsed response.
     * This is a blocking call (MVP) so it can be used from MVC services.
     */
    public PythonNlpResponse process(Resource fileResource, String filename) {
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", fileResource)
                .filename(filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        try {
            return nlpWebClient.post()
                    .uri("/process")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(mb.build()))
                    .retrieve()
                    .onStatus(
                            status -> status.is5xxServerError() || status.is4xxClientError(),
                            resp -> resp.bodyToMono(String.class)
                                    .defaultIfEmpty("NLP error")
                                    .flatMap(msg -> Mono.error(new NlpServiceException("Python /process failed: " + msg)))
                    )
                    .bodyToMono(PythonNlpResponse.class)
                    .timeout(Duration.ofSeconds(8))
                    .block(); // block for simplicity in the MVC flow
        } catch (Exception e) {
            throw new NlpServiceException("Error calling Python /process", e);
        }
    }
}
