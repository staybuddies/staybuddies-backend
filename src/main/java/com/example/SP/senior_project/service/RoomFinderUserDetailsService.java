package com.example.SP.senior_project.service;

import com.example.SP.senior_project.model.RoomFinder;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service("roomFinderUserDetailsService")
@RequiredArgsConstructor
public class RoomFinderUserDetailsService implements UserDetailsService {

    private final RoomFinderRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        RoomFinder rf = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(rf.getEmail())
                .password(rf.getPassword())
                .roles("USER")
                .disabled(!rf.isActive())
                .build();
    }
}
