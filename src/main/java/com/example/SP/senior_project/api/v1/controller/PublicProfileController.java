package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder")
public class PublicProfileController {

    private final RoomFinderRepository repo;
    private final PublicProfileService profiles;

    @GetMapping("/public")
    public List<RoomFinderPublicDto> featured(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "size", defaultValue = "8") int size,
            @RequestParam(name = "excludeSelf", defaultValue = "true") boolean excludeSelf
    ) {
        final Long selfId = (ud != null && excludeSelf)
                ? repo.findByEmailIgnoreCase(ud.getUsername()).map(RoomFinder::getId).orElse(null)
                : null;

        // pull a little extra in case we filter self
        int fetch = Math.max(1, size + 2);
        Pageable pg = PageRequest.of(0, fetch, Sort.by(Sort.Direction.DESC, "createdAt"));

        return repo.findByActiveTrue(pg).getContent().stream()
                .filter(r -> selfId == null || !Objects.equals(r.getId(), selfId))
                .limit(size)
                .map(profiles::toPublicDto)   // <-- FIX: use service mapper
                .toList();
    }

    @GetMapping("/{id}/public")
    public RoomFinderPublicDto publicProfile(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id
    ) {
        String me = (ud != null) ? ud.getUsername() : null;
        return profiles.view(me, id);
    }
}
