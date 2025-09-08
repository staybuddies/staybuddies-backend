package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.push.RegisterTokenRequest;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/push")
public class PushTokenController {
    private final RoomFinderRepository userRepo;

    @PostMapping("/token")
    public Map<String, String> register(@AuthenticationPrincipal UserDetails ud,
                                        @RequestBody RegisterTokenRequest req) {
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
        }
        RoomFinder me = userRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        me.setFcmToken(req.getToken().trim());
        userRepo.save(me);
        return Map.of("status", "ok");
    }
}
