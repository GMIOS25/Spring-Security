package com.instaclone.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusSemanticsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Bẫy 1 & H9: Request ẩn danh vào endpoint cần xác thực trả về HTTP 401 Unauthorized, không chuyển hướng 302")
    void unauthenticatedReturns401Not302() throws Exception {
        mockMvc.perform(post("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bẫy 1: User thường đã đăng nhập nhưng không đủ quyền truy cập tài nguyên quản trị trả về HTTP 403 Forbidden")
    @WithMockUser(username = "bob", roles = "USER")
    void authenticatedWithoutRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/moderation/reports"))
                .andExpect(status().isForbidden());
    }
}
