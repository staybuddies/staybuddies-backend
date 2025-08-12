package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.roomfinder.BehavioralDto;
import com.example.SP.senior_project.dto.roomfinder.PreferencesDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.mapper.RoomFinderMapper;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.service.RoomFinderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-finder")
public class RoomFinderController {
    @Autowired
    private final RoomFinderService roomFinderService;
    @Autowired
    private final RoomFinderRepository repo;
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
    public ResponseEntity<?> create(@RequestBody RoomFinderUpdateDto dto) {
        // minimal validation (avoid NPEs)
        if (dto.getEmail() == null || dto.getPassword() == null
                || dto.getName() == null || dto.getPhone() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "name, email, password and phone are required"));
        }

        // friendly duplicate check (so we never bubble a 500)
        if (repo.existsByEmail(dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already registered"));
        }

        RoomFinder rf = new RoomFinder();
        mapper.updateFromDto(dto, rf);

        rf.setJoinDate(dto.getJoinDate() != null ? dto.getJoinDate() : LocalDate.now());
        rf.setActive(true);
        rf.setLocation(dto.getLocation());
        rf.setAlreadyHasRoom(Boolean.TRUE.equals(dto.getAlreadyHasRoom()));
        rf.setPassword(passwordEncoder.encode(dto.getPassword()));
        rf.setEmailNotification(Boolean.TRUE.equals(dto.getEmailNotification()));
        rf.setLocationSharing(Boolean.TRUE.equals(dto.getLocationSharing()));

        rf = repo.save(rf);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(rf));
    }

    @PutMapping("/me/preferences")
    public PreferencesDto savePreferences(@AuthenticationPrincipal UserDetails ud,
                                          @RequestBody PreferencesDto dto) {
        return roomFinderService.updatePreferences(ud.getUsername(), dto);
    }

    @PutMapping("/me/behavioral")
    public BehavioralDto saveBehavioral(@AuthenticationPrincipal UserDetails ud,
                                        @RequestBody BehavioralDto dto) {
        return roomFinderService.updateBehavioral(ud.getUsername(), dto);
    }


    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}


