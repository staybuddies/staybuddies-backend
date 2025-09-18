package com.example.SP.senior_project.dto.roomfinder;

import com.example.SP.senior_project.model.constant.MatchStatus;
import lombok.Data;


@Data
public class RoomFinderDto {
    private Long    id;
    private String  name;
    private String  email;
    private String password;
    private String phone;
    private String  gender;
    private Integer age;
    private String location;
    private String  university;

    private String major;
    private String bio;

    private Boolean alreadyHasRoom;
    private Boolean locationSharing;
    private Boolean emailNotification;

    private String schoolEmail;
    private boolean schoolEmailVerified;
    private MatchStatus status;
}
