package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.StudentIdVerification;
import com.example.SP.senior_project.model.constant.FileType;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.repository.StudentIdVerificationRepository;
import com.example.SP.senior_project.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verifications/student-id")
public class StudentIdVerificationController {

    private final StudentIdVerificationRepository repo;
    private final RoomFinderRepository users;
    private final FileService files;

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        RoomFinder me = users.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        StudentIdVerification v = repo.findTopByUserIdOrderByCreatedAtDesc(me.getId()).orElse(null);
        if (v == null) return ResponseEntity.ok(null);

        return ResponseEntity.ok(toDto(v));
    }

    /* POST /verifications/student-id/create */
    @PostMapping("/create")
    public ResponseEntity<?> create(Authentication auth) {
        RoomFinder me = users.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudentIdVerification v = new StudentIdVerification();
        v.setUserId(me.getId());
        v.setUserEmail(me.getEmail());
        repo.save(v);

        return ResponseEntity.ok(Map.of("id", v.getId()));
    }

    /* PUT /verifications/student-id/{id}/upload  (multipart; side=front|back|selfie) */
    @PutMapping("/{id}/upload")
    public ResponseEntity<?> upload(@PathVariable Long id,
                                    @RequestParam("side") String side,
                                    @RequestParam("file") MultipartFile file,
                                    Authentication auth) {
        StudentIdVerification v = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification not found"));
        // basic ownership check
        if (!v.getUserEmail().equalsIgnoreCase(auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        // store file
        switch (side.toLowerCase()) {
            case "front" -> {
                files.handleFileUpload(file, FileType.STUDENT_ID_FRONT, id, "s3");
                v.setHasFront(true);
            }
            case "back" -> {
                files.handleFileUpload(file, FileType.STUDENT_ID_BACK, id, "s3");
                v.setHasBack(true);
            }
            case "selfie" -> {
                files.handleFileUpload(file, FileType.STUDENT_ID_SELFIE, id, "s3");
                v.setHasSelfie(true);
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid side"));
            }
        }
        v.setUpdatedAt(LocalDateTime.now());
        repo.save(v);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /* PUT /verifications/student-id/{id}/submit */
    @PutMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, Authentication auth) {
        StudentIdVerification v = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Verification not found"));
        if (!v.getUserEmail().equalsIgnoreCase(auth.getName()))
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        if (!Boolean.TRUE.equals(v.getHasFront()) || !Boolean.TRUE.equals(v.getHasBack())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please upload front & back."));
        }
        v.setStatus(StudentIdVerification.Status.PENDING);
        v.setUpdatedAt(LocalDateTime.now());
        repo.save(v);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private Map<String, Object> toDto(StudentIdVerification v) {
        String front = v.getHasFront() ? safeUrl(FileType.STUDENT_ID_FRONT, v.getId()) : null;
        String back = v.getHasBack() ? safeUrl(FileType.STUDENT_ID_BACK, v.getId()) : null;
        String selfie = v.getHasSelfie() ? safeUrl(FileType.STUDENT_ID_SELFIE, v.getId()) : null;

        return Map.of(
                "id", v.getId(),
                "status", v.getStatus().name(),
                "score", v.getScore(),
                "note", v.getNote(),
                "nameOnCard", v.getNameOnCard(),
                "studentIdOnCard", v.getStudentIdOnCard(),
                "universityOnCard", v.getUniversityOnCard(),
                "frontUrl", front,
                "backUrl", back,
                "selfieUrl", selfie
        );
    }

    private String safeUrl(FileType type, Long ownerId) {
        try {
            return files.getFileName(type, ownerId);
        } catch (Exception ex) {
            return null;
        }
    }
}
