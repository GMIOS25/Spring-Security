package com.instaclone.infrastructure.security.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilterRunsOnceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private InstaRateLimitFilter rateLimitFilter;

    @Test
    @DisplayName("Chương 2 Bẫy 1 (L694): Filter chạy đúng 1 lần mỗi request nhờ tắt tự động đăng ký vào Servlet Container")
    @WithMockUser(username = "alice", roles = "USER")
    void filterRunsExactlyOncePerRequest() throws Exception {
        mockMvc.perform(post("/api/v1/posts").with(csrf()))
                .andExpect(status().isCreated());

        verify(rateLimitFilter, times(1)).doFilter(any(), any(), any());
    }
}
