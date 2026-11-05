package com.instaclone.application.service;

import com.instaclone.application.dto.request.RegisterRequest;
import com.instaclone.application.dto.request.UpdateProfileRequest;
import com.instaclone.application.dto.response.UserProfileResponse;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.presentation.advice.ResourceNotFoundException;
import com.instaclone.presentation.advice.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfileResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already in use: " + request.email());
        }

        // Băm một lần ở Service (L1054)
        String encodedPassword = passwordEncoder.encode(request.password());
        log.debug("Encoded password generated with prefix for user: {}", request.username());

        User newUser = new User(
            request.username(),
            encodedPassword,
            request.email(),
            Set.of("ROLE_USER")
        );

        User savedUser = userRepository.save(newUser);

        return new UserProfileResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRoles()
        );
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already in use: " + request.email());
        }

        // Q1 L1065: Tuyệt đối không băm lại mật khẩu hiện tại khi chỉ cập nhật thông tin cá nhân
        user.setEmail(request.email());
        User savedUser = userRepository.save(user);

        return new UserProfileResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRoles()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }
}
