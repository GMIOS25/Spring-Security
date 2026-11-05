package com.instaclone.presentation.controller;

import com.instaclone.infrastructure.security.antipattern.FragileSecurityInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FirewallVsInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    private final FragileSecurityInterceptor interceptor = new FragileSecurityInterceptor();

    @Test
    @DisplayName("Chương 1 §2.2 & L347–348: StrictHttpFirewall từ chối URL có matrix parameter hoặc double slash trong khi Interceptor bỏ lọt")
    void testFirewallRejectsWhileInterceptorMisses() throws Exception {
        // 1. StrictHttpFirewall từ chối request có matrix variable "..;/x" trả về HTTP 400 Bad Request
        mockMvc.perform(delete("/api/v1/posts/..;/x"))
                .andExpect(status().isBadRequest());

        // 2. MockMvc khi gửi "//api/v1/posts/1" được Filter Chain bảo vệ, từ chối với 401 Unauthorized
        mockMvc.perform(delete("//api/v1/posts/1"))
                .andExpect(status().isUnauthorized());

        // 3. Trong khi đó, FragileSecurityInterceptor so khớp chuỗi thủ công:
        // Với double slash "//api/v1/posts/1", path.startsWith("/api/v1/posts/") là false
        // -> interceptor bỏ qua, trả về true (cho phép lọt qua!)
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "//api/v1/posts/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
    }
}
