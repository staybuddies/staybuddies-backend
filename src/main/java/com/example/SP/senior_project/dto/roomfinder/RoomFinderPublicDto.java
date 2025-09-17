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

    //  NEW
    private Boolean emailVerified;     // from RoomFinder.schoolEmailVerified
    private Boolean identityVerified;  // from RoomFinder.idVerified (or false if not present)
    private Boolean alreadyHasRoom;    // from RoomFinder.alreadyHasRoom
    private String schoolEmail;        // (optional) lets you show the domain

    // From quiz
    private List<String> lifestyleTags;
    private List<String> whyYouMatch;

    private String photoUrl;

    private List<Integer> lifestyleAnswers;
}
