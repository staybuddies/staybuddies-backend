package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.roomfinder.RoomFinderPublicDto;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder")
class PublicProfileController {
    private final RoomFinderRepository repo;

    @GetMapping("/{id}/public")
    public RoomFinderPublicDto publicView(@PathVariable Long id) {
        var u = repo.findById(id).orElseThrow();
        var dto = new RoomFinderPublicDto();
        dto.setId(u.getId()); dto.setName(u.getName()); dto.setGender(u.getGender());
        dto.setAge(u.getAge()); dto.setLocation(u.getLocation()); dto.setUniversity(u.getUniversity());
        return dto;
    }
}