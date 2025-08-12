package com.example.SP.senior_project.service;

import com.example.SP.senior_project.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("adminUserDetailsService")
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        var admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));
        return User.withUsername(admin.getEmail())
                .password(admin.getPassword())
                .roles("ADMIN")
                .build();
    }
}
