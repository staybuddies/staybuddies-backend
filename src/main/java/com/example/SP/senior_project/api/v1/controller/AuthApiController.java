package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.api.login.LoginRequest;
import com.example.SP.senior_project.dto.api.login.LoginResponse;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.util.JwtUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthApiController {

    private final AuthenticationManager apiAuthManager;
    private final UserDetailsService roomFinderUserDetails;
    private final JwtUtil jwtUtil;
    private final RoomFinderRepository roomFinderRepository;

    public AuthApiController(
            @Qualifier("apiAuthManager") AuthenticationManager apiAuthManager,
            @Qualifier("roomFinderUserDetailsService") UserDetailsService roomFinderUserDetails,
            JwtUtil jwtUtil,
            RoomFinderRepository roomFinderRepository
    ) {
        this.apiAuthManager = apiAuthManager;
        this.roomFinderUserDetails = roomFinderUserDetails;
        this.jwtUtil = jwtUtil;
        this.roomFinderRepository = roomFinderRepository;
    }

    // AuthApiController.java
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest req) {
        try {
            apiAuthManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException ex) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("code", "SUSPENDED", "message", "Your account is suspended."));
        }

        var user = roomFinderRepository.findByEmailIgnoreCase(req.getEmail()).orElseThrow();
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("code", "SUSPENDED", "message", "Your account is suspended."));
        }

        var ud = roomFinderUserDetails.loadUserByUsername(req.getEmail());
        var jwt = jwtUtil.generateToken(ud, user.getTokenVersion());
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

}
