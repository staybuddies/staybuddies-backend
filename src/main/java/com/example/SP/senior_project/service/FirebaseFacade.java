package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.RoomFinder;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseFacade {

    private final FirebaseMessaging messaging;

    /**
     * Send a push for a new chat message.
     * This method tries to read receiver.getFcmToken() if present.
     * If you already have a 'fcmToken' field on RoomFinder, it will be used.
     * If not, this method just no-ops (no compile error).
     */
    public void sendNewMessagePush(RoomFinder receiver, RoomFinder sender, String preview) {
        if (messaging == null) {
            log.debug("FirebaseMessaging not configured; skip FCM");
            return;
        }

        // Best-effort: read token via reflection so we don't require a compile-time field.
        String token = null;
        try {
            var m = receiver.getClass().getMethod("getFcmToken");
            Object v = m.invoke(receiver);
            token = (v == null) ? null : v.toString();
        } catch (Exception ignore) {
            log.debug("No fcmToken on RoomFinder; skipping FCM for receiver {}", receiver.getId());
        }

        if (token == null || token.isBlank()) return;

        String title = (sender.getName() == null || sender.getName().isBlank())
                ? "New message"
                : sender.getName();
        String body = preview == null ? "" :
                (preview.length() > 120 ? preview.substring(0, 117) + "…" : preview);

        try {
            Message msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "MESSAGE")
                    .putData("senderId", String.valueOf(sender.getId()))
                    .putData("receiverId", String.valueOf(receiver.getId()))
                    .build();

            String id = messaging.send(msg);
            log.debug("FCM sent: {}", id);
        } catch (Exception e) {
            log.warn("FCM send failed: {}", e.getMessage());
        }
    }
}
