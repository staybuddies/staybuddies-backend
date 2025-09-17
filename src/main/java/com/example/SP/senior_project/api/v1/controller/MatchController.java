package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.match.MatchDto;
import com.example.SP.senior_project.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    /** Get my ranked matches (requires JWT). */
    @GetMapping
    public List<MatchDto> myMatches(@AuthenticationPrincipal UserDetails ud) {
        return matchService.findMatchesFor(ud.getUsername());
    }

    /** Send a match request to another user. */
    @PostMapping("/{targetId}/request")
    public ResponseEntity<Map<String, Long>> sendRequest(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long targetId
    ) {
        Long id = matchService.sendRequest(ud.getUsername(), targetId);
        return ResponseEntity.ok(Map.of("requestId", id));
    }

    /** Accept a pending request where I am the target. */

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<Void> accept(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long requestId
    ) {
        matchService.accept(ud.getUsername(), requestId);
        return ResponseEntity.noContent().build();
    }

    /** Decline a pending request where I am the target. */
    @PostMapping("/{requestId}/decline")
    public ResponseEntity<Void> decline(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long requestId
    ) {
        matchService.decline(ud.getUsername(), requestId);
        return ResponseEntity.noContent().build();
    }
}


