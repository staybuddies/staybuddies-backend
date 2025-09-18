package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.StudentIdVerification;
import com.example.SP.senior_project.model.constant.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentIdVerificationRepository
        extends JpaRepository<StudentIdVerification, Long> {

    // If your entity has a createdAt field (e.g., from a base class), keep this:
    Optional<StudentIdVerification> findTopByUserOrderByCreatedAtDesc(RoomFinder user);

    // …otherwise use id fallback:
    // Optional<StudentIdVerification> findTopByUserOrderByIdDesc(RoomFinder user);

    Optional<StudentIdVerification> findTopByUser_IdOrderByCreatedAtDesc(Long userId);

    //  Correct derived methods (association is "user")
    boolean existsByUserAndStatus(RoomFinder user, VerificationStatus status);
    boolean existsByUser_IdAndStatus(Long userId, VerificationStatus status);
}
