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

    private final SimpMessagingTemplate broker;   // STOMP broker
    private final FirebaseFacade firebase;        // optional (may no-op if not configured)

    /**
     * Broadcasts a newly-saved message to:
     * - /topic/threads/{threadId}                 (live chat stream for the open thread)
     * - /user/{receiverEmail}/queue/notice        (best: per-user queue, requires Principal.name=email)
     * - /topic/notice.user-{receiverId}           (fallback: id-keyed public topic, no Principal needed)
     * - FCM (optional)
     */
    public void notifyNewMessage(RoomFinder receiver,
                                 RoomFinder sender,
                                 Long threadId,
                                 Message m) {

        // Stable ISO-8601 timestamp (UTC)
        final String iso = (m.getCreatedAt() == null)
                ? OffsetDateTime.now(ZoneOffset.UTC).toString()
                : m.getCreatedAt().atOffset(ZoneOffset.UTC).toString();

        // Full event payload used by the chat stream
        final Map<String, Object> evt = Map.of(
                "type", "MESSAGE",
                "threadId", threadId,
                "id", m.getId(),
                "senderId", sender.getId(),
                "text", String.valueOf(m.getContent()),
                "time", iso
        );

        // 1) Broadcast to the thread stream so both ends see the message instantly
        try {
            broker.convertAndSend("/topic/threads/" + threadId, evt);
            log.debug("WS -> /topic/threads/{} (msgId={})", threadId, m.getId());
        } catch (Exception ex) {
            log.warn("WS thread broadcast failed", ex);
        }

        // 2) Per-user queue (BEST): requires Principal.name to be the user's email on the WS session
        final String receiverEmail = safeTrim(receiver.getEmail());
        if (!receiverEmail.isEmpty()) {
            try {
                broker.convertAndSendToUser(
                        receiverEmail,
                        "/queue/notice",
                        Map.of("type", "MESSAGE", "threadId", threadId)
                );
                log.debug("WS -> /user/{}/queue/notice", receiverEmail);
            } catch (Exception ex) {
                log.debug("WS user queue failed (no Principal or different name?), falling back to id-topic");
            }
        } else {
            log.debug("Receiver email empty; skipping /user queue, using id-topic fallback only.");
        }

        // 3) Fallback public topic keyed by numeric user id (works without WS Principal)
        try {
            broker.convertAndSend(
                    "/topic/notice.user-" + receiver.getId(),
                    Map.of("type", "MESSAGE", "threadId", threadId)
            );
            log.debug("WS -> /topic/notice.user-{}", receiver.getId());
        } catch (Exception ex) {
            log.warn("WS notice fallback failed", ex);
        }

        // 4) Optional FCM
        try {
            firebase.sendNewMessagePush(receiver, sender, m.getContent());
        } catch (Exception ex) {
            log.warn("FCM send failed (non-fatal): {}", ex.getMessage());
        }
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
