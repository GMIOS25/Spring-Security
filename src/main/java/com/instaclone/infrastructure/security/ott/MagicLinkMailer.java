package com.instaclone.infrastructure.security.ott;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MagicLinkMailer {

    private static final Logger log = LoggerFactory.getLogger(MagicLinkMailer.class);

    public record MagicLinkMessage(String username, String tokenValue, Instant sentAt) {}

    private final List<MagicLinkMessage> outbox = new CopyOnWriteArrayList<>();

    public void sendMagicLink(String username, String tokenValue) {
        String masked = maskIdentifier(username);
        log.info("[MagicLinkMailer] Dispatched OTT to user {}: [MASKED_TOKEN]", masked);
        outbox.add(new MagicLinkMessage(username, tokenValue, Instant.now()));
    }

    public String getLastToken() {
        if (outbox.isEmpty()) {
            return null;
        }
        return outbox.get(outbox.size() - 1).tokenValue();
    }

    public List<MagicLinkMessage> getOutbox() {
        return Collections.unmodifiableList(outbox);
    }

    public void clear() {
        outbox.clear();
    }

    private String maskIdentifier(String id) {
        if (id == null || id.isBlank()) {
            return "***";
        }
        if (id.length() <= 3) {
            return id.charAt(0) + "***";
        }
        return id.substring(0, 3) + "***";
    }
}
