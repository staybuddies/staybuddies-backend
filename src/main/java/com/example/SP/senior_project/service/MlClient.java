package com.example.SP.senior_project.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class MlClient {

    private final RestTemplate mlRestTemplate;

    @Value("${ml.service.url}")           // e.g. http://192.168.1.20:8000
    private String mlBase;

    @Value("${ml.service.path:/analyze}") // must match FastAPI (default /analyze)
    private String mlPath;

    public AnalyzeResponse analyze(String frontAbsPath,
                                   String backAbsPath,
                                   String selfieAbsPath) {
        // Validate files BEFORE we make the HTTP call
        requireReadable("front", frontAbsPath);
        if (backAbsPath != null)   requireReadable("back", backAbsPath);
        if (selfieAbsPath != null) requireReadable("selfie", selfieAbsPath);

        String url = UriComponentsBuilder.fromHttpUrl(mlBase)
                .path(mlPath)
                .toUriString();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("front",  new FileSystemResource(frontAbsPath));
        if (backAbsPath != null)   body.add("back",   new FileSystemResource(backAbsPath));
        if (selfieAbsPath != null) body.add("selfie", new FileSystemResource(selfieAbsPath));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        log.info("ML call -> {} (front={}, back={}, selfie={})",
                url, frontAbsPath, backAbsPath, selfieAbsPath);

        try {
            ResponseEntity<AnalyzeResponse> resp = mlRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), AnalyzeResponse.class);

            return resp.getBody();
        } catch (RestClientException ex) {
            // You can map this to 502 in your controller
            log.error("ML call failed: {}", ex.toString(), ex);
            throw ex;
        }
    }

    private static void requireReadable(String tag, String p) {
        if (p == null) throw new IllegalArgumentException(tag + " image required");
        Path path = Path.of(p);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("File not readable (" + tag + "): " + p);
        }
    }

    public boolean ping() {
        var url = UriComponentsBuilder.fromHttpUrl(mlBase).path("/health").toUriString();
        try {
            var resp = mlRestTemplate.getForEntity(url, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // FastAPI also returns "debug", etc.
    public static class AnalyzeResponse {
        private boolean ok;           // getter: isOk()
        private Double score;

        @JsonProperty("student_id")
        private String studentId;     // getter: getStudentId()

        private String name;
        private String university;

        @JsonProperty("grad_year")
        private Integer gradYear;     // getter: getGradYear()

        private String note;
    }
}
