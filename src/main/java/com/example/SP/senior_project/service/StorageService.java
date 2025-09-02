package com.example.SP.senior_project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String save(String side, MultipartFile file, long verificationId) throws IOException {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String ext = sanitizeExt(file.getOriginalFilename());
        String name = verificationId + "_" + side + "_" + ts + "_" + UUID.randomUUID() + ext;

        Path dest = root.resolve(name);
        Files.copy(file.getInputStream(), dest);
        return dest.toString(); // absolute path stored in DB
    }

    // URL used by Vue thumbs
    public String toPublicUrl(String storedPath) {
        if (storedPath == null) return null;
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path p = Path.of(storedPath).toAbsolutePath().normalize();
        String fileName = root.relativize(p).toString().replace("\\", "/");
        return "/uploads/" + fileName;
    }

    private String sanitizeExt(String filename) {
        if (filename == null) return ".bin";
        int i = filename.lastIndexOf('.');
        if (i < 0) return ".bin";
        String e = filename.substring(i).toLowerCase();
        return (e.length() <= 6 && e.matches("\\.[a-z0-9]+")) ? e : ".bin";
    }
}
