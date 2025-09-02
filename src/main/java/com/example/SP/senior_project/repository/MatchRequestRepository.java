package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.MatchRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    // used by MatchService.toMatchDto(...)
    Optional<MatchRequest> findByRequester_IdAndTarget_Id(Long requesterId, Long targetId);

    // used by MatchService.sendRequest(...)
    boolean existsByRequester_IdAndTarget_Id(Long requesterId, Long targetId);

    // used by MatchService.accept/decline(...)
    @Query("""
           select mr
           from MatchRequest mr
           join fetch mr.requester
           join fetch mr.target
           where mr.id = :id
           """)
    Optional<MatchRequest> findWithUsersById(@Param("id") Long id);

    // utility for cleanup (e.g., when deleting a user)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           delete from MatchRequest mr
           where mr.requester.id = :id
              or mr.target.id = :id
           """)
    void deleteAllForUser(@Param("id") Long id);
}
