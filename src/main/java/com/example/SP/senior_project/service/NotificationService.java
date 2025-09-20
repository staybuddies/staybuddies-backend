// src/main/java/com/example/SP/senior_project/service/NotificationService.java
package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.notice.NoticeDto;
import com.example.SP.senior_project.model.Notice;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.model.constant.NoticeType;
import com.example.SP.senior_project.repository.NoticeRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NoticeRepository repo;
    private final RoomFinderRepository userRepo;

    private RoomFinder me(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /* --------- API ops --------- */

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return repo.countByUserAndReadFlagFalse(me(email));
    }

    @Transactional(readOnly = true)
    public List<NoticeDto> latest(String email, int limit) {
        var list = repo.findTop100ByUserOrderByCreatedAtDesc(me(email));
        if (limit > 0 && list.size() > limit) list = list.subList(0, limit);
        return list.stream().map(NotificationService::toDto).toList();
    }

    @Transactional
    public void markRead(String email, Long id) {
        var n = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!n.getUser().getEmail().equalsIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        n.setReadFlag(true);
        repo.save(n);
    }

    @Transactional
    public int markAllRead(String email) {
        return repo.markAllRead(me(email));
    }

    /* --------- write helper used by PushNotificationService --------- */
    @Transactional
    public Notice saveNotice(RoomFinder target, NoticeType type, Long threadId,
                             RoomFinder from, String title, String body) {
        var n = new Notice();
        n.setUser(target);
        n.setType(type);
        n.setThreadId(threadId);
        if (from != null) {
            n.setFromUserId(from.getId());
            n.setFromName(from.getName());
        }
        n.setTitle(title);
        n.setBody(body);
        n.setCreatedAt(OffsetDateTime.now());
        n.setReadFlag(false);
        return repo.save(n);
    }

    private static NoticeDto toDto(Notice n) {
        var d = new NoticeDto();
        d.setId(n.getId());
        d.setType(n.getType().name());
        d.setRead(n.isReadFlag());
        d.setCreatedAt(n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
        d.setThreadId(n.getThreadId());
        d.setFromUserId(n.getFromUserId());
        d.setFromName(n.getFromName());
        d.setTitle(n.getTitle());
        d.setBody(n.getBody());
        return d;
    }
}
