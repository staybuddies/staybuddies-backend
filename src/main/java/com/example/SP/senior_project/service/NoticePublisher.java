// src/main/java/.../service/NoticePublisher.java
package com.example.SP.senior_project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NoticePublisher {
    private final SimpMessagingTemplate ws;

    public void toUser(Long userId, Map<String, Object> payload) {
        if (userId == null) return;
        payload.putIfAbsent("createdAt", OffsetDateTime.now().toString());

        // Per-user queue
        ws.convertAndSendToUser(String.valueOf(userId), "/queue/notice", payload);
        // Public fallback topic (your Navbar also subscribes to this)
        ws.convertAndSend("/topic/notice.user-" + userId, payload);
    }
}
