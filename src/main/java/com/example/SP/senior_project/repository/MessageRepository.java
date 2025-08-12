package com.example.SP.senior_project.repository;

import com.example.SP.senior_project.model.Message;
import com.example.SP.senior_project.model.MessageThread;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByThreadOrderByCreatedAtAsc(MessageThread t);

    Optional<Message> findTopByThreadOrderByCreatedAtDesc(MessageThread t);

    long countByThreadAndReadByOtherFalseAndSender_IdNot(MessageThread t, Long notSenderId);
}
