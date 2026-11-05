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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BolaDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithUserDetails("bob")
    @DisplayName("BOLA: Bob không thể xóa bài viết của Alice -> 403 và bài viết vẫn tồn tại")
    void bobCannotDeleteAlicePost() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post alicePost = postRepository.save(new Post("Test Alice Post for BOLA", "https://img/alice.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = alicePost.getId();

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(postRepository.existsById(postId)).isTrue();
    }

    @Test
    @WithUserDetails("alice")
    @DisplayName("Chủ sở hữu: Alice xóa bài viết của chính mình -> 204 và bài viết bị xóa khỏi DB")
    void aliceCanDeleteOwnPost() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post alicePost = postRepository.save(new Post("Alice Post to Delete", "https://img/alice.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = alicePost.getId();

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(postRepository.existsById(postId)).isFalse();
    }

    @Test
    @WithUserDetails("mod_alice")
    @DisplayName("Quyền đặc quyền: Moderator (mod_alice) có POST_DELETE_ANY có thể xóa bài của Bob -> 204")
    void moderatorCanDeleteBobPost() throws Exception {
        User bob = userRepository.findByUsername("bob").orElseThrow();
        Post bobPost = postRepository.save(new Post("Bob Post for Mod", "https://img/bob.jpg", bob, Post.PostVisibility.PUBLIC));
        Long postId = bobPost.getId();

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(postRepository.existsById(postId)).isFalse();
    }

    @Test
    @WithUserDetails("cuong_admin")
    @DisplayName("Quyền đặc quyền: Admin (cuong_admin) có POST_DELETE_ANY có thể xóa bài của Bob -> 204")
    void adminCanDeleteBobPost() throws Exception {
        User bob = userRepository.findByUsername("bob").orElseThrow();
        Post bobPost = postRepository.save(new Post("Bob Post for Admin", "https://img/bob.jpg", bob, Post.PostVisibility.PUBLIC));
        Long postId = bobPost.getId();

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(postRepository.existsById(postId)).isFalse();
    }

    @Test
    @DisplayName("Chưa xác thực: Người dùng ẩn danh cố xóa bài viết -> 401 Unauthorized")
    void anonymousCannotDeletePost() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post alicePost = postRepository.save(new Post("Anon delete test", "https://img/alice.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = alicePost.getId();

        mockMvc.perform(delete("/api/v1/posts/" + postId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        assertThat(postRepository.existsById(postId)).isTrue();
    }

    @Test
    @WithUserDetails("bob")
    @DisplayName("D6: Xóa bài viết không tồn tại trả về 404 Not Found thay vì 403")
    void deleteNonExistentPostReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/999999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
