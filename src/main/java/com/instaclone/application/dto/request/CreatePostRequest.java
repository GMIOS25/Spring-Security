package com.instaclone.application.dto.request;

import com.instaclone.domain.entity.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "Caption must not be blank")
        @Size(max = 1000, message = "Caption must not exceed 1000 characters")
        String caption,

        @NotBlank(message = "Image URL must not be blank")
        String imageUrl,

        Post.PostVisibility visibility
) {}
