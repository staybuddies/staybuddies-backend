package com.example.SP.senior_project.service;

import com.example.SP.senior_project.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service("adminUserDetailsService")
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {
    private final AdminRepository admins;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var a = admins.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No admin: " + email));

        return User.withUsername(a.getEmail())
                .password(a.getPassword())   // BCrypt
                .roles("ADMIN")
                .build();
    }
}
