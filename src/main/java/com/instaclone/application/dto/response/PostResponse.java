package com.instaclone.application.dto.response;

import com.instaclone.domain.entity.Post;

import java.time.Instant;

public record PostResponse(
        Long id,
        String caption,
        String imageUrl,
        String authorUsername,
        Post.PostVisibility visibility,
        Instant createdAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getCaption(),
                post.getImageUrl(),
                post.getAuthor().getUsername(),
                post.getVisibility(),
                post.getCreatedAt()
        );
    }
}
