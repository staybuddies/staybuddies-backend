package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder")
class PublicProfileController {

    private final PublicProfileService svc;

    @GetMapping("/{id}/public")
    public RoomFinderPublicDto publicProfile(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id) {
        String me = (ud != null) ? ud.getUsername() : null;
        return svc.view(me, id);
    }
}