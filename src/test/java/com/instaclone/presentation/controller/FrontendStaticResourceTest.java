package com.instaclone.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontendStaticResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Frontend Web UI: Root path / and /index.html return 200 OK without requiring authentication")
    void testRootPathAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("Frontend Web UI: Static assets /css/app.css and /js/app.js are served publicly")
    void testStaticAssetsAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Frontend Web UI: Static avatar and post SVG assets are served publicly without authentication")
    void testStaticImageAssetsAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/images/avatars/alice.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));

        mockMvc.perform(get("/images/avatars/bob.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/avatars/charlie.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/avatars/mod_alice.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/avatars/cuong_admin.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/avatars/default.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/posts/p1.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));

        mockMvc.perform(get("/images/posts/p2.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/posts/p3.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/posts/p4.svg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/images/posts/default.svg"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Frontend API: GET /api/v1/posts returns visible posts for authenticated user")
    @WithMockUser(username = "alice", roles = {"USER"})
    void testGetFeedPostsAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Frontend API: GET /api/v1/posts without authentication is rejected with 401")
    void testGetFeedPostsAnonymousRejected() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Frontend API: GET /api/v1/csrf/token returns masked token anonymously")
    void testCsrfTokenAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/csrf/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").isString());
    }

    @Test
    @DisplayName("Frontend API: GET /ott/latest returns 200 OK")
    void testOttLatestAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/ott/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("Frontend API: Chain 1 GET /api/v1/feed/public/explore returns 200 OK anonymously")
    void testPublicExploreAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/feed/public/explore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Frontend API: GET /api/v1/auth/me returns granted authorities including factors")
    @WithMockUser(username = "alice", authorities = {"ROLE_USER", "FACTOR_PASSWORD", "FACTOR_OTT"})
    void testCurrentUserReturnsAllGrantedAuthorities() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities[?(@ == 'FACTOR_OTT')]").exists());
    }
}
