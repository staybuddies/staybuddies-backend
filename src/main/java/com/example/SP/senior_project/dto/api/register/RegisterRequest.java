package com.example.SP.senior_project.dto.api.register;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
    // …any other fields…
}
