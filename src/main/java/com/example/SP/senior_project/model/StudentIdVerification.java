package com.example.SP.senior_project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_id_verifications")
@Getter
@Setter
public class StudentIdVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;     // RoomFinder.id
    @Column(nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT; // DRAFT -> PENDING -> VERIFIED/REJECTED

    private Integer score;
    private String note;
    private String nameOnCard;
    private String studentIdOnCard;
    private String universityOnCard;

    // file presence flags or storage keys (optional)
    private Boolean hasFront = false;
    private Boolean hasBack = false;
    private Boolean hasSelfie = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {DRAFT, PENDING, VERIFIED, REJECTED}
}
