package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.MatchRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {
    boolean existsByRequester_IdAndTarget_Id(Long requesterId, Long targetId);
    Optional<MatchRequest> findByRequester_IdAndTarget_Id(Long requesterId, Long targetId);

    @EntityGraph(attributePaths = {"requester","target"})
    Optional<MatchRequest> findWithUsersById(Long id);
}
