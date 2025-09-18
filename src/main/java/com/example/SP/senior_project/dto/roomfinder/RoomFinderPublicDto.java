package com.example.SP.senior_project.dto.roomfinder;

import com.example.SP.senior_project.model.constant.MatchStatus;
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

    private boolean emailVerified;     // from RoomFinder.schoolEmailVerified
    private boolean identityVerified;  // from RoomFinder.idVerified (or false if not present)
    private boolean alreadyHasRoom;    // from RoomFinder.alreadyHasRoom
    private String schoolEmail;        //  lets you show the domain

    // From quiz
    private List<String> lifestyleTags;
    private List<String> whyYouMatch;

    private String photoUrl;

    private List<Integer> lifestyleAnswers;

    private MatchStatus status;
}
