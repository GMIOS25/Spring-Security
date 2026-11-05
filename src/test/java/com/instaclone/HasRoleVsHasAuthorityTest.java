package com.instaclone;

import com.instaclone.antipattern.RoleVsAuthorityDemoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class HasRoleVsHasAuthorityTest {

    @Autowired
    private RoleVsAuthorityDemoService demoService;

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("User có authority ADMIN (không có ROLE_): qua hasAuthority, bị chặn bởi hasRole")
    void authorityOnlyUser() {
        assertThat(demoService.authorityAdminOnly()).isEqualTo("AUTHORITY_ADMIN_GRANTED");

        assertThatThrownBy(() -> demoService.roleAdminOnly())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("User có role ADMIN (tự thêm prefix ROLE_ADMIN): qua hasRole, bị chặn bởi hasAuthority('ADMIN')")
    void roleOnlyUser() {
        assertThat(demoService.roleAdminOnly()).isEqualTo("ROLE_ADMIN_GRANTED");

        assertThatThrownBy(() -> demoService.authorityAdminOnly())
                .isInstanceOf(AccessDeniedException.class);
    }
}
