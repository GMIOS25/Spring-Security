package com.instaclone;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.LoginRequest;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.RefreshTokenRepository;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.jwt.resource.InMemoryJtiBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutAllTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Chương 7 §2.3: Đăng xuất mọi thiết bị - Refresh token bị xóa, access token hiện tại bị blacklist ngay (401), user khác không ảnh hưởng")
    void testLogoutAllAndAccessRevocation() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        User bob = userRepository.findByUsername("bob").orElseThrow();

        // 1. Cấp token cho Alice
        MvcResult aliceTokenRes = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode aliceNode = objectMapper.readTree(aliceTokenRes.getResponse().getContentAsString());
        String aliceAccess = aliceNode.get("accessToken").asText();

        // 2. Cấp token cho Bob
        MvcResult bobTokenRes = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("bob", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bobNode = objectMapper.readTree(bobTokenRes.getResponse().getContentAsString());
        String bobAccess = bobNode.get("accessToken").asText();

        // Trước khi logout-all: cả Alice và Bob đều truy cập được
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + aliceAccess))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + bobAccess))
                .andExpect(status().isOk());

        // 3. Alice thực hiện đăng xuất tất cả thiết bị (POST /api/v1/auth/logout-all)
        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + aliceAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all devices"));

        // 4. Kiểm tra Refresh token của Alice đã bị dọn sạch
        assertThat(refreshTokenRepository.findAllByUserId(alice.getId())).isEmpty();

        // 5. Access token hiện tại của Alice bị từ chối NGAY LẬP TỨC (do jti đã vào blacklist) -> 401
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + aliceAccess))
                .andExpect(status().isUnauthorized());

        // 6. Token của Bob hoàn toàn không bị ảnh hưởng -> 200 OK
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + bobAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    @DisplayName("Chương 7 §2.3: JtiBlacklist TTL - Mục blacklist tự động hết hạn và dọn dẹp sau khi token hết hạn")
    void testJtiBlacklistTtlCleanup() {
        Instant t0 = Instant.parse("2026-06-01T12:00:00Z");
        MutableTestClock testClock = new MutableTestClock(t0);
        InMemoryJtiBlacklist blacklist = new InMemoryJtiBlacklist(testClock);

        String jti = "test-jti-123";
        Instant exp = t0.plusSeconds(300); // Hết hạn sau 5 phút

        blacklist.blacklist(jti, exp);
        assertThat(blacklist.isBlacklisted(jti)).isTrue();

        // Đẩy đồng hồ thêm 301 giây (quá thời hạn exp)
        testClock.setInstant(t0.plusSeconds(301));
        assertThat(blacklist.isBlacklisted(jti)).isFalse();
    }

    private static class MutableTestClock extends Clock {
        private Instant instant;
        private final ZoneId zone = ZoneId.of("UTC");

        public MutableTestClock(Instant instant) {
            this.instant = instant;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
