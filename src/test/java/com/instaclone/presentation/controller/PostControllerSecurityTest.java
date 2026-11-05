package com.instaclone.presentation.controller;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private Long alicePostId;

    @BeforeEach
    void setUp() {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post(
                "Alice test post for security tests",
                "https://images.instaclone.com/alice-sec.jpg",
                alice,
                Post.PostVisibility.PUBLIC
        ));
        alicePostId = post.getId();
    }

    @Test
    @DisplayName("1. Truy cập Feed công khai không cần đăng nhập -> Trả về 200 OK")
    void getPublicFeed_Anonymous_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/feed/public/explore"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. Xóa bài viết khi chưa đăng nhập (Unauthenticated) -> Trả về 401 Unauthorized")
    void deletePost_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/" + alicePostId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Người dùng Bob xóa bài của Alice (BOLA Attack) -> Bị chặn với 403 Forbidden")
    @WithMockUser(username = "bob", roles = {"USER"})
    void deletePost_NotOwner_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/" + alicePostId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(postRepository.existsById(alicePostId)).isTrue();
    }

    @Test
    @DisplayName("4. Kiểm duyệt viên xóa bài vi phạm -> Trả về 200 OK")
    @WithMockUser(username = "mod_alice", authorities = {"ROLE_MODERATOR", "POST_DELETE_ANY"})
    void deletePost_AsModerator_Returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/" + alicePostId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(postRepository.existsById(alicePostId)).isFalse();
    }

    @Test
    @DisplayName("5. Kiểm thử với Stateless JWT PostProcessor: Quyền Admin -> Trả về 200 OK")
    void deletePost_WithStatelessJwt_Returns200() throws Exception {
        // H12 & D5: mockMvc jwt() post-processor giả lập SecurityContext.
        // Cần thêm header Authorization: Bearer với token hợp lệ để FilterChainProxy định tuyến vào chain stateless 2 (không cần CSRF).
        User admin = userRepository.findByUsername("cuong_admin").orElseThrow();
        var auth = new UsernamePasswordAuthenticationToken(
                admin.getUsername(),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("POST_DELETE_ANY"))
        );
        String token = jwtTokenService.generateAccessToken(auth, admin.getId());

        mockMvc.perform(delete("/api/v1/posts/" + alicePostId)
                        .header("Authorization", "Bearer " + token)
                        .with(jwt().jwt(builder -> builder
                                .subject("cuong_admin")
                                .claim("roles", java.util.List.of("ROLE_ADMIN", "POST_DELETE_ANY"))
                        )))
                .andExpect(status().isNoContent());

        assertThat(postRepository.existsById(alicePostId)).isFalse();
    }
}
