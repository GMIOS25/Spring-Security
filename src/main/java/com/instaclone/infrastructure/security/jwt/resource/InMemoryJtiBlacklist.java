package com.instaclone.infrastructure.security.jwt.resource;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryJtiBlacklist implements JtiBlacklist {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryJtiBlacklist(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        blacklist.put(jti, expiresAt);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        cleanExpired();
        Instant exp = blacklist.get(jti);
        return exp != null && exp.isAfter(clock.instant());
    }

    private void cleanExpired() {
        Instant now = clock.instant();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
