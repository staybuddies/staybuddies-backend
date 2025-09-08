package com.example.SP.senior_project.api.v1.controller;

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
@RequestMapping("/api/v1/me")
public class MeController {
    private final RoomFinderRepository userRepo;

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal UserDetails ud) {
        RoomFinder me = userRepo.findByEmail(ud.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return Map.of(
                "id", me.getId(),
                "name", me.getName()
        );
    }
}
