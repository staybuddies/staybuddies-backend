package com.example.SP.senior_project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens",
        indexes = @Index(name = "idx_otp_user_purpose", columnList = "userEmail,purpose,expiresAt"))
@Getter
@Setter
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;
    @Column(nullable = false, length = 40)
    private String purpose;   // e.g. ACCOUNT_VERIFY
    @Column(nullable = false, length = 120)
    private String codeHash; // bcrypt
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Column
    private LocalDateTime consumedAt;
    @Column(nullable = false)
    private int attempts = 0;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
