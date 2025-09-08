package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.DeviceToken;
import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUser(RoomFinder user);
    Optional<DeviceToken> findByToken(String token);
    void deleteByToken(String token);
}
