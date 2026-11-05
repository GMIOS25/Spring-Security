package com.instaclone;

import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.RegisterRequest;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Đăng ký thành công trả 201, không lộ mật khẩu/hash trong JSON, DB lưu tiền tố {bcrypt}")
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "david_beckham",
                "Password123!",
                "david@instaclone.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("david_beckham"))
                .andExpect(jsonPath("$.email").value("david@instaclone.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                // Tuyệt đối không chứa password hay hash trong response JSON
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.hash").doesNotExist())
                .andExpect(jsonPath("$", not(containsString("Password123!"))))
                .andExpect(jsonPath("$", not(containsString("$2a$"))));

        // Kiểm tra database: hash phải bắt đầu bằng {bcrypt}
        Optional<User> savedUserOpt = userRepository.findByUsername("david_beckham");
        assertThat(savedUserOpt).isPresent();
        User savedUser = savedUserOpt.get();
        assertThat(savedUser.getPassword()).startsWith("{bcrypt}$2a$10$");
    }

    @Test
    @DisplayName("Đăng ký với username đã tồn tại trả về 409 Conflict")
    void registerDuplicateUsernameReturns409() throws Exception {
        // User alice đã được seed sẵn bởi DataSeeder
        RegisterRequest duplicateAlice = new RegisterRequest(
                "alice",
                "Password123!",
                "new_alice@instaclone.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateAlice)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("Username already taken")));
    }

    @Test
    @DisplayName("Đăng ký với định dạng email sai trả về 400 Bad Request")
    void registerInvalidEmailReturns400() throws Exception {
        RegisterRequest invalidEmail = new RegisterRequest(
                "user_invalid_email",
                "Password123!",
                "not-a-valid-email-address"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Đăng ký với mật khẩu ngắn hơn 8 ký tự hoặc dài hơn 72 ký tự trả về 400 Bad Request")
    void registerInvalidPasswordLengthReturns400() throws Exception {
        RegisterRequest shortPw = new RegisterRequest(
                "user_short_pw",
                "short",
                "short@instaclone.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shortPw)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        RegisterRequest longPw = new RegisterRequest(
                "user_long_pw",
                "A".repeat(73),
                "long@instaclone.com"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPw)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
