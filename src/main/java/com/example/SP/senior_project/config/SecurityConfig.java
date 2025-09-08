package com.example.SP.senior_project.config;

import com.example.SP.senior_project.config.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService roomFinderDetails;
    private final UserDetailsService adminDetails;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            @Qualifier("roomFinderUserDetailsService") UserDetailsService roomFinderDetails,
            @Qualifier("adminUserDetailsService") UserDetailsService adminDetails,
            PasswordEncoder passwordEncoder,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.roomFinderDetails = roomFinderDetails;
        this.adminDetails = adminDetails;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /* --------- providers --------- */
    @Bean
    public DaoAuthenticationProvider roomFinderAuthProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(roomFinderDetails);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    @Bean
    public DaoAuthenticationProvider adminAuthProvider() {
        var p = new DaoAuthenticationProvider();
        p.setUserDetailsService(adminDetails);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    /* --------- per-chain AuthenticationManagers (named) --------- */
    @Bean("apiAuthManager")
    @Primary                                          // <-- make ONE manager primary
    public AuthenticationManager apiAuthManager() {
        return new ProviderManager(List.of(roomFinderAuthProvider()));
    }

    @Bean("adminAuthManager")
    public AuthenticationManager adminAuthManager() {
        return new ProviderManager(List.of(adminAuthProvider()));
    }

    /* --------- CORS for API --------- */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setAllowCredentials(true);
        var src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    @Order(0)
    public SecurityFilterChain staticResources(HttpSecurity http) throws Exception {
        http.securityMatcher("/uploads/**", "/files/**", "/favicon.ico")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(r -> r.disable())
                .securityContext(s -> s.disable());

        return http.build();
    }

    /* --------- API chain (/api/**) --------- */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/room-finder").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/authenticate").permitAll()
                        .requestMatchers("/api/v1/verify-email/**").permitAll()              // <-- change here
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/room-finder/public",
                                "/api/v1/room-finder/*/public").permitAll()
                        .requestMatchers("/api/v1/verifications/student-id/**").authenticated()
                        .requestMatchers("/api/v1/matches/**", "/api/v1/messages/**").authenticated()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers("/api/v1/room-finder/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationManager(apiAuthManager())  // use API manager explicitly
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /* --------- MVC chain (admin pages) --------- */
    @Bean
    @Order(2)
    public SecurityFilterChain mvcSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/login", "/register", "/logout",
                        "/admins/**", "/css/**", "/js/**", "/images/**", "/uploads/**","/files/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**","/uploads/**", "/files/**").permitAll()
                        .requestMatchers("/admins/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .authenticationManager(adminAuthManager()) // use ADMIN manager explicitly
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/admins", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                );

        return http.build();
    }
}
