package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.DeviceToken;
import com.example.SP.senior_project.repository.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseFacade {

    private final FirebaseMessaging messaging;          // may be null if FCM not wired
    private final DeviceTokenRepository tokenRepo;      // stores per-device web/mobile tokens

    /** Send to every token registered for the user. */
    private void sendToAll(RoomFinder receiver, String title, String body, Map<String, String> data) {
        if (messaging == null) {
            log.debug("FirebaseMessaging not configured; skip FCM");
            return;
        }
        if (receiver == null || receiver.getId() == null) return;

        // Make the element type explicit so .getToken() resolves.
        final List<DeviceToken> rows = tokenRepo.findAllByUser_Id(receiver.getId());
        final List<String> tokens = rows == null ? List.of() :
                rows.stream()
                        .map(DeviceToken::getToken)
                        .filter(t -> t != null && !t.isBlank())
                        .distinct()
                        .collect(Collectors.toList());

        if (tokens.isEmpty()) return;

        for (String t : tokens) {
            try {
                Message.Builder b = Message.builder().setToken(t);

                if (title != null || body != null) {
                    b.setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
                }
                if (data != null && !data.isEmpty()) {
                    data.forEach(b::putData);
                }

                final String id = messaging.send(b.build());
                log.debug("FCM sent to user {} token {}… ok ({})", receiver.getId(), t.substring(0, Math.min(8, t.length())), id);
            } catch (Exception e) {
                log.warn("FCM send failed for token {}: {}", t, e.getMessage());
            }
        }
    }

    /** Chat message push (kept for compatibility) */
    public void sendNewMessagePush(RoomFinder receiver, RoomFinder sender, String preview, Long threadId) {
        final String title = (sender != null && sender.getName() != null && !sender.getName().isBlank())
                ? sender.getName() : "New message";
        final String body = preview == null ? "" : (preview.length() > 120 ? preview.substring(0, 117) + "…" : preview);

        sendToAll(receiver, title, body, Map.of(
                "type", "MESSAGE",
                "threadId", threadId == null ? "" : String.valueOf(threadId),
                "senderId", sender == null || sender.getId() == null ? "" : String.valueOf(sender.getId())
        ));
    }

    /** Match request → notify target */
    public void sendMatchRequestPush(RoomFinder target, RoomFinder requester) {
        final String title = "New match request";
        final String body  = (requester != null && requester.getName() != null)
                ? requester.getName() + " sent you a match request"
                : "You have a new match request";

        sendToAll(target, title, body, Map.of(
                "type", "MATCH_REQUESTED",
                "fromUserId", requester == null || requester.getId() == null ? "" : String.valueOf(requester.getId())
        ));
    }

    /** Match accepted → notify requester */
    public void sendMatchAcceptedPush(RoomFinder requester, RoomFinder target) {
        final String title = "Request accepted";
        final String body  = (target != null && target.getName() != null)
                ? target.getName() + " accepted your match request"
                : "Your match request was accepted";

        sendToAll(requester, title, body, Map.of(
                "type", "MATCH_ACCEPTED",
                "fromUserId", target == null || target.getId() == null ? "" : String.valueOf(target.getId())
        ));
    }
}
