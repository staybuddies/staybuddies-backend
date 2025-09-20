package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.Notice;
import com.example.SP.senior_project.model.RoomFinder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    long countByUserAndReadFlagFalse(RoomFinder user);

    List<Notice> findTop100ByUserOrderByCreatedAtDesc(RoomFinder user);

    @Query("update Notice n set n.readFlag = true where n.user = :user and n.readFlag = false")
    @Modifying
    int markAllRead(RoomFinder user);
}
