package com.instaclone.presentation.controller;

import com.instaclone.application.dto.request.LoginRequest;
import com.instaclone.application.dto.request.RefreshTokenRequest;
import com.instaclone.application.dto.response.TokenResponse;
import com.instaclone.application.service.RefreshTokenService;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import com.instaclone.infrastructure.security.jwt.resource.JtiBlacklist;
import com.instaclone.presentation.advice.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthTokenController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JtiBlacklist jtiBlacklist;

    public AuthTokenController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            JtiBlacklist jtiBlacklist) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jtiBlacklist = jtiBlacklist;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.username()));

        String accessToken = jwtTokenService.generateAccessToken(auth, user.getId());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, "Bearer", 900));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = refreshTokenService.rotateRefreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {
        Long userId = null;
        String jti = null;
        Instant exp = null;

        if (jwt != null) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Number number) {
                userId = number.longValue();
            }
            jti = jwt.getId();
            exp = jwt.getExpiresAt();
        }

        if (userId == null && authentication != null) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        if (userId != null) {
            refreshTokenService.revokeAllForUser(userId);
        }

        if (jti != null && exp != null) {
            jtiBlacklist.blacklist(jti, exp);
        }

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}
