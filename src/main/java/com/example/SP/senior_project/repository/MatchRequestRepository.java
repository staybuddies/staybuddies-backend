package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.MatchRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    Optional<MatchRequest> findByRequester_IdAndTarget_Id(Long requesterId, Long targetId);
    boolean existsByRequester_IdAndTarget_Id(Long requesterId, Long targetId);

    @Query("""
           select mr
           from MatchRequest mr
           join fetch mr.requester
           join fetch mr.target
           where mr.id = :id
           """)
    Optional<MatchRequest> findWithUsersById(@Param("id") Long id);

    // ---- DISTINCT unordered pairs (no double counting) ----
    @Query(value = """
      select least(requester_id, target_id) as a,
             greatest(requester_id, target_id) as b
      from match_requests
      where status = 'ACCEPTED'
      group by a, b
    """, nativeQuery = true)
    List<Object[]> distinctAcceptedPairs();

    @Query(value = """
      select count(*) from (
        select least(requester_id, target_id) as a,
               greatest(requester_id, target_id) as b
        from match_requests
        where status = 'ACCEPTED'
        group by a, b
      ) t
    """, nativeQuery = true)
    long countDistinctAcceptedPairs();
}
