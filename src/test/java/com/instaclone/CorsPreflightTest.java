package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsPreflightTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chương 6 §2.3 & Bẫy 2, 3: Preflight OPTIONS từ origin hợp lệ thành công không cần login, allow-credentials=true, origin cụ thể")
    void validOriginPreflightSucceedsWithoutAuth() throws Exception {
        mockMvc.perform(options("/api/v1/posts/105")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "DELETE")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type, X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("Chương 6 Bẫy 2: Preflight OPTIONS từ origin lạ (evil-attacker.com) không nhận được allow-origin")
    void invalidOriginPreflightRejected() throws Exception {
        var result = mockMvc.perform(options("/api/v1/posts/105")
                        .header("Origin", "https://evil-attacker.com")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andReturn();

        String allowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
        assertThat(allowOrigin).isNull();
    }
}
