package com.instaclone;

import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatelessChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Chương 7 §2 & Bẫy 1: Request Bearer không sinh JSESSIONID, không tạo session, POST không cần CSRF, Bearer sai bị 401")
    void statelessChainBehavior() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtTokenService.generateAccessToken(auth, 1L);

        // 1. POST /api/v1/posts với Bearer hợp lệ -> 201 Created (KHÔNG cần token CSRF)
        MvcResult postResult = mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caption\":\"Stateless post\",\"imageUrl\":\"https://img/state.jpg\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        // 2. Không sinh Set-Cookie JSESSIONID và request.getSession(false) là null
        String setCookie = postResult.getResponse().getHeader("Set-Cookie");
        if (setCookie != null) {
            assertThat(setCookie).doesNotContain("JSESSIONID");
        }
        assertThat(postResult.getRequest().getSession(false)).isNull();

        // 3. Bearer token sai -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid.bearer.token"))
                .andExpect(status().isUnauthorized());
    }
}
