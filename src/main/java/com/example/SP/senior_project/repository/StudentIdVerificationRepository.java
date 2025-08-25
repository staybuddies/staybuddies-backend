package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.StudentIdVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentIdVerificationRepository extends JpaRepository<StudentIdVerification, Long> {
    Optional<StudentIdVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
