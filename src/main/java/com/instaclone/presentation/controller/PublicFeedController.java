package com.instaclone.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feed/public")
public class PublicFeedController {

    @GetMapping("/explore")
    public ResponseEntity<List<Map<String, Object>>> explore() {
        return ResponseEntity.ok(List.of(
            Map.of("id", 1, "caption", "Welcome to InstaClone!", "imageUrl", "https://img.instaclone.com/1.jpg")
        ));
    }
}
