package com.example.SP.senior_project.dto.admin;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminUserRow {
    private Long id;
    private String email;
    private String name;
    private LocalDate joinDate;
    private boolean active;

    // NEW
    private boolean emailVerified;   // from RoomFinder.schoolEmailVerified
    private String idvStatus;        // "VERIFIED" | "PENDING" | "REJECTED" | null
}