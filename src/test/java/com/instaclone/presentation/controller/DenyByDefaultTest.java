package com.instaclone.presentation.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DenyByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chương 1 §2.3: Deny-by-default - Ẩn danh gọi đường dẫn không tồn tại /api/v1/anything bị từ chối 401, không làm lộ 404")
    void unauthenticatedAccessToNonExistentEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/anything"))
                .andExpect(status().isUnauthorized());
    }
}
