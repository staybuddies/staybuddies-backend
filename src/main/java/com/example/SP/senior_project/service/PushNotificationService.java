// src/main/java/com/example/SP/senior_project/service/PushNotificationService.java
package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.Message;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.constant.NoticeType;
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

    private final SimpMessagingTemplate broker;
    private final FirebaseFacade firebase;
    private final NotificationService notiSvc;

    /* -------------------- Message (existing) -------------------- */
    public void notifyNewMessage(RoomFinder receiver,
                                 RoomFinder sender,
                                 Long threadId,
                                 Message m) {
        final String iso = (m.getCreatedAt() == null)
                ? OffsetDateTime.now(ZoneOffset.UTC).toString()
                : m.getCreatedAt().atOffset(ZoneOffset.UTC).toString();

        final Map<String, Object> evt = Map.of(
                "type", "MESSAGE",
                "threadId", threadId,
                "id", m.getId(),
                "senderId", sender.getId(),
                "text", String.valueOf(m.getContent()),
                "time", iso
        );

        // Thread stream
        safe(() -> broker.convertAndSend("/topic/threads/" + threadId, evt));

        // Per-user queue (best)
        safe(() -> broker.convertAndSendToUser(
                safeTrim(receiver.getEmail()), "/queue/notice", Map.of("type", "MESSAGE", "threadId", threadId)));

        // Fallback public topic
        safe(() -> broker.convertAndSend("/topic/notice.user-" + receiver.getId(),
                Map.of("type", "MESSAGE", "threadId", threadId)));

        // Persist + FCM
        var n = notiSvc.saveNotice(receiver, NoticeType.MESSAGE, threadId, sender,
                sender.getName(), m.getContent());
        firebase.sendNewMessagePush(receiver, sender, m.getContent(), threadId);
        log.debug("Notice saved MESSAGE id={}", n.getId());
    }

    /* -------------------- Match requested -------------------- */
    public void notifyMatchRequested(RoomFinder target, RoomFinder requester) {
        String title = "New match request";
        String body = (requester != null && requester.getName() != null)
                ? requester.getName() + " sent you a match request"
                : "You have a new match request";

        // Persist
        var n = notiSvc.saveNotice(target, NoticeType.MATCH_REQUESTED, null, requester, title, body);

        // WS user queue + fallback topic
        var payload = Map.of(
                "id", n.getId(),
                "type", "MATCH_REQUESTED",
                "fromUserId", requester == null ? null : requester.getId(),
                "fromName", requester == null ? null : requester.getName(),
                "title", title,
                "body", body,
                "createdAt", n.getCreatedAt().toString()
        );
        safe(() -> broker.convertAndSendToUser(safeTrim(target.getEmail()), "/queue/notice", payload));
        safe(() -> broker.convertAndSend("/topic/notice.user-" + target.getId(), payload));

        // FCM
        firebase.sendMatchRequestPush(target, requester);
    }


    /* -------------------- Match accepted -------------------- */
    public void notifyMatchAccepted(RoomFinder requester, RoomFinder target) {
        String title = "Request accepted";
        String body = (target != null && target.getName() != null)
                ? target.getName() + " accepted your match request"
                : "Your match request was accepted";

        var n = notiSvc.saveNotice(requester, NoticeType.MATCH_ACCEPTED, null, target, title, body);

        var payload = Map.of(
                "id", n.getId(),
                "type", "MATCH_ACCEPTED",
                "fromUserId", target == null ? null : target.getId(),
                "fromName", target == null ? null : target.getName(),
                "title", title,
                "body", body,
                "createdAt", n.getCreatedAt().toString()
        );
        safe(() -> broker.convertAndSendToUser(safeTrim(requester.getEmail()), "/queue/notice", payload));
        safe(() -> broker.convertAndSend("/topic/notice.user-" + requester.getId(), payload));

        firebase.sendMatchAcceptedPush(requester, target);
    }

    /* -------------------- util -------------------- */
    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (Exception ignored) {
        }
    }
}
