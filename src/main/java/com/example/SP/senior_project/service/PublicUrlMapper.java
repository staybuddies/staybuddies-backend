package com.example.SP.senior_project.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PublicUrlMapper {

    @Value("${app.local.upload-dir:/Users/zwenyanwin/Downloads/senior-project}")
    private String uploadDir;

    @Value("${app.local.web-base:/files}")
    private String webBase;

    public String toPublicUrl(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (stored.startsWith("http://") || stored.startsWith("https://")) return stored;

        String base = webBase.endsWith("/") ? webBase.substring(0, webBase.length() - 1) : webBase;

        // If it looks like just a key/filename
        if (!stored.contains("/") && !stored.contains("\\")) {
            return base + "/" + stored;
        }

        // If it's an absolute path under uploadDir, relativize it
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path p = Paths.get(stored).toAbsolutePath().normalize();

        if (p.startsWith(root)) {
            String rel = root.relativize(p).toString().replace("\\", "/");
            return base + "/" + rel;
        }

        // Fallback: expose file name
        return base + "/" + p.getFileName().toString();
    }
}
