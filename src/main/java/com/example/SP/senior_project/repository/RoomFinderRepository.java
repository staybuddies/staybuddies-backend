package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoomFinderRepository extends JpaRepository<RoomFinder, Long> {
    Optional<RoomFinder> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<RoomFinder> findByEmailIgnoreCase(String email);

    Page<RoomFinder> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    Page<RoomFinder> findByActiveTrue(Pageable pageable);

}