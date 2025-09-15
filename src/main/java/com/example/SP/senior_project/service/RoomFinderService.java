// src/main/java/com/example/SP/senior_project/service/RoomFinderService.java
package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.roomfinder.*;
import com.example.SP.senior_project.mapper.RoomFinderMapper;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("roomFinderService")
@RequiredArgsConstructor
public class RoomFinderService {

    private final RoomFinderRepository repo;
    private final RoomFinderMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public RoomFinderDto findDtoByEmail(String email) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return mapper.toDto(rf);
    }

    public RoomFinderDto updatePersonalInfo(String email, RoomFinderUpdateDto dto) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            rf.setPassword(passwordEncoder.encode(dto.getPassword()));
            dto.setPassword(null);
        }
        mapper.updateFromDto(dto, rf);
        repo.save(rf);
        return mapper.toDto(rf);
    }

    public PreferencesDto updatePreferences(String email, PreferencesDto dto) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        mapper.updateFromPreferences(dto, rf);
        repo.save(rf);
        return mapper.toPreferencesDto(rf);
    }

    /* NEW: read + write for behavior */
    public BehavioralDto getBehavioral(String email) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return mapper.toBehavioralDto(rf);
    }

    public BehavioralDto updateBehavioral(String email, BehavioralDto dto) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        mapper.updateFromBehavioral(dto, rf);
        repo.save(rf);
        return mapper.toBehavioralDto(rf);
    }
}
