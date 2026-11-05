package com.instaclone.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email format")
    String email
) {}
