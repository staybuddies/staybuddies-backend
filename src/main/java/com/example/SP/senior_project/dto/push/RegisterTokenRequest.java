package com.example.SP.senior_project.dto.push;

import lombok.Data;

@Data
public class RegisterTokenRequest {
    private String token;    // required
    private String platform; // optional: WEB/IOS/ANDROID
    private String deviceId; // optional
}
