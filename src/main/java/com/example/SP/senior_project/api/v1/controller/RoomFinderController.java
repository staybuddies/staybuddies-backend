package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.admin.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.admin.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.mapper.RoomFinderMapper;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.RoomFinderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder")
public class RoomFinderController {
    @Autowired
    private final RoomFinderService roomFinderService;
    @Autowired
    private final RoomFinderRepository repo;
    @Qualifier("roomFinderMapperImpl")
    @Autowired
    private final RoomFinderMapper mapper;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public RoomFinderDto me(@AuthenticationPrincipal UserDetails ud) {
        return roomFinderService.findDtoByEmail(ud.getUsername());
    }

    @PutMapping("/me")
    public RoomFinderDto saveMe(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody RoomFinderUpdateDto dto
    ) {
        return roomFinderService.updatePersonalInfo(ud.getUsername(), dto);
    }

    // If you want full admin CRUD on all RoomFinders:
    @GetMapping
    public List<RoomFinderDto> all() {
        return repo.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    public RoomFinderDto create(@RequestBody RoomFinderUpdateDto dto) {
        RoomFinder rf = new RoomFinder();

        // Copy values from DTO
        mapper.updateFromDto(dto, rf);

        // Handle missing joinDate (if DTO doesn't provide one)
        rf.setJoinDate(dto.getJoinDate() != null ? dto.getJoinDate() : LocalDate.now());

        // Set required fields
        rf.setActive(true);

        // Default fallback if null (avoid null boolean)
        rf.setAlreadyHasRoom(dto.getAlreadyHasRoom() != null ? dto.getAlreadyHasRoom() : false);

        // Set password (encoded)
        rf.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Save and return DTO
        rf = repo.save(rf);
        return mapper.toDto(rf);
    }


    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}


