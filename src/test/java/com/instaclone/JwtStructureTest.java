package com.instaclone;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtStructureTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Chương 7 §2.1 & Bẫy 3: Cấu trúc JWT chuẩn (Header, Payload, Signature), RS256, typ=JWT, không chứa email/password, exp=15 phút")
    void testJwtStructureAndClaims() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        String token = jwtTokenService.generateAccessToken(auth, 100L);

        // 1. Token có đúng 3 phần phân tách bằng dấu chấm
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // 2. Kiểm tra Header
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JsonNode headerNode = objectMapper.readTree(headerJson);
        assertThat(headerNode.get("alg").asText()).isEqualTo("RS256");
        assertThat(headerNode.get("typ").asText()).isEqualTo("JWT");

        // 3. Kiểm tra Payload (Claims)
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode payloadNode = objectMapper.readTree(payloadJson);

        assertThat(payloadNode.get("sub").asText()).isEqualTo("alice");
        assertThat(payloadNode.get("iss").asText()).isEqualTo("https://api.instaclone.com");
        assertThat(payloadNode.get("userId").asLong()).isEqualTo(100L);
        assertThat(payloadNode.get("jti")).isNotNull();
        assertThat(payloadNode.get("roles").isArray()).isTrue();
        assertThat(payloadNode.get("roles").get(0).asText()).isEqualTo("ROLE_USER");

        // Bẫy 3: Tuyệt đối KHÔNG chứa email hoặc password trong Payload
        assertThat(payloadNode.has("email")).isFalse();
        assertThat(payloadNode.has("password")).isFalse();
        assertThat(payloadNode.has("phoneNumber")).isFalse();

        // 4. Thời hạn sống: exp - iat = 15 phút (900 giây)
        long iat = payloadNode.get("iat").asLong();
        long exp = payloadNode.get("exp").asLong();
        assertThat(exp - iat).isEqualTo(900L);
    }
}
