package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.push.RegisterTokenRequest;
import com.example.SP.senior_project.model.DeviceToken;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.DeviceTokenRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RoomFinderRepository userRepo;
    private final DeviceTokenRepository tokenRepo;

    @PostMapping("/register-token")
    public Map<String, Object> register(@AuthenticationPrincipal UserDetails ud,
                                        @RequestBody RegisterTokenRequest req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
        }
        RoomFinder me = userRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // upsert by token
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
    public Map<String, Object> unregister(@AuthenticationPrincipal UserDetails ud,
                                          @PathVariable String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
        }
        tokenRepo.deleteByToken(token);
        return Map.of("ok", true);
    }
}
