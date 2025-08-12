package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.QuizResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizResponseRepository extends JpaRepository<QuizResponse, Long> {
    Optional<QuizResponse> findByRoomFinderEmail(String email);

    @EntityGraph(attributePaths = {"roomFinder","answers"})
    List<QuizResponse> findAll();
}
