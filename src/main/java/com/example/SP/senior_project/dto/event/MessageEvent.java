package com.example.SP.senior_project.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {
    private Long threadId;
    private Long id;        // message id
    private Long senderId;  // who sent it
    private String text;    // message content
    private String time;    // ISO createdAt
}