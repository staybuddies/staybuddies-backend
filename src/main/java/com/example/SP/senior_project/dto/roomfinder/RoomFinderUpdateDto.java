package com.example.SP.senior_project.dto.roomfinder;

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
    private String location;
    private String university;
    private Boolean alreadyHasRoom;
    private Boolean locationSharing;
    private Boolean emailNotification;
    private LocalDate joinDate;

}
