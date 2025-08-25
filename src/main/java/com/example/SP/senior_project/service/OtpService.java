package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.OtpToken;
import com.example.SP.senior_project.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional
    public void send(String email, String purpose, Duration ttl) {
        String normalized = email.trim().toLowerCase();

        repo.deleteByUserEmailAndPurposeAndExpiresAtBefore(
                normalized, purpose, LocalDateTime.now());

        String code = String.format("%06d",
                ThreadLocalRandom.current().nextInt(0, 1_000_000));

        OtpToken t = new OtpToken();
        t.setUserEmail(normalized);
        t.setPurpose(purpose);
        t.setCodeHash(passwordEncoder.encode(code));
        t.setExpiresAt(LocalDateTime.now().plusSeconds(ttl.getSeconds()));
        repo.save(t);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mailService.sendOtp(normalized, code);
                } catch (Exception ex) {
                    log.error("OTP email send failed to {}: {}", normalized, ex.getMessage(), ex);
                }
            }
        });
    }

    @Transactional
    public boolean verify(String email, String purpose, String code) {
        String normalized = email.trim().toLowerCase();
        var opt = repo.findFirstByUserEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                normalized, purpose);
        if (opt.isEmpty()) return false;

        var t = opt.get();
        if (t.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        if (!passwordEncoder.matches(code, t.getCodeHash())) {
            t.setAttempts(t.getAttempts() + 1);
            repo.save(t);
            return false;
        }

        t.setConsumedAt(LocalDateTime.now());
        repo.save(t);
        return true;
    }
}
