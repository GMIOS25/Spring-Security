package com.instaclone;

import com.instaclone.infrastructure.security.model.InstaCloneUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TestPrincipalTypesTest {

    @Test
    @DisplayName("Bẫy 2: @WithMockUser tạo principal chuẩn của Spring Security (không phải InstaCloneUserDetails, ép kiểu ném ClassCastException)")
    @WithMockUser(username = "cuong_admin", roles = {"ADMIN"})
    void testWithMockUserPrincipalType() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        Object principal = auth.getPrincipal();

        // Principal là org.springframework.security.core.userdetails.User chuẩn, không có id và không phải InstaCloneUserDetails
        assertThat(principal).isInstanceOf(org.springframework.security.core.userdetails.User.class);
        assertThat(principal).isNotInstanceOf(InstaCloneUserDetails.class);

        assertThatThrownBy(() -> {
            InstaCloneUserDetails userDetails = (InstaCloneUserDetails) principal;
        }).isInstanceOf(ClassCastException.class);
    }

    @Test
    @DisplayName("Bẫy 2: @WithUserDetails nạp đúng InstaCloneUserDetails từ DB có id thật")
    @WithUserDetails(value = "cuong_admin", userDetailsServiceBeanName = "instaCloneUserDetailsService")
    void testWithUserDetailsPrincipalType() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        Object principal = auth.getPrincipal();

        assertThat(principal).isInstanceOf(InstaCloneUserDetails.class);
        InstaCloneUserDetails userDetails = (InstaCloneUserDetails) principal;
        assertThat(userDetails.getUsername()).isEqualTo("cuong_admin");
        assertThat(userDetails.getId()).isNotNull();
        assertThat(userDetails.getEmail()).isEqualTo("cuong_admin@instaclone.com");
    }
}
