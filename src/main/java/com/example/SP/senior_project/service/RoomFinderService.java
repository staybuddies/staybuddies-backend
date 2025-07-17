package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.Admin;
import com.example.SP.senior_project.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomFinderService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(admin.getEmail())
                .password(admin.getPassword())
                .roles("ADMIN")   // adjust roles as needed
                .build();
    }
}