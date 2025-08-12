package com.example.SP.senior_project.dto.api.login;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}