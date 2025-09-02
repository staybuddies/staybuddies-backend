package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.StudentIdVerification;
import com.example.SP.senior_project.model.constant.VerificationStatus;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.repository.StudentIdVerificationRepository;
import com.example.SP.senior_project.service.MlClient;
import com.example.SP.senior_project.service.StorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/verifications/student-id")
@RequiredArgsConstructor
public class StudentIdVerificationController {

    private final StudentIdVerificationRepository repo;
    private final RoomFinderRepository users;
    private final StorageService storage;
    private final MlClient ml;

    /* ---------------- me ---------------- */

    @GetMapping("/me")
    public ResponseEntity<Dto> me() {
        RoomFinder me = currentUser();
        return ResponseEntity.ok(
                repo.findTopByUserOrderByCreatedAtDesc(me)
                        .map(this::toDto)
                        .orElse(null)
        );
    }

    /* ---------------- create ---------------- */

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<Map<String, Object>> create() {
        RoomFinder me = currentUser();

        StudentIdVerification v = new StudentIdVerification();
        v.setUser(me);
        v.setStatus(VerificationStatus.DRAFT);
        // make sure old paths are empty for a new record
        v.setFrontPath(null);
        v.setBackPath(null);
        v.setSelfiePath(null);

        repo.save(v);
        return ResponseEntity.ok(Map.of("id", v.getId(), "status", v.getStatus()));
    }

    /* ---------------- upload (FRONT ONLY) ---------------- */

    @PutMapping("/{id}/upload")
    @Transactional
    public ResponseEntity<Void> uploadFront(@PathVariable long id,
                                            @RequestParam(value = "side", required = false) String side,
                                            @RequestPart("file") MultipartFile file) throws IOException {
        StudentIdVerification v = getOwned(id);

        // Accept only front. If a client still sends side=front that's fine; anything else is rejected.
        if (side != null && !"front".equalsIgnoreCase(side)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only 'front' upload is supported.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required.");
        }

        String saved = storage.save("front", file, v.getId());
        v.setFrontPath(saved);

        // make sure back/selfie are not used in this flow
        v.setBackPath(null);
        v.setSelfiePath(null);

        v.setStatus(VerificationStatus.DRAFT);
        repo.save(v);
        return ResponseEntity.ok().build();
    }

    /* ---------------- submit (FRONT ONLY) ---------------- */

    @PutMapping("/{id}/submit")
    @Transactional
    public ResponseEntity<Void> submit(@PathVariable long id) {
        StudentIdVerification v = getOwned(id);
        if (v.getFrontPath() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "front image required");
        }

        v.setStatus(VerificationStatus.PENDING);
        repo.save(v);

        // ---- Call Python ML (front only)
        MlClient.AnalyzeResponse r;
        try {
            r = ml.analyze(v.getFrontPath(), null, null);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Verification service is temporarily unavailable. Please try again.",
                    ex
            );
        }

        // Map ML results
        v.setScore(r.getScore());
        v.setNameOnCard(r.getName());
        v.setStudentIdOnCard(r.getStudentId());
        v.setUniversityOnCard(r.getUniversity());
        v.setGradYearOnCard(r.getGradYear());

        // ---- Business rule: ML ok AND email matches the ID pattern
        boolean mlOk = r.isOk();
        boolean emailMatches = idMatchesAnyUserEmail(v.getUser(), r.getStudentId());
        v.setStatus(mlOk && emailMatches ? VerificationStatus.VERIFIED : VerificationStatus.REJECTED);

        StringBuilder note = new StringBuilder();
        if (r.getNote() != null && !r.getNote().isBlank()) note.append(r.getNote());
        if (note.length() > 0) note.append("; ");
        note.append(emailMatches ? "email/ID match" : "email/ID mismatch");

        v.setNote(note.toString());

        repo.save(v);
        return ResponseEntity.ok().build();
    }

    /* ------------- helpers ------------- */

    private RoomFinder currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseGet(() -> users.findByEmail(auth.getName())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));
    }

    private StudentIdVerification getOwned(long id) {
        RoomFinder me = currentUser();
        StudentIdVerification v = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!v.getUser().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return v;
    }

    private boolean idMatchesAnyUserEmail(RoomFinder user, String studentId) {
        if (studentId == null || studentId.isBlank()) return false;

        String school = Optional.ofNullable(user.getSchoolEmail()).orElse("");
        String primary = Optional.ofNullable(user.getEmail()).orElse("");

        // e.g. u6540179@*.au.edu or 6540179@*.edu
        Pattern p1 = Pattern.compile("^u?" + Pattern.quote(studentId) + "@.+\\.(?:au\\.)?edu$", Pattern.CASE_INSENSITIVE);

        return p1.matcher(school).find() || p1.matcher(primary).find();
    }

    private Dto toDto(StudentIdVerification v) {
        Dto d = new Dto();
        d.setId(v.getId());
        d.setStatus(v.getStatus().name());
        d.setScore(v.getScore());
        d.setNote(v.getNote());
        d.setNameOnCard(v.getNameOnCard());
        d.setStudentIdOnCard(v.getStudentIdOnCard());
        d.setUniversityOnCard(v.getUniversityOnCard());
        d.setGradYearOnCard(v.getGradYearOnCard());
        d.setFrontUrl(storage.toPublicUrl(v.getFrontPath()));
        // back/selfie are not used; keep fields null for client compatibility
        d.setBackUrl(null);
        d.setSelfieUrl(null);
        return d;
    }

    @Data
    public static class Dto {
        private Long id;
        private String status;
        private Double score;
        private String note;
        private String nameOnCard;
        private String studentIdOnCard;
        private String universityOnCard;
        private Integer gradYearOnCard;
        private String frontUrl;
        private String backUrl;
        private String selfieUrl;
    }
}
