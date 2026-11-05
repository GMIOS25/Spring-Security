package com.instaclone.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/moderation")
public class ModerationController {

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getReports() {
        return ResponseEntity.ok(List.of(
                Map.of("id", 1, "reason", "SPAM", "status", "PENDING")
        ));
    }
}
