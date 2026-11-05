package com.instaclone;

import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatelessVerificationNoDbTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Chương 7 Câu hỏi trọng tâm (L2146): Xác thực Stateless bằng JWT KHÔNG CẦN truy vấn Database (verifyNoInteractions)")
    void statelessVerificationRequiresNoDatabaseHit() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtTokenService.generateAccessToken(auth, 1L);

        // Xóa lịch sử gọi repository từ quá trình bootstrap seeder
        clearInvocations(userRepository);

        // Gọi GET /api/v1/users/me mang JWT
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        // Chứng minh tuyệt đối: UserRepository hoàn toàn KHÔNG bị gọi bất kỳ tương tác nào!
        verifyNoInteractions(userRepository);
    }
}
