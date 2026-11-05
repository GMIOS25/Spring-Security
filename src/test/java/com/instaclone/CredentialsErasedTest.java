package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CredentialsErasedTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Test
    @DisplayName("Câu hỏi Q2 L1507: Sau khi xác thực thành công, ProviderManager xóa sạch credentials (null) để chống rò rỉ bộ nhớ")
    void credentialsAreErasedAfterAuthentication() {
        UsernamePasswordAuthenticationToken unauthenticatedToken =
                UsernamePasswordAuthenticationToken.unauthenticated("alice", "Password123!");

        // Trước khi xác thực: credentials chứa mật khẩu thô
        assertThat(unauthenticatedToken.getCredentials()).isEqualTo("Password123!");

        // Thực hiện xác thực
        Authentication authenticated = authenticationManager.authenticate(unauthenticatedToken);

        // Sau khi xác thực: credentials bắt buộc phải bị xóa sạch (null)
        assertThat(authenticated.isAuthenticated()).isTrue();
        assertThat(authenticated.getCredentials()).isNull();
    }
}
