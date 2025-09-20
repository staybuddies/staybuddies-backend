package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.DeviceToken;
import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);
    void deleteByToken(String token);

    @Query("select t from DeviceToken t where t.user.id = :uid")
    List<DeviceToken> findAllByUser_Id(@Param("uid") Long userId);

}
