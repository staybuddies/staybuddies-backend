package com.example.SP.senior_project.dto.roomfinder;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RoomFinderUpdateDto {
    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    private String password;      // not used in admin edit

    private String phone;

    private String gender;

    @Min(10) @Max(100)
    private Integer age;

    private String location;
    private String university;
    private String major;

    @Size(max = 2000, message = "Bio too long")
    private String bio;

    private Boolean alreadyHasRoom;
    private Boolean locationSharing;
    private Boolean emailNotification;

    @PastOrPresent(message = "Join date cannot be in the future")
    private LocalDate joinDate;
}
