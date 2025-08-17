package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.api.login.LoginRequest;
import com.example.SP.senior_project.dto.api.login.LoginResponse;
import com.example.SP.senior_project.util.JwtUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthApiController {

    private final AuthenticationManager apiAuthManager;
    private final UserDetailsService roomFinderUserDetails;
    private final JwtUtil jwtUtil;

    public AuthApiController(
            @Qualifier("apiAuthManager") AuthenticationManager apiAuthManager,
            @Qualifier("roomFinderUserDetailsService") UserDetailsService roomFinderUserDetails,
            JwtUtil jwtUtil
    ) {
        this.apiAuthManager = apiAuthManager;
        this.roomFinderUserDetails = roomFinderUserDetails;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginRequest req) {
        apiAuthManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        var ud = roomFinderUserDetails.loadUserByUsername(req.getEmail());
        var jwt = jwtUtil.generateToken(ud);
        return ResponseEntity.ok(new LoginResponse(jwt));
    }
}