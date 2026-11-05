package com.instaclone;

import com.instaclone.infrastructure.security.ott.MagicLinkMailer;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OttLoginFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MagicLinkMailer magicLinkMailer;

    @MockitoSpyBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        magicLinkMailer.clear();
        given(clock.instant()).willReturn(Instant.now());
    }

    @Test
    @DisplayName("Luồng One-Time Token (OTT) hoàn chỉnh: generate, submit-page GET không tiêu thụ, POST login cấp FACTOR_OTT, chống replay và hết hạn")
    void testFullOttFlow() throws Exception {
        // 1. POST /ott/generate -> 200 OK và token gửi vào MagicLinkMailer
        MvcResult genResult = mockMvc.perform(post("/ott/generate")
                        .param("username", "alice")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("One-time login token has been dispatched to your email."))
                .andReturn();

        String token = magicLinkMailer.getLastToken();
        assertThat(token).isNotBlank();

        // 2. GET /login/ott?token=... chỉ hiển thị form submit và CHƯA tiêu thụ token
        mockMvc.perform(get("/login/ott").param("token", token))
                .andExpect(status().isOk());

        // 3. POST /login/ott -> Tiêu thụ token, xác thực thành công và sở hữu FACTOR_OTT
        MvcResult loginResult = mockMvc.perform(post("/login/ott")
                        .param("token", token)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext).isNotNull();

        Authentication auth = securityContext.getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("FACTOR_OTT"));

        // 4. Chống Replay: Dùng lại token đã tiêu thụ -> Thất bại
        mockMvc.perform(post("/login/ott")
                        .param("token", token)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // 5. Token hết hạn (quá 5 phút) -> Thất bại
        Instant now = Instant.now();
        given(clock.instant()).willReturn(now);

        mockMvc.perform(post("/ott/generate")
                        .param("username", "bob")
                        .with(csrf()))
                .andExpect(status().isOk());

        String bobToken = magicLinkMailer.getLastToken();
        assertThat(bobToken).isNotBlank();

        // Đẩy clock thêm 6 phút
        given(clock.instant()).willReturn(now.plus(Duration.ofMinutes(6)));

        mockMvc.perform(post("/login/ott")
                        .param("token", bobToken)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // 6. Chống Username Enumeration: Username không tồn tại trả về phản hồi giống username thật
        mockMvc.perform(post("/ott/generate")
                        .param("username", "non_existent_ghost_user")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("One-time login token has been dispatched to your email."));
    }
}
