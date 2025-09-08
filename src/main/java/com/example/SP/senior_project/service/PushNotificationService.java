package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.Message;
import com.example.SP.senior_project.model.RoomFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final SimpMessagingTemplate broker;     // WebSocket broker (STOMP)
    private final FirebaseFacade firebase;          // wraps FirebaseMessaging (may no-op if not configured)

    /**
     * Called AFTER message is persisted.
     */
    public void notifyNewMessage(RoomFinder receiver,
                                 RoomFinder sender,
                                 Long threadId,
                                 Message m) {
        // ISO-8601 time in UTC (never throws on LocalDateTime)
        String iso = (m.getCreatedAt() == null)
                ? OffsetDateTime.now(ZoneOffset.UTC).toString()
                : m.getCreatedAt().atOffset(ZoneOffset.UTC).toString();

        // 1) broadcast full event to thread stream (both participants)
        Map<String, Object> evt = Map.of(
                "type", "MESSAGE",
                "threadId", threadId,
                "id", m.getId(),
                "senderId", sender.getId(),
                "text", m.getContent(),
                "time", iso
        );
        try {
            broker.convertAndSend("/topic/threads/" + threadId, evt);
            log.debug("WS: /topic/threads/{} -> {}", threadId, m.getId());
        } catch (Exception ex) {
            log.warn("WS thread broadcast failed", ex);
        }

        // 2) lightweight notice: preferred per-user queue (needs WS principal)
        try {
            broker.convertAndSendToUser(
                    String.valueOf(receiver.getId()),
                    "/queue/notice",
                    Map.of("type", "MESSAGE", "threadId", threadId));
            log.debug("WS: /user/{}/queue/notice", receiver.getId());
        } catch (Exception ex) {
            log.debug("WS user queue failed (no Principal?) -> will rely on topic fallback");
        }

        // 3) fallback public topic keyed by user id (no Principal needed)
        try {
            broker.convertAndSend(
                    "/topic/notice.user-" + receiver.getId(),
                    Map.of("type", "MESSAGE", "threadId", threadId));
            log.debug("WS: /topic/notice.user-{}", receiver.getId());
        } catch (Exception ex) {
            log.warn("WS notice fallback failed", ex);
        }

        // 4) FCM (if receiver has device token)
        try {
            firebase.sendNewMessagePush(receiver, sender, m.getContent());
        } catch (Exception ex) {
            log.warn("FCM send failed (non-fatal): {}", ex.getMessage());
        }
    }
}
