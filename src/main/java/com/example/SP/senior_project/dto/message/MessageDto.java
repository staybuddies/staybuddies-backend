package com.example.SP.senior_project.dto.message;

import lombok.Data;

@Data
public class MessageDto {
    private Long id;
    private boolean fromMe;
    private String text;
    private String time;
}
