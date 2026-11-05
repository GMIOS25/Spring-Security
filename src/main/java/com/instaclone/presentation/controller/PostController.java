package com.instaclone.presentation.controller;

import com.instaclone.application.dto.request.CreatePostRequest;
import com.instaclone.application.dto.response.PostResponse;
import com.instaclone.application.service.PostService;
import com.instaclone.domain.entity.Post;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestBody(required = false) CreatePostRequest request,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "alice";
        CreatePostRequest effective = (request != null && request.caption() != null && !request.caption().isBlank())
                ? request
                : new CreatePostRequest("Default post caption", "https://images.instaclone.local/default.jpg", Post.PostVisibility.PUBLIC);
        PostResponse response = postService.createPost(effective, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        PostResponse response = postService.getPostDetail(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Demo endpoint for Bẫy 2: Uses pure @PostAuthorize on service layer, which returns 403 on private posts of others.
     */
    @GetMapping("/{id}/post-authorize-demo")
    public ResponseEntity<PostResponse> getPostDemo(@PathVariable Long id) {
        PostResponse response = postService.getPostDetailPostAuthorizePure(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "comment created", "postId", id));
    }
}
