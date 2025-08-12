package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.api.login.LoginRequest;
import com.example.SP.senior_project.dto.api.login.LoginResponse;
import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthApiController {

    @Autowired
    private final AuthenticationManager authenticationManager;
    @Autowired
    private final JwtUtil jwtUtil;
    @Qualifier("roomFinderUserDetailsService")
    private final UserDetailsService userDetailsService;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final RoomFinderRepository roomFinderRepository;

    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponse> createAuthenticationToken(@RequestBody LoginRequest loginRequest) {
        // This will call our DAO provider -> UserDetailsService -> DB
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
        String jwt = jwtUtil.generateToken(userDetails);
        return ResponseEntity.ok(new LoginResponse(jwt));
    }

//    @PostMapping("/register")
//    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
//        RoomFinder user = new RoomFinder();
//        user.setName(req.getName());
//        user.setEmail(req.getEmail());
//        user.setPassword(passwordEncoder.encode(req.getPassword())); // BCrypt!
//        user.setPhone(req.getPhone());
//        roomFinderRepository.save(user);
//        return ResponseEntity.status(201).build();
//    }
}
