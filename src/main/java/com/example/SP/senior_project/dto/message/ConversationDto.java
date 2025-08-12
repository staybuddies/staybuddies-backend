package com.example.SP.senior_project.dto.message;

import lombok.Data;

@Data
public class ConversationDto {
    private Long id;                   // thread id
    private Long otherId;
    private String otherName;
    private String otherGender;
    private String otherLocation;
    private String lastMessage;
    private String lastTime;           // ISO string
    private long unread;
}
