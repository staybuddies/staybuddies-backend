package com.example.SP.senior_project.service;

public interface EmailVerificationService {
    void sendCode(Long roomFinderId, String email);
    void confirmCode(Long roomFinderId, String email, String code);
}
