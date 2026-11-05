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
class StaticResourceProtectedTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chương 1 §2.2: Filter bảo vệ tài nguyên tĩnh /uploads/private.txt trước khi tới ResourceHandler, trả 401 nếu chưa xác thực")
    void unauthenticatedStaticResourceReturns401() throws Exception {
        mockMvc.perform(get("/uploads/private.txt"))
                .andExpect(status().isUnauthorized());
    }
}
