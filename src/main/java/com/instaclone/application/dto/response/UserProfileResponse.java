package com.instaclone.application.dto.response;

import java.util.Set;

public record UserProfileResponse(
    Long id,
    String username,
    String email,
    Set<String> roles
) {}
