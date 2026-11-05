package com.instaclone;

import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private RSAPrivateKey rsaPrivateKey;

    private String createValidToken() {
        var auth = new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return jwtTokenService.generateAccessToken(auth, 1L);
    }

    @Test
    @DisplayName("H6 - Ca 1: Sửa đổi Payload nhưng giữ nguyên chữ ký -> 401 Unauthorized")
    void tamperedPayloadRejected() throws Exception {
        String validToken = createValidToken();
        String[] parts = validToken.split("\\.");

        // Giả mạo payload: thay đổi alice thành bob
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tamperedPayloadJson = payloadJson.replace("\"alice\"", "\"attacker_hacker\"");
        String tamperedPayloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayloadJson.getBytes(StandardCharsets.UTF_8));

        String tamperedToken = parts[0] + "." + tamperedPayloadEncoded + "." + parts[2];

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H6 - Ca 2: Token dùng thuật toán alg=none -> 401 Unauthorized")
    void algNoneRejected() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://api.instaclone.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", 1L)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        PlainJWT plainJWT = new PlainJWT(claimsSet);
        String unsignedToken = plainJWT.serialize();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + unsignedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H6 - Ca 3: Token ký bằng khóa RSA ngoại lai khác -> 401 Unauthorized")
    void differentRsaKeyRejected() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair otherKeyPair = kpg.generateKeyPair();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://api.instaclone.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", 1L)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(otherKeyPair.getPrivate()));
        String foreignToken = signedJWT.serialize();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H6 - Ca 4: Token hết hạn quá clockSkew (60s) -> 401 Unauthorized")
    void expiredTokenRejected() throws Exception {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://api.instaclone.com")
                .issueTime(Date.from(past))
                .expirationTime(Date.from(past.plus(15, ChronoUnit.MINUTES))) // Hết hạn cách đây hơn 1 giờ
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", 1L)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(rsaPrivateKey));
        String expiredToken = signedJWT.serialize();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H6 - Ca 5: Sai issuer (iss) -> 401 Unauthorized")
    void invalidIssuerRejected() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://evil-fake-issuer.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", 1L)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(com.nimbusds.jose.JOSEObjectType.JWT).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(rsaPrivateKey));
        String fakeIssuerToken = signedJWT.serialize();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + fakeIssuerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("H6 - Ca 6: Token có typ khác JWT hoặc at+jwt -> 401 Unauthorized")
    void invalidTypeHeaderRejected() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("alice")
                .issuer("https://api.instaclone.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", 1L)
                .claim("roles", List.of("ROLE_USER"))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(new com.nimbusds.jose.JOSEObjectType("INVALID_TYPE")).build(),
                claimsSet
        );
        signedJWT.sign(new RSASSASigner(rsaPrivateKey));
        String invalidTypeToken = signedJWT.serialize();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + invalidTypeToken))
                .andExpect(status().isUnauthorized());
    }
}
