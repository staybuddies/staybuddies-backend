package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.message.ConversationDto;
import com.example.SP.senior_project.dto.message.MessageDto;
import com.example.SP.senior_project.model.Message;
import com.example.SP.senior_project.model.MessageThread;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.MessageRepository;
import com.example.SP.senior_project.repository.MessageThreadRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final RoomFinderRepository userRepo;
    private final MessageThreadRepository threadRepo;
    private final MessageRepository messageRepo;
    private final PushNotificationService pushSvc;

    /* ---------------------- list my conversations ---------------------- */
    @Transactional(readOnly = true)
    public List<ConversationDto> threadsFor(String email) {
        RoomFinder me = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return threadRepo.findAllForUser(me.getId()).stream()
                .map(t -> toConversationDto(t, me))
                .toList();
    }

    /* ---------------------- helpers ---------------------- */
    private RoomFinder me(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /* ---------------------- list threads (with eager user info) ---------------------- */
    @Transactional(readOnly = true)
    public List<ConversationDto> list(String email) {
        var me = me(email);
        var threads = threadRepo.findAllForUserWithUsers(email);
        return threads.stream().map(t -> toConversationDto(t, me)).toList();
    }

    /* ---------------------- ensure/get thread by OTHER USER ID ---------------------- */
    @Transactional
    public Long ensureThreadByEmail(String myEmail, Long otherId) {
        RoomFinder me = userRepo.findByEmail(myEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        RoomFinder other = userRepo.findById(otherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        long a = Math.min(me.getId(), other.getId());
        long b = Math.max(me.getId(), other.getId());

        return threadRepo.findBetween(a, b)
                .map(MessageThread::getId)
                .orElseGet(() -> {
                    var t = new MessageThread();
                    t.setUser1(userRepo.getReferenceById(a));
                    t.setUser2(userRepo.getReferenceById(b));
                    return threadRepo.save(t).getId();
                });
    }

    /* ---------------------- ensure/get thread by two IDs ---------------------- */
    @Transactional(readOnly = true)
    public Long findExistingThreadId(Long aId, Long bId) {
        long a = Math.min(aId, bId);
        long b = Math.max(aId, bId);
        return threadRepo.findBetween(a, b).map(MessageThread::getId).orElse(null);
    }

    @Transactional
    public Long ensureThread(Long aId, Long bId) {
        long a = Math.min(aId, bId);
        long b = Math.max(aId, bId);
        return threadRepo.findBetween(a, b)
                .map(MessageThread::getId)
                .orElseGet(() -> {
                    var t = new MessageThread();
                    t.setUser1(userRepo.getReferenceById(a));
                    t.setUser2(userRepo.getReferenceById(b));
                    return threadRepo.save(t).getId();
                });
    }

    /* ---------------------- messages in a thread I participate in ---------------------- */
    @Transactional(readOnly = true)
    public List<MessageDto> messages(String myEmail, Long threadId) {
        RoomFinder me = userRepo.findByEmail(myEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        MessageThread t = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!participant(t, me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your thread");

        return messageRepo.findByThreadOrderByCreatedAtAsc(t).stream()
                .map(m -> toMessageDto(m, me.getId()))
                .toList();
    }

    /* ---------------------- send message (+ push) ---------------------- */
    @Transactional
    public MessageDto send(String myEmail, Long threadId, String content) {
        if (content == null || content.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content required");

        RoomFinder me = userRepo.findByEmail(myEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        MessageThread t = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!participant(t, me.getId())) {
            Long other = t.getUser1().getId().equals(me.getId()) ? t.getUser2().getId() : t.getUser1().getId();
            throw new ThreadNotOwned(other);
        }

        var m = new Message();
        m.setThread(t);
        m.setSender(me);
        m.setContent(content.trim());
        m.setReadByOther(false);
        m = messageRepo.save(m);

        // receiver for notification
        RoomFinder receiver = t.getUser1().getId().equals(me.getId()) ? t.getUser2() : t.getUser1();

        // async push (FCM + WebSocket)
        pushSvc.notifyNewMessage(receiver, me, t.getId(), m);

        return toMessageDto(m, me.getId());
    }

    /* ---------------------- mark read ---------------------- */
    @Transactional
    public int markRead(String myEmail, Long threadId) {
        RoomFinder me = userRepo.findByEmail(myEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        MessageThread t = threadRepo.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!participant(t, me.getId())) {
            Long other = t.getUser1().getId().equals(me.getId())
                    ? t.getUser2().getId() : t.getUser1().getId();
            throw new ThreadNotOwned(other);
        }

        return messageRepo.markReadByOther(threadId, me.getId());
    }

    /* ---------------------- mapping helpers ---------------------- */
    private boolean participant(MessageThread t, Long uid) {
        return t.getUser1().getId().equals(uid) || t.getUser2().getId().equals(uid);
    }

    private ConversationDto toConversationDto(MessageThread t, RoomFinder me) {
        var other = t.getUser1().getId().equals(me.getId()) ? t.getUser2() : t.getUser1();
        var last = messageRepo.findTopByThreadOrderByCreatedAtDesc(t).orElse(null);
        long unread = messageRepo.countByThreadAndReadByOtherFalseAndSender_IdNot(t, me.getId());

        var dto = new ConversationDto();
        dto.setId(t.getId());
        dto.setOtherId(other.getId());
        dto.setOtherName(other.getName());
        dto.setOtherGender(other.getGender());
        dto.setOtherLocation(other.getLocation());
        if (last != null) {
            dto.setLastMessage(last.getContent());
            dto.setLastTime(last.getCreatedAt().toString());
        }
        dto.setUnread(unread);
        return dto;
    }

    private MessageDto toMessageDto(Message m, Long myId) {
        var dto = new MessageDto();
        dto.setId(m.getId());
        dto.setFromMe(m.getSender().getId().equals(myId));
        dto.setText(m.getContent());
        dto.setTime(m.getCreatedAt().toString());
        return dto;
    }

    /* ---------------------- error type used by controller ---------------------- */
    @Getter
    @Setter
    public static class ThreadNotOwned extends RuntimeException {
        private final Long otherUserId;

        public ThreadNotOwned(Long otherUserId) {
            this.otherUserId = otherUserId;
        }
    }
}
