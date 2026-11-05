package com.instaclone.infrastructure.security.model;

import com.instaclone.domain.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InstaCloneUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public InstaCloneUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();

        Set<GrantedAuthority> auths = new HashSet<>();
        for (String role : user.getRoles()) {
            auths.add(new SimpleGrantedAuthority(role));
            // Role -> authority (B4, Ch1 §2.3, Ch5 §2.2): ROLE_MODERATOR và ROLE_ADMIN đều kèm authority POST_DELETE_ANY
            if ("ROLE_MODERATOR".equals(role) || "ROLE_ADMIN".equals(role)) {
                auths.add(new SimpleGrantedAuthority("POST_DELETE_ANY"));
            }
        }
        this.authorities = Collections.unmodifiableSet(auths);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
