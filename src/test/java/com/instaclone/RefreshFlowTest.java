package com.instaclone;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.LoginRequest;
import com.instaclone.application.dto.request.RefreshTokenRequest;
import com.instaclone.application.service.RefreshTokenService;
import com.instaclone.domain.entity.RefreshToken;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.RefreshTokenRepository;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Chương 7 §2.3 & D16: Cấp phát, xoay vòng (rotation), chống replay và phát hiện token hết hạn/sai định dạng")
    void testFullRefreshTokenFlow() throws Exception {
        // 1. Cấp phát cặp Access + Refresh token từ /auth/token
        LoginRequest loginRequest = new LoginRequest("alice", "Password123!");
        MvcResult tokenResult = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        JsonNode tokenNode = objectMapper.readTree(tokenResult.getResponse().getContentAsString());
        String access1 = tokenNode.get("accessToken").asText();
        String refresh1 = tokenNode.get("refreshToken").asText();

        // 2. Xoay vòng (Rotation): Đổi refresh1 lấy cặp mới (access2, refresh2)
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refresh1);
        MvcResult rotateResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn();

        JsonNode rotateNode = objectMapper.readTree(rotateResult.getResponse().getContentAsString());
        String access2 = rotateNode.get("accessToken").asText();
        String refresh2 = rotateNode.get("refreshToken").asText();

        assertThat(refresh2).isNotEqualTo(refresh1);
        assertThat(access2).isNotEqualTo(access1);

        // 3. Chống Replay: Dùng lại refresh1 đã bị xoay vòng (revoked) -> Phát hiện replay, thu hồi toàn bộ token của user
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());

        // Sau khi bị phát hiện replay, ngay cả refresh2 cũng không còn dùng được
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refresh2))))
                .andExpect(status().isUnauthorized());

        // 4. Refresh token hết hạn -> 401
        User bob = userRepository.findByUsername("bob").orElseThrow();
        String expiredRaw = "expired_raw_token_xyz_123456789";
        String expiredHash = RefreshTokenService.hashToken(expiredRaw);
        refreshTokenRepository.save(new RefreshToken(bob.getId(), expiredHash, Instant.now().minus(1, ChronoUnit.HOURS)));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(expiredRaw))))
                .andExpect(status().isUnauthorized());

        // 5. Dùng Refresh Token làm Bearer Token -> 401 (không phải JWT hợp lệ)
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + refresh1))
                .andExpect(status().isUnauthorized());
    }
}
