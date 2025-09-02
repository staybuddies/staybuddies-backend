package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import com.example.SP.senior_project.model.constant.VerificationStatus;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "student_id_verifications")
public class StudentIdVerification extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SINGLE association only; keep the existing DB column name
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_finder_id", nullable = false)
    private RoomFinder user;

    // denormalized for convenience / reporting
    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.DRAFT;

    @Column(name = "front_path")
    private String frontPath;

    @Column(name = "back_path")
    private String backPath;

    @Column(name = "selfie_path")
    private String selfiePath;

    @Column(name = "name_on_card")
    private String nameOnCard;

    @Column(name = "student_id_on_card")
    private String studentIdOnCard;

    @Column(name = "university_on_card")
    private String universityOnCard;

    @Column(name = "grad_year_on_card")
    private Integer gradYearOnCard;

    @Column(length = 1000)
    private String note;

    private Double score;

    @PrePersist
    void fillUserEmail() {
        if (userEmail == null && user != null) {
            userEmail = user.getEmail();
        }
    }
}
