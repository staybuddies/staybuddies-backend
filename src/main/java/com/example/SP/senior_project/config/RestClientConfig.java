package com.example.SP.senior_project.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate mlRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        // was 20s – bump to 60s to avoid 502 on slow OCR jobs
        f.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
        return new RestTemplate(f);
    }
}
