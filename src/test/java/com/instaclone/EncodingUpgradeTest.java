package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EncodingUpgradeTest {

    @Test
    @DisplayName("Kiểm chứng H5 (tầng PasswordEncoder): Khi default-id là pbkdf2, hash {bcrypt} cũ vẫn matches và upgradeEncoding trả về true")
    void delegatingPasswordEncoderFlagsOldHashForUpgrade() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder(10));
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        // Cấu hình default-id là pbkdf2
        DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder("pbkdf2", encoders);

        String rawPassword = "Password123!";
        // Giả lập hash cũ trong DB có prefix {bcrypt}
        String oldBcryptHash = "{bcrypt}" + new BCryptPasswordEncoder(10).encode(rawPassword);

        // 1. Vẫn matches thành công
        assertThat(delegatingPasswordEncoder.matches(rawPassword, oldBcryptHash)).isTrue();

        // 2. upgradeEncoding trả về true vì thuật toán hiện tại (bcrypt) khác default-id (pbkdf2)
        assertThat(delegatingPasswordEncoder.upgradeEncoding(oldBcryptHash)).isTrue();

        // 3. Khi băm lại bằng default encoder mới (pbkdf2)
        String newPbkdf2Hash = delegatingPasswordEncoder.encode(rawPassword);
        assertThat(newPbkdf2Hash).startsWith("{pbkdf2}");
        assertThat(delegatingPasswordEncoder.matches(rawPassword, newPbkdf2Hash)).isTrue();

        // Hash mới không còn cần nâng cấp nữa
        assertThat(delegatingPasswordEncoder.upgradeEncoding(newPbkdf2Hash)).isFalse();
    }
}
