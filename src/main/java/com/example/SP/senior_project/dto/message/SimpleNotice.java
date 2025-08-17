package com.example.SP.senior_project.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class SimpleNotice {
    private String type;
    private Long threadId;
}
