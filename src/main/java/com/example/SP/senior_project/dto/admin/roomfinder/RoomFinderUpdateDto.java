package com.example.SP.senior_project.dto.admin.roomfinder;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RoomFinderUpdateDto {
    private String name;
    private String email;
    private String password;
    private String phone;
    private String gender;
    private Integer age;
    private String university;
    private Boolean alreadyHasRoom;
    private LocalDate joinDate;
}
