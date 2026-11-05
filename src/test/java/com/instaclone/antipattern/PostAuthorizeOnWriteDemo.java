package com.instaclone.antipattern;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.repository.PostRepository;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PostAuthorizeOnWriteDemo {

    private final PostRepository postRepository;

    public PostAuthorizeOnWriteDemo(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Bẫy 3: Using @PostAuthorize on a state-changing (write) method.
     * The method deletes the post BEFORE the @PostAuthorize expression evaluates.
     * Even though AccessDeniedException is thrown to the caller, the data in DB is already deleted!
     */
    @PostAuthorize("false")
    @Transactional
    public void deleteWithPostAuthorize(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        postRepository.delete(post);
    }
}
