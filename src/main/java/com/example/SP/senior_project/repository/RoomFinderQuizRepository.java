package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.RoomFinderQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomFinderQuizRepository extends JpaRepository<RoomFinderQuiz, Long> {
    Optional<RoomFinderQuiz> findByRoomFinderEmail(String email);
}