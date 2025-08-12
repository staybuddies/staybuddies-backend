package com.example.SP.senior_project.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "quiz_responses")
public class QuizResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_finder_id", unique = true)
    private RoomFinder roomFinder;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quiz_answers", joinColumns = @JoinColumn(name = "quiz_id"))
    @OrderColumn(name = "idx")
    @Column(name = "answer")
    private List<Integer> answers = new ArrayList<>(); // 10 integers, 1..5
}

