package com.instaclone.infrastructure.security.evaluator;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("postSecurity")
public class PostSecurityEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PostSecurityEvaluator.class);
    private final PostRepository postRepository;

    public PostSecurityEvaluator(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Check if the authenticated user is the owner of the post.
     * Evaluated via SpEL in @PreAuthorize: @postSecurity.isOwner(#postId, authentication)
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long postId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String currentUsername = authentication.getName();
        Post post = postRepository.findById(postId).orElse(null);

        if (post == null) {
            // D6: Return true if post not found so Service can throw ResourceNotFoundException (404) instead of 403
            return true;
        }

        boolean isAuthor = post.getAuthor().getUsername().equals(currentUsername);
        log.debug("Checking ownership for post {}: user={}, isAuthor={}", postId, currentUsername, isAuthor);
        return isAuthor;
    }
}
