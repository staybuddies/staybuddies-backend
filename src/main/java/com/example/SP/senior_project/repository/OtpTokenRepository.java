package com.example.SP.senior_project.repository;


import com.example.SP.senior_project.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    @Transactional
    @Modifying
    int deleteByUserEmailAndPurposeAndExpiresAtBefore(String email, String purpose, LocalDateTime now);

    Optional<OtpToken>
    findFirstByUserEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(String email, String purpose);
}
