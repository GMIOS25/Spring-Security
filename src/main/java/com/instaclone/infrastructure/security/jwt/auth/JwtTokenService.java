package com.instaclone.infrastructure.security.jwt.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    public static final String ISSUER = "https://api.instaclone.com";
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    /**
     * Issue a short-lived (15 minutes) Stateless Access Token.
     * Contains only necessary metadata: sub, iss, iat, exp, jti, userId, roles.
     * Does NOT include sensitive user info like email or phone (Bẫy 3).
     */
    public String generateAccessToken(Authentication authentication, Long userId) {
        Instant now = clock.instant();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return generateToken(authentication.getName(), userId, roles, now, now.plus(15, ChronoUnit.MINUTES));
    }

    public String generateToken(String username, Long userId, List<String> roles, Instant issuedAt, Instant expiresAt) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .keyId(JwtAuthConfig.KEY_ID)
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
