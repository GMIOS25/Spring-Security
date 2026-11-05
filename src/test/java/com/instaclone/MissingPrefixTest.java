package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissingPrefixTest {

    @Test
    @DisplayName("Bẫy 1: DelegatingPasswordEncoder ném IllegalArgumentException khi chuỗi hash không có tiền tố {id}")
    void missingPrefixThrowsException() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        encoders.put("bcrypt", bcrypt);

        DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);

        // Giả lập hash cũ trong DB không có tiền tố {bcrypt}
        String rawBcryptHash = bcrypt.encode("Password123!");
        assertThat(rawBcryptHash).startsWith("$2a$");

        // Spring Security 7 cập nhật thông báo lỗi thân thiện hơn so với 5.x/6.x ("There is no PasswordEncoder mapped for the id \"null\""):
        // "Given that there is no default password encoder configured, each password must have a password encoding prefix..."
        assertThatThrownBy(() -> delegatingPasswordEncoder.matches("Password123!", rawBcryptHash))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password encoding prefix");

        // Khắc phục: thiết lập DefaultPasswordEncoderForMatches
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(bcrypt);

        // Sau khi thiết lập fallback, matches thành công và không ném ngoại lệ
        boolean matched = delegatingPasswordEncoder.matches("Password123!", rawBcryptHash);
        assertThat(matched).isTrue();
    }
}
