package com.uade.auth.controller;

import com.uade.auth.dto.LoginRequest;
import com.uade.auth.dto.TokenResponse;
import com.uade.auth.entity.User;
import com.uade.auth.repository.UserRepository;
import com.uade.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .map(user -> {
                    List<String> roles = user.getRoles().stream()
                            .map(r -> "ROLE_" + r.getName())
                            .toList();
                    String token = jwtService.generateToken(user.getUsername(), roles);
                    return ResponseEntity.ok(new TokenResponse(token, "Bearer", 3600));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}
