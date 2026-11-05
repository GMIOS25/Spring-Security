package com.instaclone.antipattern;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class RoleVsAuthorityDemoService {

    @PreAuthorize("hasRole('ADMIN')")
    public String roleAdminOnly() {
        return "ROLE_ADMIN_GRANTED";
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public String authorityAdminOnly() {
        return "AUTHORITY_ADMIN_GRANTED";
    }
}
