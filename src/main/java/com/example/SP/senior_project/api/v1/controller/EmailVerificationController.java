package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/verify-email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final OtpService otp;
    private final RoomFinderRepository roomFinderRepo;

    private static final String PURPOSE = "ACCOUNT_VERIFY";

    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> request(@RequestParam String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL_RE.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address.");
        }

        // send OTP (10 minutes)
        otp.send(normalized, PURPOSE, Duration.ofMinutes(10));

        // if logged in, store email and reset verified flag
        currentUser().ifPresent(user -> {
            user.setSchoolEmail(normalized);
            user.setSchoolEmailVerified(false);
            roomFinderRepo.save(user);
        });

        return ResponseEntity.ok(Map.of("status", "SENT"));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@RequestParam String email, @RequestParam String code) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!EMAIL_RE.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address.");
        }
        if (!otp.verify(normalized, PURPOSE, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid/expired code.");
        }

        RoomFinder saved = currentUser().map(user -> {
            user.setSchoolEmail(normalized);
            user.setSchoolEmailVerified(true);
            return roomFinderRepo.save(user);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // return fresh truth so UI can flip immediately
        return ResponseEntity.ok(Map.of(
                "schoolEmail", saved.getSchoolEmail(),
                "schoolEmailVerified", saved.isSchoolEmailVerified()
        ));
    }

    private Optional<RoomFinder> currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        String principalEmail = auth.getName();
        return roomFinderRepo.findByEmailIgnoreCase(principalEmail);
    }
}
