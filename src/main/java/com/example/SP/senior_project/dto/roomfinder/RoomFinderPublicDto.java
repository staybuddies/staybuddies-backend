package com.example.SP.senior_project.dto.roomfinder;

import lombok.Data;

import java.util.List;

@Data
public class RoomFinderPublicDto {
    Long id;
    String name;
    String gender;
    Integer age;
    String location;
    String university;

    private String bio;
    private String major;

    // From quiz
    private List<String> lifestyleTags; // e.g. ["Early bird","Very tidy","Needs quiet"]
    private List<String> whyYouMatch;
}
