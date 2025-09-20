package com.example.SP.senior_project.dto.notice;

import lombok.Data;

@Data
public class NoticeDto {
    private Long id;
    private String type;       // "MESSAGE" / "MATCH_REQUESTED" / ...
    private boolean read;
    private String createdAt;  // ISO

    private Long threadId;
    private Long fromUserId;
    private String fromName;

    private String title;
    private String body;
}
