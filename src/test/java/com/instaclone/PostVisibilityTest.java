package com.instaclone;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostVisibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithUserDetails("bob")
    @DisplayName("Bob xem bài viết PUBLIC của Alice -> 200 OK")
    void bobCanViewAlicePublicPost() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Public Post", "https://img/public.jpg", alice, Post.PostVisibility.PUBLIC));

        mockMvc.perform(get("/api/v1/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("Public Post"))
                .andExpect(jsonPath("$.authorUsername").value("alice"));
    }

    @Test
    @WithUserDetails("bob")
    @DisplayName("Chính sách ẩn tài nguyên (D6): Bob xem bài PRIVATE của Alice -> 404 Not Found thay vì 403")
    void bobCannotViewAlicePrivatePostHidingPolicy() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Private Post", "https://img/private.jpg", alice, Post.PostVisibility.PRIVATE));

        mockMvc.perform(get("/api/v1/posts/" + post.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @WithUserDetails("alice")
    @DisplayName("Alice xem bài PRIVATE của chính mình -> 200 OK")
    void aliceCanViewOwnPrivatePost() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Alice Own Private Post", "https://img/private.jpg", alice, Post.PostVisibility.PRIVATE));

        mockMvc.perform(get("/api/v1/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("Alice Own Private Post"))
                .andExpect(jsonPath("$.authorUsername").value("alice"));
    }

    @Test
    @WithUserDetails("bob")
    @DisplayName("Bẫy 2: Biến thể @PostAuthorize thuần ném 403 Forbidden, làm rò rỉ sự tồn tại của bài đăng")
    void postAuthorizePureLeaksExistenceWith403() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Private Leaked Post", "https://img/private.jpg", alice, Post.PostVisibility.PRIVATE));

        mockMvc.perform(get("/api/v1/posts/" + post.getId() + "/post-authorize-demo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
