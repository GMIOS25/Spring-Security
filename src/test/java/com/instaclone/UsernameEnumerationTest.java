package com.instaclone;

import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsernameEnumerationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Bẫy 2: Chống Username Enumeration - user không tồn tại và user đúng nhưng sai pass trả cùng HTTP 401 và cùng body")
    void preventUsernameEnumeration() throws Exception {
        LoginRequest nonExistentUser = new LoginRequest("user_not_exist_404", "Password123!");
        LoginRequest existingUserWrongPass = new LoginRequest("alice", "WrongPassword999!");

        // Request 1: User không tồn tại
        MvcResult result1 = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUser)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Request 2: User tồn tại nhưng sai mật khẩu
        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingUserWrongPass)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String body1 = result1.getResponse().getContentAsString();
        String body2 = result2.getResponse().getContentAsString();

        // Cả hai phản hồi phải hoàn toàn giống nhau về cấu trúc và thông điệp
        assertThat(body1).isEqualTo(body2);
        assertThat(body1).contains("\"error\":\"UNAUTHORIZED\"");
        assertThat(body1).contains("\"message\":\"Invalid username or password\"");
    }
}
