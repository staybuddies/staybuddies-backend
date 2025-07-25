package com.example.SP.senior_project.service;

import com.example.SP.senior_project.dto.admin.roomfinder.RoomFinderDto;
import com.example.SP.senior_project.dto.admin.roomfinder.RoomFinderUpdateDto;
import com.example.SP.senior_project.mapper.RoomFinderMapper;
import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.AdminRepository;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomFinderService implements UserDetailsService {

    private final RoomFinderRepository repo;
    @Qualifier("roomFinderMapperImpl")
    private final RoomFinderMapper mapper;
    private final AdminRepository adminRepository;

//    @Autowired
//    public RoomFinderService(RoomFinderRepository repo,
//                             @Qualifier("roomFinderMapperImpl") RoomFinderMapper mapper, AdminRepository adminRepository) {
//        this.repo    = repo;
//        this.mapper  = mapper;
//        this.adminRepository = adminRepository;
//    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        return User.withUsername(admin.getEmail())
                .password(admin.getPassword())
                .roles("ADMIN")
                .build();
    }

    public RoomFinderDto findDtoByEmail(String email) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return mapper.toDto(rf);
    }

    public RoomFinderDto updatePersonalInfo(String email, RoomFinderUpdateDto dto) {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        mapper.updateFromDto(dto, rf);
        repo.save(rf);
        return mapper.toDto(rf);
    }

    // (similarly add updatePreferences and updateBehavioral if you split those DTOs)
}
