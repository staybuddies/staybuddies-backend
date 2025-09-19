package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomFinderRepository extends JpaRepository<RoomFinder, Long> {
    Optional<RoomFinder> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<RoomFinder> findByEmailIgnoreCase(String email);

    Page<RoomFinder> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String name, String email, Pageable pageable);

    Page<RoomFinder> findByActiveTrue(Pageable pageable);

    long countByActiveTrue();

    long countByJoinDate(LocalDate date);

    @Query("""
            select u.joinDate, count(u)
            from RoomFinder u
            where u.joinDate between :from and :to
            group by u.joinDate
            order by u.joinDate
            """)
    List<Object[]> countNewUsersByDay(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    long countByJoinDateBetween(LocalDateTime from, LocalDateTime to);
}