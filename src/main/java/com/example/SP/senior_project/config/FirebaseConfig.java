package com.example.SP.senior_project.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
public class FirebaseConfig {

    /**
     * Option A: path to JSON file, e.g. file:/etc/keys/fb.json or /app/secret.json
     */
    @Value("${firebase.service-account:}")
    private String serviceAccountPathOrEmpty;

    /**
     * Option B: env or property containing the JSON itself
     */
    @Value("${firebase.credentials.json:}")
    private String inlineJsonOrEmpty;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials creds;
        if (!inlineJsonOrEmpty.isBlank()) {
            try (var in = new ByteArrayInputStream(inlineJsonOrEmpty.getBytes(StandardCharsets.UTF_8))) {
                creds = GoogleCredentials.fromStream(in);
            }
        } else if (!serviceAccountPathOrEmpty.isBlank()) {
            try (var in = new FileInputStream(serviceAccountPathOrEmpty.replaceFirst("^file:", ""))) {
                creds = GoogleCredentials.fromStream(in);
            }
        } else {
            // Fall back to ADC if present
            creds = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(creds)
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        return FirebaseMessaging.getInstance(app);
    }

    @GetMapping("/api/v1/notifications/health")
    public Map<String, Object> fbHealth() {
        return Map.of("firebase", "ok");
    }
}
