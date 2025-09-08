package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder")
class PublicProfileController {

    private final PublicProfileService svc;
    @GetMapping("/public")
    public Page<RoomFinderPublicDto> featured(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        return svc.featured(Math.max(0, page), Math.max(1, size));
    }

    @GetMapping("/{id}/public")
    public RoomFinderPublicDto publicProfile(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id) {
        String me = (ud != null) ? ud.getUsername() : null;
        return svc.view(me, id);
    }
}