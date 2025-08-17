package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

}


