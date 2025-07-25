package com.example.SP.senior_project.dto.admin.api.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String jwt;
}
