package com.example.SP.senior_project.dto.admin;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AdminRequest {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private MultipartFile file;
}