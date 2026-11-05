package com.instaclone.infrastructure.security.jwt.resource;

import java.time.Instant;

public interface JtiBlacklist {
    void blacklist(String jti, Instant expiresAt);
    boolean isBlacklisted(String jti);
}
