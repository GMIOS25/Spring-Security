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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrentSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Chương 6 §2.2: Giới hạn tối đa 2 phiên đồng thời - Đăng nhập lần 3 làm phiên 1 hết hạn (401), phiên 2 & 3 hoạt động bình thường")
    void testConcurrentSessionLimit() throws Exception {
        LoginRequest loginRequest = new LoginRequest("alice", "Password123!");
        String json = objectMapper.writeValueAsString(loginRequest);

        // Đăng nhập phiên 1
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session1 = (MockHttpSession) res1.getRequest().getSession(false);
        assertThat(session1).isNotNull();

        // Kiểm tra phiên 1 dùng được
        mockMvc.perform(get("/api/v1/auth/me").session(session1))
                .andExpect(status().isOk());

        // Đăng nhập phiên 2
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session2 = (MockHttpSession) res2.getRequest().getSession(false);
        assertThat(session2).isNotNull();

        // Kiểm tra phiên 2 dùng được
        mockMvc.perform(get("/api/v1/auth/me").session(session2))
                .andExpect(status().isOk());

        // Đăng nhập phiên 3 (vượt giới hạn maximumSessions=2)
        MvcResult res3 = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session3 = (MockHttpSession) res3.getRequest().getSession(false);
        assertThat(session3).isNotNull();

        // KẾT QUẢ: Phiên 1 (cũ nhất) bị ConcurrentSessionFilter đá ra -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me").session(session1))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        // Phiên 2 và 3 vẫn hoạt động bình thường -> 200 OK
        mockMvc.perform(get("/api/v1/auth/me").session(session2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").session(session3))
                .andExpect(status().isOk());
    }
}
