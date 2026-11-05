package com.instaclone;

import com.instaclone.application.dto.request.UpdateProfileRequest;
import com.instaclone.application.dto.response.UserProfileResponse;
import com.instaclone.application.service.UserService;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DoubleEncodingTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Bẫy 2: Mã hóa hai lần encode(encode(p)) làm matches(p) luôn trả về false")
    void doubleEncodingCausesMatchFailure() {
        String rawPassword = "Password123!";

        // Mã hóa lần 1 (hợp lệ ở Service)
        String firstPass = passwordEncoder.encode(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, firstPass)).isTrue();

        // Mã hóa lần 2 (lỗi tai hại: controller/service gọi encode lần nữa)
        String doubleEncoded = passwordEncoder.encode(firstPass);

        // Mật khẩu gốc không bao giờ match với chuỗi băm 2 lần
        assertThat(passwordEncoder.matches(rawPassword, doubleEncoded)).isFalse();
    }

    @Test
    @DisplayName("Câu hỏi Q1 L1065: updateProfile không băm lại mật khẩu, user vẫn xác thực thành công")
    void updateProfileDoesNotTouchPassword() {
        User bob = userRepository.findByUsername("bob").orElseThrow();
        String originalStoredPassword = bob.getPassword();

        // Cập nhật thông tin profile (email)
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("bob_new_email@instaclone.com");
        UserProfileResponse updated = userService.updateProfile(bob.getId(), updateRequest);

        assertThat(updated.email()).isEqualTo("bob_new_email@instaclone.com");

        // Kiểm tra mật khẩu trong DB không bị thay đổi hoặc băm lại
        User reloadedBob = userRepository.findById(bob.getId()).orElseThrow();
        assertThat(reloadedBob.getPassword()).isEqualTo(originalStoredPassword);

        // Mật khẩu gốc Password123! vẫn khớp hoàn toàn
        assertThat(passwordEncoder.matches("Password123!", reloadedBob.getPassword())).isTrue();
    }
}
