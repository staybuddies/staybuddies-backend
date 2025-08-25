package com.example.SP.senior_project.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final Environment env;

    private String resolveFrom() {
        // Prefer app.mail.from; fallback to spring.mail.username
        return env.getProperty("app.mail.from",
                env.getProperty("spring.mail.username", ""));
    }

    public void sendOtp(String to, String code) {
        String from = resolveFrom();
        String user = env.getProperty("spring.mail.username", "");
        String pass = env.getProperty("spring.mail.password", "");

        if (!StringUtils.hasText(from) || !from.contains("@")) {
            throw new IllegalStateException("Email not configured: missing 'app.mail.from' or 'spring.mail.username'.");
        }
        if (!StringUtils.hasText(user)) {
            throw new IllegalStateException("Email not configured: missing 'spring.mail.username'.");
        }
        if (!StringUtils.hasText(pass)) {
            // This is the exact cause of your previous stacktrace
            throw new IllegalStateException("Email not configured: missing 'spring.mail.password' (Gmail App Password).");
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Your verification code");
        msg.setText("Your code is: " + code + " (valid for 10 minutes)");
        mailSender.send(msg);
        log.info("OTP email queued to {}", to);
    }
}
