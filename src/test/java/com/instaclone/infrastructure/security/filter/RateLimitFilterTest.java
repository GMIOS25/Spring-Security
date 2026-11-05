package com.instaclone.infrastructure.security.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Clock clock;

    private final Instant baseTime = Instant.parse("2026-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(baseTime);
    }

    @Test
    @DisplayName("Chương 2 §3.3 & D10: Giới hạn tần suất 3 req/phút - lần 4 trả về 429, sau 61s mở lại, X-Forwarded-For giả không bypass, GET không bị tính")
    @WithMockUser(username = "alice", roles = "USER")
    void testRateLimitEnforcement() throws Exception {
        // Lần 1, 2, 3 cùng IP (127.0.0.1) -> 201 Created
        mockMvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isCreated());

        // Lần 4 cùng IP trong cùng 1 phút -> 429 Too Many Requests
        mockMvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));

        // Header X-Forwarded-For giả mạo KHÔNG né được giới hạn (D10)
        mockMvc.perform(post("/api/v1/posts").with(csrf()).header("X-Forwarded-For", "203.0.113.195"))
                .andExpect(status().isTooManyRequests());

        // GET không bị đếm trong rate limiter của POST
        mockMvc.perform(get("/api/v1/feed/public/explore"))
                .andExpect(status().isOk());

        // Đẩy đồng hồ thêm 61 giây (sang phút tiếp theo)
        when(clock.instant()).thenReturn(baseTime.plusSeconds(61));

        // Request tiếp theo thành công 201 Created
        mockMvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isCreated());
    }
}
