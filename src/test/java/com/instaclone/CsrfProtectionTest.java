package com.instaclone;

import tools.jackson.databind.JsonNode;
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
class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Chương 6 §2.1 & H3: POST cần CSRF token, token lấy từ /csrf/token được XOR mask, raw cookie token bị 403")
    void testCsrfProtectionAndXorMasking() throws Exception {
        // 1. Đăng nhập để có session hợp lệ
        LoginRequest loginRequest = new LoginRequest("alice", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 2. Request POST có session nhưng KHÔNG có CSRF token -> 403 Forbidden
        mockMvc.perform(post("/api/v1/posts")
                        .session(session))
                .andExpect(status().isForbidden());

        // 3. Lấy CSRF token lần 1 từ endpoint /api/v1/csrf/token
        MvcResult csrfResult1 = mockMvc.perform(get("/api/v1/csrf/token")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").isString())
                .andReturn();

        JsonNode jsonNode1 = objectMapper.readTree(csrfResult1.getResponse().getContentAsString());
        String maskedToken1 = jsonNode1.get("token").asText();
        String headerName = jsonNode1.get("headerName").asText();

        // Lấy cookie XSRF-TOKEN (raw token trong cookie)
        Cookie xsrfCookie = csrfResult1.getResponse().getCookie("XSRF-TOKEN");

        // 4. Request POST với session + token đã che (masked token 1) -> 201 Created
        mockMvc.perform(post("/api/v1/posts")
                        .session(session)
                        .header(headerName, maskedToken1))
                .andExpect(status().isCreated());

        // 5. Lấy CSRF token lần 2 từ endpoint /api/v1/csrf/token trong cùng session
        MvcResult csrfResult2 = mockMvc.perform(get("/api/v1/csrf/token")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode2 = objectMapper.readTree(csrfResult2.getResponse().getContentAsString());
        String maskedToken2 = jsonNode2.get("token").asText();

        // XOR Masking: hai lần lấy token trong cùng session cho hai chuỗi token KHÁC NHAU chống BREACH
        assertThat(maskedToken1).isNotEqualTo(maskedToken2);

        // Nhưng masked token 2 vẫn hợp lệ -> 201 Created
        mockMvc.perform(post("/api/v1/posts")
                        .session(session)
                        .header(headerName, maskedToken2))
                .andExpect(status().isCreated());

        // 6. Kiểm chứng H3: Nếu client gửi raw token lấy từ cookie vào header -> XorCsrfTokenRequestAttributeHandler từ chối -> 403 Forbidden
        if (xsrfCookie != null) {
            String rawCookieToken = xsrfCookie.getValue();
            mockMvc.perform(post("/api/v1/posts")
                            .session(session)
                            .cookie(xsrfCookie)
                            .header(headerName, rawCookieToken))
                    .andExpect(status().isForbidden());
        }
    }
}
