package com.instaclone.antipattern;

import com.instaclone.application.service.PostService;
import com.instaclone.domain.entity.Post;
import com.instaclone.domain.repository.PostRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SelfInvocationDemo {

    private final PostRepository postRepository;
    private final PostService postService;

    public SelfInvocationDemo(PostRepository postRepository, @Lazy PostService postService) {
        this.postRepository = postRepository;
        this.postService = postService;
    }

    /**
     * Bẫy 1: Calling a @PreAuthorize method internally via this.method() bypasses the Spring AOP proxy.
     */
    public void deleteViaSelfInvocation(Long postId) {
        this.deleteProtectedInternal(postId);
    }

    /**
     * Calling via an external injected bean goes through the Spring AOP proxy and enforces security.
     */
    public void deleteViaInjectedBean(Long postId) {
        postService.deletePost(postId);
    }

    @PreAuthorize("@postSecurity.isOwner(#postId, authentication) or hasAuthority('POST_DELETE_ANY')")
    @Transactional
    public void deleteProtectedInternal(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        postRepository.delete(post);
    }
}
