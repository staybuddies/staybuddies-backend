package com.example.SP.senior_project.dto.match;

import lombok.Data;

@Data
public class MatchDto {
    private Long userId;
    private String name;
    private Integer age;
    private String gender;
    private String location;
    private Integer compatibility; // 0..100
    private String relationStatus; // NONE | PENDING_SENT | PENDING_RECEIVED | ACCEPTED | DECLINED
    private Long requestId;        // present if there is a request between you two
    private Long threadId;
    private String photoUrl;
}
