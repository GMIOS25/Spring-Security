package com.instaclone;

import com.instaclone.application.dto.request.LoginRequest;
import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.RefreshToken;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.RefreshTokenRepository;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.ott.MagicLinkMailer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MfaDeleteAccountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MagicLinkMailer magicLinkMailer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("H8: MFA bắt buộc cả 2 yếu tố: chỉ password -> 403, chỉ OTT -> 403, cả hai -> 200 và xóa sạch dữ liệu")
    void testMfaAccountDeletionRequiresBothFactors() throws Exception {
        // Tạo một user độc lập để kiểm thử xóa tài khoản: mfa_user
        User mfaUser = userRepository.save(new User(
                "mfa_user",
                passwordEncoder.encode("Password123!"),
                "mfa_user@instaclone.com",
                Set.of("ROLE_USER")
        ));
        Long mfaUserId = mfaUser.getId();

        // Tạo bài post và refresh token liên quan
        postRepository.save(new Post("My sensitive post", "https://img.com/post.jpg", mfaUser, Post.PostVisibility.PUBLIC));
        refreshTokenRepository.save(new RefreshToken(mfaUserId, "dummy_token_hash", Instant.now().plus(7, ChronoUnit.DAYS)));

        // ==========================================
        // Kịch bản 1: Chỉ đăng nhập bằng Mật khẩu -> Bị từ chối 403
        // ==========================================
        MvcResult passwordLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("mfa_user", "Password123!")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession passwordSession = (MockHttpSession) passwordLoginResult.getRequest().getSession(false);
        assertThat(passwordSession).isNotNull();

        // Thực hiện xóa tài khoản chỉ với password factor
        mockMvc.perform(delete("/api/v1/users/me")
                        .session(passwordSession)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Kiểm tra tài khoản vẫn an toàn trong DB
        assertThat(userRepository.findById(mfaUserId)).isPresent();

        // ==========================================
        // Kịch bản 2: Chỉ đăng nhập bằng OTT -> Bị từ chối 403
        // ==========================================
        magicLinkMailer.clear();
        mockMvc.perform(post("/ott/generate")
                        .param("username", "mfa_user")
                        .with(csrf()))
                .andExpect(status().isOk());

        String ottToken1 = magicLinkMailer.getLastToken();
        assertThat(ottToken1).isNotBlank();

        // Đăng nhập một phiên mới chỉ bằng OTT
        MvcResult ottLoginResult = mockMvc.perform(post("/login/ott")
                        .param("token", ottToken1)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession ottOnlySession = (MockHttpSession) ottLoginResult.getRequest().getSession(false);
        assertThat(ottOnlySession).isNotNull();

        // Thực hiện xóa tài khoản chỉ với OTT factor
        mockMvc.perform(delete("/api/v1/users/me")
                        .session(ottOnlySession)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Tài khoản vẫn tồn tại
        assertThat(userRepository.findById(mfaUserId)).isPresent();

        // ==========================================
        // Kịch bản 3: Đăng nhập cả Password và OTT trong CÙNG một phiên -> 200 OK và xóa sạch
        // ==========================================
        // Bước 3.1: Đăng nhập password để có FACTOR_PASSWORD
        MvcResult multiStepLogin1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("mfa_user", "Password123!")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession mfaSession = (MockHttpSession) multiStepLogin1.getRequest().getSession(false);
        assertThat(mfaSession).isNotNull();

        // Bước 3.2: Sinh mã OTT
        magicLinkMailer.clear();
        mockMvc.perform(post("/ott/generate")
                        .session(mfaSession)
                        .param("username", "mfa_user")
                        .with(csrf()))
                .andExpect(status().isOk());

        String ottToken2 = magicLinkMailer.getLastToken();
        assertThat(ottToken2).isNotBlank();

        // Bước 3.3: Xác thực OTT trong CÙNG phiên mfaSession
        mockMvc.perform(post("/login/ott")
                        .session(mfaSession)
                        .param("token", ottToken2)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Kiểm tra session hiện tại tích lũy đủ cả FACTOR_PASSWORD và FACTOR_OTT
        SecurityContext context = (SecurityContext) mfaSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context).isNotNull();
        Authentication finalAuth = context.getAuthentication();
        assertThat(finalAuth).isNotNull();
        assertThat(finalAuth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("FACTOR_PASSWORD"));
        assertThat(finalAuth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("FACTOR_OTT"));

        // Bước 3.4: Xóa tài khoản thành công với đủ cả 2 yếu tố MFA!
        mockMvc.perform(delete("/api/v1/users/me")
                        .session(mfaSession)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));

        // Kiểm tra tài khoản, bài post và refresh token bị dọn sạch
        assertThat(userRepository.findById(mfaUserId)).isEmpty();
        assertThat(postRepository.findByAuthorId(mfaUserId)).isEmpty();
        assertThat(refreshTokenRepository.findAllByUserId(mfaUserId)).isEmpty();
    }
}
