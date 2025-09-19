package com.example.SP.senior_project.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminMetrics {
    private long totalUsers;
    private long activeUsers;
    private long matchesMade;
    private long newUsersToday;
    private String activeNote;
    private String matchRateNote;
}
