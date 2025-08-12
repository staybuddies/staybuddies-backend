package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.roomfinder.BehavioralDto;
import com.example.SP.senior_project.dto.roomfinder.PreferencesDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.roomfinder.RoomFinderUpdateDto;
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

        // encode only when client actually wants to change it
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            rf.setPassword(passwordEncoder.encode(dto.getPassword()));
            dto.setPassword(null); // prevent mapper from overwriting encoded value
        }

        mapper.updateFromDto(dto, rf); // thanks to IGNORE, nulls won't clobber fields
        repo.save(rf);
        return mapper.toDto(rf); // no password in response anymore
    }

    public PreferencesDto updatePreferences(String email, PreferencesDto dto) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        mapper.updateFromPreferences(dto, rf);
        repo.save(rf);
        return mapper.toPreferencesDto(rf);
    }

    public BehavioralDto updateBehavioral(String email, BehavioralDto dto) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        mapper.updateFromBehavioral(dto, rf);
        repo.save(rf);
        return mapper.toBehavioralDto(rf);
    }
}
