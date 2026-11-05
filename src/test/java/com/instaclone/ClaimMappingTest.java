package com.instaclone;

import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClaimMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Chương 7 §3.2: Ánh xạ claim 'roles' thành GrantedAuthority - ROLE_MODERATOR được truy cập /moderation/reports -> 200")
    void moderatorRoleMappedSuccessfully() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "mod_alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_MODERATOR"))
        );
        String modToken = jwtTokenService.generateAccessToken(auth, 4L);

        mockMvc.perform(get("/api/v1/moderation/reports")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Chương 7 §3.2: User chỉ có ROLE_USER bị từ chối truy cập /moderation/reports -> 403 Forbidden")
    void userRoleForbiddenFromModeration() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "bob",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String userToken = jwtTokenService.generateAccessToken(auth, 2L);

        mockMvc.perform(get("/api/v1/moderation/reports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chương 7 Bẫy 2 (L2463): JwtGrantedAuthoritiesConverter mặc định chỉ đọc 'scope/scp' sinh SCOPE_*, bỏ qua claim 'roles'")
    void defaultJwtGrantedAuthoritiesConverterIgnoresRolesClaim() {
        Jwt jwt = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(900),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "charlie",
                        "roles", List.of("ROLE_USER", "ROLE_MODERATOR"),
                        "scope", "read write"
                )
        );

        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

        List<String> authorityStrings = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        // Mặc định sinh SCOPE_read và SCOPE_write
        assertThat(authorityStrings).containsExactlyInAnyOrder("SCOPE_read", "SCOPE_write");

        // Không hề chứa bất kỳ ROLE_ nào từ claim 'roles'
        assertThat(authorityStrings).doesNotContain("ROLE_USER", "ROLE_MODERATOR");
    }
}
