package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "room_finders")   // change table name to underscores
public class RoomFinder extends AbstractAuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phone;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(nullable = false)
    private boolean active;

    private String gender;

    private int age;

    private String location;

    private String university;

    @Column(name = "already_has_room", nullable = false)
    private boolean alreadyHasRoom;

    @Column(nullable = false)
    private boolean locationSharing = false;
    @Column(nullable = false)
    private boolean emailNotification = false;

    @Column(length = 2000)          // bio can be long
    private String bio;

    @Column(length = 255)
    private String major;
    // for email verification
    @Column
    private String schoolEmail;
    @Column(nullable = false)
    private boolean schoolEmailVerified = false;

    @Column(nullable = false)
    private int tokenVersion = 0;


}


