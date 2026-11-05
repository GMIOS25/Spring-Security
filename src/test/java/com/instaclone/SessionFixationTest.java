package com.instaclone;

import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.LoginRequest;
import jakarta.servlet.http.Cookie;
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
class SessionFixationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Chương 6 §2.2 & H2: Chống Session Fixation - ID phiên đổi sau đăng nhập (S2 != S1), dùng session cũ S1 bị 401")
    void testSessionFixationProtection() throws Exception {
        // 1. Giả lập một session ẩn danh S1 đã tồn tại phía client
        MockHttpSession sessionS1 = new MockHttpSession();
        String s1Id = sessionS1.getId();

        // 2. Đăng nhập mang theo session S1 (kèm requestedSessionId để báo hiệu session đã tồn tại phía client)
        LoginRequest loginRequest = new LoginRequest("alice", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .session(sessionS1)
                        .with(req -> {
                            req.setRequestedSessionId(s1Id);
                            req.setRequestedSessionIdValid(true);
                            return req;
                        })
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sessionS2 = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(sessionS2).isNotNull();
        String s2Id = sessionS2.getId();

        // 3. Chứng minh H2: Session ID đã bị thay đổi để ngăn chặn kẻ tấn công cố định phiên
        assertThat(s2Id).isNotEqualTo(s1Id);

        // 4. Phiên mới S2 truy cập tài nguyên bảo vệ thành công
        mockMvc.perform(get("/api/v1/auth/me")
                        .session(sessionS2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        // 5. Cố tình sử dụng Cookie JSESSIONID cũ (S1) -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new Cookie("JSESSIONID", s1Id)))
                .andExpect(status().isUnauthorized());
    }
}
