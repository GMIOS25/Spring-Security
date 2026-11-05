package com.instaclone.application.service;

import com.instaclone.application.dto.request.CreatePostRequest;
import com.instaclone.application.dto.response.PostResponse;
import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.presentation.advice.ResourceNotFoundException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request, String authorUsername) {
        User author = userRepository.findByUsername(authorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authorUsername));

        Post.PostVisibility visibility = request.visibility() != null
                ? request.visibility()
                : Post.PostVisibility.PUBLIC;

        Post post = new Post(request.caption(), request.imageUrl(), author, visibility);
        Post saved = postRepository.save(post);
        return PostResponse.from(saved);
    }

    /**
     * BOLA/IDOR defense: only the author or users with POST_DELETE_ANY authority (MODERATOR, ADMIN) can delete.
     * If the post does not exist, postSecurity.isOwner returns true (D6) and this method throws
     * ResourceNotFoundException (404) rather than 403.
     */
    @PreAuthorize("@postSecurity.isOwner(#postId, authentication) or hasAuthority('POST_DELETE_ANY')")
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        postRepository.delete(post);
    }

    /**
     * Standard visibility check implementing the hidden resource policy (D6 / Bẫy 2):
     * If the post is not PUBLIC and the caller is not the owner or a moderator/admin,
     * throws ResourceNotFoundException (404) instead of 403 Forbidden to hide the post's existence.
     */
    @Transactional(readOnly = true)
    public PostResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        if (post.getVisibility() != Post.PostVisibility.PUBLIC) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthor = auth != null && auth.isAuthenticated() && post.getAuthor().getUsername().equals(auth.getName());
            boolean isStaff = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream().anyMatch(a ->
                    "POST_DELETE_ANY".equals(a.getAuthority()) ||
                    "ROLE_MODERATOR".equals(a.getAuthority()) ||
                    "ROLE_ADMIN".equals(a.getAuthority()));

            if (!isAuthor && !isStaff) {
                throw new ResourceNotFoundException("Post not found: " + postId);
            }
        }

        return PostResponse.from(post);
    }

    /**
     * Demonstrates Bẫy 2: pure @PostAuthorize returns 403 on private posts of others,
     * leaking the existence of the resource (BOLA/IDOR information leak).
     */
    @PostAuthorize("returnObject.visibility() == T(com.instaclone.domain.entity.Post$PostVisibility).PUBLIC or returnObject.authorUsername() == authentication.name")
    @Transactional(readOnly = true)
    public PostResponse getPostDetailPostAuthorizePure(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        return PostResponse.from(post);
    }
}
