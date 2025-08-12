package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.MessageThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {

    // find thread between two users (any order)
    @Query("""
               select t from MessageThread t
                 join fetch t.user1 u1
                 join fetch t.user2 u2
               where (u1.id = :a and u2.id = :b) or (u1.id = :b and u2.id = :a)
            """)
    Optional<MessageThread> findBetween(Long a, Long b);

    // list all threads for user (by email) with both users eagerly loaded
    @Query("""
               select t from MessageThread t
                 join fetch t.user1 u1
                 join fetch t.user2 u2
               where u1.email = :email or u2.email = :email
               order by t.updatedAt desc
            """)
    List<MessageThread> findAllForUserWithUsers(String email);

    Optional<MessageThread> findByUser1_IdAndUser2_IdOrUser1_IdAndUser2_Id(
            Long a1, Long a2, Long b1, Long b2);

    @Query("""
            select t from MessageThread t
            where t.user1.id = :uid or t.user2.id = :uid
            order by t.updatedAt desc
            """)
    List<MessageThread> findAllForUser(@Param("uid") Long uid);
}
