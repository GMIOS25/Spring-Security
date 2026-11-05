package com.instaclone;

import com.instaclone.antipattern.PostAuthorizeOnWriteDemo;
import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PostAuthorizeOnWriteTest {

    @Autowired
    private PostAuthorizeOnWriteDemo demo;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser("bob")
    @DisplayName("Bẫy 3: @PostAuthorize trên thao tác ghi làm ném AccessDeniedException NHƯNG dữ liệu đã bị xóa khỏi DB")
    void postAuthorizeOnWriteDeletesBeforeException() {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Post to write-test", "https://img/write.jpg", alice, Post.PostVisibility.PUBLIC));
        Long postId = post.getId();

        assertThat(postRepository.existsById(postId)).isTrue();

        // Phương thức gắn @PostAuthorize("false") sẽ thực thi xóa dữ liệu trong DB trước,
        // sau đó SpEL đánh giá 'false' và ném AccessDeniedException
        assertThatThrownBy(() -> demo.deleteWithPostAuthorize(postId))
                .isInstanceOf(AccessDeniedException.class);

        // HẬU QUẢ: Dữ liệu đã bị xóa mất dù caller nhận lỗi 403 / AccessDeniedException!
        assertThat(postRepository.existsById(postId)).isFalse();
    }
}
