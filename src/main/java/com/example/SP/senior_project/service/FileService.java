package com.example.SP.senior_project.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.SP.senior_project.model.FileStorage;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.repository.FileStorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileService {

    private static final String UPLOAD_DIR = "/Users/zwenyanwin/Downloads/senior-project";

    @Value("${cloud.aws.bucket}")
    private String s3BucketName;

    @Value("${cloud.aws.region.static}")
    private String awsRegion;

    private final FileStorageRepository fileStorageRepository;
    private final AmazonS3 amazonS3;
    private final PublicUrlMapper publicUrlMapper;

    private final String s3BaseUrl = "https://%s.s3.%s.amazonaws.com/%s";

    /**
     * Returns the file URL for a given file type and id (e.g. ADMIN_PROFILE, adminId).
     * If no file exists, returns null.
     */
    public String getFileName(FileType fileType, Long id) {
        var list = fileStorageRepository.findByTypeAndFileId(fileType, id);
        if (list == null || list.isEmpty()) return null;

        var file = list.stream()
                .sorted(Comparator.comparing(FileStorage::getCreatedAt).reversed())
                .findFirst()
                .orElse(null);

        if (file == null) return null;
        return getFileUrl(file.getKey(), file.getServiceName());
    }

    public String getFileUrl(String fileKey, String serviceName) {
        if (fileKey == null || serviceName == null) return null;

        if ("local".equalsIgnoreCase(serviceName)) {
            // Let the mapper handle keys or absolute paths
            return publicUrlMapper.toPublicUrl(fileKey);
        }
        // S3
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                s3BucketName, awsRegion, fileKey);
    }

    /**
     * Handles uploading a file for a given entity.
     * If an old file exists for the same type + id, deletes it first.
     */
    public void handleFileUpload(MultipartFile file, FileType fileType, Long id, String serviceName) {

        if (file == null || file.isEmpty()) {
            log.info("No file uploaded for type {} and id {}", fileType, id);
            return;
        }

        // Delete old file if exists
        deleteExistingFile(fileType, id);

        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String fileExtension = "";

        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String storedFileName = uuid + fileExtension;

        try {
            if ("local".equalsIgnoreCase(serviceName)) {
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) dir.mkdirs();

                String filePath = UPLOAD_DIR + File.separator + storedFileName;
                file.transferTo(new File(filePath));
                log.info("File uploaded successfully to Local: {}", storedFileName);
            } else {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(file.getContentType());
                metadata.setContentLength(file.getSize());

                amazonS3.putObject(s3BucketName, storedFileName, file.getInputStream(), metadata);
                log.info("File uploaded successfully to S3: {}", storedFileName);
            }

            FileStorage fileStorage = new FileStorage();
            fileStorage.setFileName(originalFileName);
            fileStorage.setKey(storedFileName);
            fileStorage.setFileSize(file.getSize());
            fileStorage.setType(fileType);
            fileStorage.setFileId(id);
            fileStorage.setContentType(file.getContentType());
            fileStorage.setServiceName(serviceName);

            fileStorageRepository.save(fileStorage);

        } catch (IOException ex) {
            throw new RuntimeException("Failed to upload file: " + ex.getMessage(), ex);
        }
    }

    /**
     * Deletes an existing file record and physical file (local or S3).
     */
    private void deleteExistingFile(FileType fileType, Long id) {
        List<FileStorage> existingFiles = fileStorageRepository.findByTypeAndFileId(fileType, id);

        for (FileStorage file : existingFiles) {
            // Delete physical file
            if ("local".equalsIgnoreCase(file.getServiceName())) {
                String filePath = UPLOAD_DIR + File.separator + file.getKey();
                File localFile = new File(filePath);
                if (localFile.exists() && localFile.delete()) {
                    log.info("Deleted local file: {}", filePath);
                }
            } else {
                if (amazonS3.doesObjectExist(s3BucketName, file.getKey())) {
                    amazonS3.deleteObject(s3BucketName, file.getKey());
                    log.info("Deleted S3 file: {}", file.getKey());
                }
            }

            // Delete DB record
            fileStorageRepository.delete(file);
        }
    }
}
