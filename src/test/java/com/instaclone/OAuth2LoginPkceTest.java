package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2LoginPkceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("H7: GET /oauth2/authorization/github sinh 302 với PKCE S256 và redirect_uri chuẩn")
    void testOAuth2AuthorizationRequestWithPkce() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        assertThat(location).startsWith("https://github.com/login/oauth/authorize");

        String decodedUrl = URLDecoder.decode(location, StandardCharsets.UTF_8);

        // 1. Bắt buộc có PKCE (code_challenge & code_challenge_method=S256)
        assertThat(decodedUrl).contains("code_challenge=");
        assertThat(decodedUrl).contains("code_challenge_method=S256");

        // 2. Chống CSRF qua state parameter
        assertThat(decodedUrl).contains("state=");

        // 3. Chuẩn OAuth 2.1: Bắt buộc Exact Match Redirect URI
        assertThat(decodedUrl).contains("redirect_uri=http://localhost/login/oauth2/code/github");

        // 4. Khai tử Implicit Grant: Nghiêm cấm response_type=token
        assertThat(decodedUrl).doesNotContain("response_type=token");
        assertThat(decodedUrl).contains("response_type=code");
    }

    @Test
    @DisplayName("OAuth2 Principal đăng nhập thành công có thể truy cập /api/v1/users/me")
    void testOAuth2UserAccessProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .with(oauth2Login().attributes(attrs -> {
                            attrs.put("login", "octocat_developer");
                            attrs.put("name", "Octo Cat");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").isString());
    }
}
