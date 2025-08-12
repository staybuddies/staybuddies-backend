package com.example.SP.senior_project.model;

import com.example.SP.senior_project.model.base.AbstractAuditableEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "room_finder_quizzes",
        uniqueConstraints = @UniqueConstraint(columnNames = "room_finder_id"))
@Data
public class RoomFinderQuiz extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_finder_id", nullable = false)
    private RoomFinder roomFinder;

    // 10 weighted answers, 1..5 (or your range)
    @Column(nullable = false)
    private Integer q1;
    @Column(nullable = false)
    private Integer q2;
    @Column(nullable = false)
    private Integer q3;
    @Column(nullable = false)
    private Integer q4;
    @Column(nullable = false)
    private Integer q5;
    @Column(nullable = false)
    private Integer q6;
    @Column(nullable = false)
    private Integer q7;
    @Column(nullable = false)
    private Integer q8;
    @Column(nullable = false)
    private Integer q9;
    @Column(nullable = false)
    private Integer q10;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;
}
