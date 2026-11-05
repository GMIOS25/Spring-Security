package com.instaclone;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfInTestsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private Long postId;

    @BeforeEach
    void setUp() {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post(
                "Csrf test post",
                "https://images.instaclone.com/csrf-test.jpg",
                alice,
                Post.PostVisibility.PUBLIC
        ));
        postId = post.getId();
    }

    @Test
    @DisplayName("Bẫy 1: Thao tác DELETE có quyền ADMIN nhưng thiếu .with(csrf()) -> Bị từ chối 403 do CsrfFilter")
    @WithMockUser(username = "cuong_admin", authorities = {"ROLE_ADMIN", "POST_DELETE_ANY"})
    void deleteWithoutCsrf_FailsWith403() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/" + postId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bẫy 1: Thao tác DELETE có quyền ADMIN và bổ sung .with(csrf()) -> Thành công 200 OK")
    @WithMockUser(username = "cuong_admin", authorities = {"ROLE_ADMIN", "POST_DELETE_ANY"})
    void deleteWithCsrf_SucceedsWith200() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
