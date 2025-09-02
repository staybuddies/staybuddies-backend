package com.example.SP.senior_project.config.jwt;

import com.example.SP.senior_project.repository.RoomFinderRepository;
import com.example.SP.senior_project.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final RoomFinderRepository roomFinderRepository;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            @Qualifier("roomFinderUserDetailsService") UserDetailsService uds,
            RoomFinderRepository roomFinderRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = uds;
        this.roomFinderRepository = roomFinderRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);
        try {
            String email = jwtUtil.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                var user = roomFinderRepository.findByEmailIgnoreCase(email).orElse(null);
                if (user == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 1) Hard-block suspended
                if (!user.isActive()) {
                    response.setStatus(423); // LOCKED
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"code\":\"SUSPENDED\",\"message\":\"Your account is suspended.\"}");
                    return;
                }

                // 2) Reject old tokens
                Integer v = jwtUtil.extractTokenVersion(token);
                if (v == null || v != user.getTokenVersion()) {
                    response.setStatus(401);
                    return;
                }

                // 3) Normal JWT validation
                UserDetails ud = userDetailsService.loadUserByUsername(email);
                if (jwtUtil.validateToken(token, ud)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            ud, null, ud.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.JwtException | UsernameNotFoundException ex) {
            // invalid/expired/etc — just proceed unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}


