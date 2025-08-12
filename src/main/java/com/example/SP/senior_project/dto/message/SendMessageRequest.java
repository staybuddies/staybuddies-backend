package com.example.SP.senior_project.dto.message;

import lombok.Data;

@Data
public class SendMessageRequest {
    // standardize on "content"
    private String content;

    // optional: backward-compat if you already shipped "body" from FE
    private String body;

    public String resolved() {
        return (content != null && !content.isBlank()) ? content : body;
    }
}
