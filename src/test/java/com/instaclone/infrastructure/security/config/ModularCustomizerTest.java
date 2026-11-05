package com.instaclone.infrastructure.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModularCustomizerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chương 2 §3.2 & H1: Bean Customizer<HttpSecurity> được tự động áp dụng, header CSP xuất hiện trên response")
    void testModularCustomizerAppliedAutomatically() throws Exception {
        mockMvc.perform(get("/api/v1/feed/public/explore"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'; script-src 'self'"));
    }
}
