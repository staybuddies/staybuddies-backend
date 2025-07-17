package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "room_finders")   // change table name to underscores
public class RoomFinder extends AbstractAuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

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
}
