package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PasswordEncodingTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Hai user cùng mật khẩu nhưng sinh ra chuỗi hash khác biệt do Salt ngẫu nhiên")
    void differentSaltProducesDifferentHash() {
        String rawPassword = "Password123!";
        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
    }

    @Test
    @DisplayName("Chuỗi hash bắt đầu bằng {bcrypt}$2a$10$ và phần sau dài đúng 60 ký tự với 4 thành phần chuẩn")
    void bcryptHashStructureVerification() {
        String rawPassword = "Password123!";
        String encoded = passwordEncoder.encode(rawPassword);

        // Bắt đầu bằng tiền tố định danh thuật toán {bcrypt}$2a$10$
        assertThat(encoded).startsWith("{bcrypt}$2a$10$");

        String bcryptRaw = encoded.substring("{bcrypt}".length());
        assertThat(bcryptRaw).hasSize(60);

        // Cấu trúc 4 phần của BCrypt:
        // Phần 1: phiên bản $2a$ (4 ký tự)
        String version = bcryptRaw.substring(0, 4);
        assertThat(version).isEqualTo("$2a$");

        // Phần 2: cost factor 10$ (3 ký tự)
        String costFactor = bcryptRaw.substring(4, 7);
        assertThat(costFactor).isEqualTo("10$");

        // Phần 3: salt 16 bytes = 22 ký tự Radix-64
        String salt = bcryptRaw.substring(7, 29);
        assertThat(salt).hasSize(22);

        // Phần 4: hash value = 31 ký tự Radix-64
        String hash = bcryptRaw.substring(29);
        assertThat(hash).hasSize(31);
    }

    @Test
    @DisplayName("Hàm matches trả về true khi mật khẩu đúng và false khi mật khẩu sai")
    void matchesVerification() {
        String rawPassword = "Password123!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches("Password123!", encoded)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
        assertThat(passwordEncoder.matches("", encoded)).isFalse();
    }

    @Test
    @DisplayName("Kiểm chứng H4: BCryptPasswordEncoder 7.x ném IllegalArgumentException khi mật khẩu > 72 byte")
    void bcrypt72() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        String pw72 = "A".repeat(72);
        String pw73 = "A".repeat(73);

        String hash72 = bcrypt.encode(pw72);
        assertThat(bcrypt.matches(pw72, hash72)).isTrue();

        // Kiểm chứng H4: ném ngoại lệ tường minh thay vì âm thầm cắt ngắn
        assertThatThrownBy(() -> bcrypt.encode(pw73))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password cannot be more than 72 bytes");
    }
}
