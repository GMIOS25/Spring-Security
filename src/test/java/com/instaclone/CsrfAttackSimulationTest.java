package com.instaclone;

import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfAttackSimulationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Chương 6 §1: Tấn công CSRF từ trang độc evil-giveaway.com mang session hợp lệ nhưng không có token -> 403")
    void simulateCrossSiteRequestForgeryAttack() throws Exception {
        // Alice đăng nhập thành công vào InstaClone
        LoginRequest loginRequest = new LoginRequest("alice", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // Trang web độc 'https://evil-giveaway.com' kích hoạt gửi request POST kèm session của Alice
        // nhưng không thể đọc và gửi được CSRF token (do Same-Origin Policy trên cookie/header)
        mockMvc.perform(post("/api/v1/posts")
                        .session(session)
                        .header("Origin", "https://evil-giveaway.com"))
                .andExpect(status().isForbidden());
    }
}
