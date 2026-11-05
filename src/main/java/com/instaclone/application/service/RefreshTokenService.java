package com.instaclone.application.service;

import com.instaclone.application.dto.response.TokenResponse;
import com.instaclone.domain.entity.RefreshToken;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.RefreshTokenRepository;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import com.instaclone.presentation.advice.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    @Transactional
    public String createRefreshToken(Long userId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);
        Instant expiresAt = clock.instant().plus(7, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (token.getExpiresAt().isBefore(clock.instant())) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        if (token.isRevoked()) {
            // Replay detection: Revoked token was submitted again! Revoke all tokens for this user immediately!
            refreshTokenRepository.deleteAllByUserId(token.getUserId());
            throw new BadCredentialsException("Compromised refresh token reused. All sessions revoked.");
        }

        // Mark old token as revoked (rotation)
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        // Generate new refresh token
        String newRefreshToken = createRefreshToken(token.getUserId());

        // Find user to issue new access token
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + token.getUserId()));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
        String newAccessToken = jwtTokenService.generateAccessToken(auth, user.getId());

        return new TokenResponse(newAccessToken, newRefreshToken, "Bearer", 900);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
