package com.instaclone.presentation.controller;

import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.RefreshTokenRepository;
import com.instaclone.domain.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserManagementController(
            UserRepository userRepository,
            PostRepository postRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * API xóa tài khoản cá nhân vĩnh viễn.
     * Bảo vệ tầng sâu: Bắt buộc đã xác thực và sở hữu nhân tố FACTOR_OTT từ Spring Security 7.x
     */
    @DeleteMapping("/me")
    @PreAuthorize("hasAuthority('FACTOR_OTT')")
    @Transactional
    public ResponseEntity<?> deleteMyAccount(Authentication authentication) {
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            refreshTokenRepository.deleteAllByUserId(user.getId());
            postRepository.deleteAllByAuthorId(user.getId());
            userRepository.delete(user);
        }

        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "message", "User account " + username + " and all associated data have been permanently purged."
        ));
    }
}
