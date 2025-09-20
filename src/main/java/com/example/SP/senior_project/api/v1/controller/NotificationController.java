package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.notice.NoticeDto;
import com.example.SP.senior_project.dto.push.RegisterTokenRequest;
import com.example.SP.senior_project.model.DeviceToken;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.DeviceTokenRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RoomFinderRepository userRepo;
    private final DeviceTokenRepository tokenRepo;
    private final NotificationService notiSvc;

    @PostMapping("/register-token")
    public Map<String, Object> register(@AuthenticationPrincipal UserDetails ud,
                                        @RequestBody RegisterTokenRequest req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
        }
        RoomFinder me = userRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        var existing = tokenRepo.findByToken(req.getToken()).orElse(null);
        if (existing == null) {
            var t = new DeviceToken();
            t.setUser(me);
            t.setToken(req.getToken().trim());
            t.setPlatform(req.getPlatform());
            t.setDeviceId(req.getDeviceId());
            tokenRepo.save(t);
        } else {
            existing.setUser(me);
            existing.setPlatform(req.getPlatform());
            existing.setDeviceId(req.getDeviceId());
            tokenRepo.save(existing);
        }
        return Map.of("ok", true);
    }

    @DeleteMapping("/token/{token}")
    public Map<String, Object> unregister(@PathVariable String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
        }
        tokenRepo.deleteByToken(token);
        return Map.of("ok", true);
    }

    /* ---------- list/read/unread-count ---------- */

    @GetMapping
    public List<NoticeDto> list(@AuthenticationPrincipal UserDetails ud,
                                @RequestParam(defaultValue = "50") int limit) {
        return notiSvc.latest(ud.getUsername(), Math.max(1, Math.min(100, limit)));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal UserDetails ud) {
        return Map.of("unread", notiSvc.unreadCount(ud.getUsername()));
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@AuthenticationPrincipal UserDetails ud,
                                        @PathVariable Long id) {
        notiSvc.markRead(ud.getUsername(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAll(@AuthenticationPrincipal UserDetails ud) {
        int n = notiSvc.markAllRead(ud.getUsername());
        return Map.of("ok", true, "count", n);
    }
}
