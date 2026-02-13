package com.notifyhub.service;

import com.notifyhub.dto.AuthRequest;
import com.notifyhub.dto.AuthResponse;
import com.notifyhub.dto.RegisterRequest;
import com.notifyhub.model.User;
import com.notifyhub.repository.UserRepository;
import com.notifyhub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Username already exists"));
                    }
                    return userRepository.existsByEmail(request.getEmail());
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Email already exists"));
                    }
                    User user = User.builder()
                            .username(request.getUsername())
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role("USER")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(user);
                })
                .map(user -> {
                    String token = jwtUtil.generateToken(user);
                    log.info("User registered: {}", user.getUsername());
                    return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
                });
    }

    public Mono<AuthResponse> login(AuthRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new RuntimeException("Invalid password"));
                    }
                    String token = jwtUtil.generateToken(user);
                    log.info("User logged in: {}", user.getUsername());
                    return Mono.just(new AuthResponse(token, user.getUsername(), user.getRole(), user.getId()));
                });
    }
}
