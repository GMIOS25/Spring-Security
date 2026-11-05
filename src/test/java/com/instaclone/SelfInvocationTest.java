package com.instaclone;

import com.instaclone.antipattern.SelfInvocationDemo;
import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SelfInvocationTest {

    @Autowired
    private SelfInvocationDemo selfInvocationDemo;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithUserDetails("bob")
    @DisplayName("Bẫy 1: Gọi nội bộ (this.method) vượt mặt Spring AOP proxy, Bob xóa được bài của Alice")
    void selfInvocationBypassesPreAuthorize() {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Post to bypass", "https://img/bypass.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = post.getId();

        // Bob gọi hàm deleteViaSelfInvocation bên trong gọi this.deleteProtectedInternal(postId)
        // Cuộc gọi này không qua Spring AOP proxy -> @PreAuthorize không được kích hoạt -> bài bị xóa
        selfInvocationDemo.deleteViaSelfInvocation(postId);

        assertThat(postRepository.existsById(postId)).isFalse();
    }

    @Test
    @WithUserDetails("bob")
    @DisplayName("Khắc phục Bẫy 1: Gọi qua bean tiêm độc lập (injected bean) kích hoạt AOP proxy và chặn Bob -> AccessDeniedException")
    void callViaInjectedBeanEnforcesPreAuthorize() {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Post protected", "https://img/protected.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = post.getId();

        assertThatThrownBy(() -> selfInvocationDemo.deleteViaInjectedBean(postId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(postRepository.existsById(postId)).isTrue();
    }

    @Test
    @WithUserDetails("bob")
    @DisplayName("Khắc phục Bẫy 1: Gọi phương thức @PreAuthorize trực tiếp từ bean ngoài qua proxy cũng chặn Bob -> AccessDeniedException")
    void directCallOnProxyEnforcesPreAuthorize() {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Post protected direct", "https://img/protected2.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = post.getId();

        assertThatThrownBy(() -> selfInvocationDemo.deleteProtectedInternal(postId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(postRepository.existsById(postId)).isTrue();
    }
}
