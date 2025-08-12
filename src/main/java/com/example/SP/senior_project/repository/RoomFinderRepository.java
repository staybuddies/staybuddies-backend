package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoomFinderRepository extends JpaRepository<RoomFinder, Long> {
    Optional<RoomFinder> findByEmail(String email);
    boolean existsByEmail(String email);

}