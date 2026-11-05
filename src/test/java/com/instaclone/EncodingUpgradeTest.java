package com.instaclone;

import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "instaclone.security.password.default-id=pbkdf2")
    class AutoUpgradeIntegrationTest {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        @DisplayName("Kiểm chứng H5 (tích hợp toàn diện): DaoAuthenticationProvider tự băm lại khi login thành công qua UserDetailsPasswordService")
        void daoAuthenticationProviderUpgradesEncodingOnSuccessfulLogin() {
            String rawPassword = "Password123!";
            // Tạo một user mang hash {bcrypt} cũ
            String oldBcryptHash = "{bcrypt}" + new BCryptPasswordEncoder(10).encode(rawPassword);
            User upgradeUser = userRepository.save(new User(
                    "upgrade_user",
                    oldBcryptHash,
                    "upgrade@instaclone.com",
                    Set.of("ROLE_USER")
            ));

            assertThat(upgradeUser.getPassword()).startsWith("{bcrypt}");

            // 1. Thực hiện đăng nhập lần đầu
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated("upgrade_user", rawPassword)
            );
            assertThat(auth.isAuthenticated()).isTrue();

            // 2. Kiểm tra database: DaoAuthenticationProvider đã tự động kích hoạt
            // UserDetailsPasswordService.updatePassword(...) và nâng cấp hash sang {pbkdf2}
            User reloadedUser = userRepository.findByUsername("upgrade_user").orElseThrow();
            assertThat(reloadedUser.getPassword()).startsWith("{pbkdf2}");
            assertThat(reloadedUser.getPassword()).isNotEqualTo(oldBcryptHash);

            // 3. Đăng nhập lại lần hai với hash {pbkdf2} mới -> Vẫn thành công
            Authentication auth2 = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated("upgrade_user", rawPassword)
            );
            assertThat(auth2.isAuthenticated()).isTrue();
        }
    }
}
