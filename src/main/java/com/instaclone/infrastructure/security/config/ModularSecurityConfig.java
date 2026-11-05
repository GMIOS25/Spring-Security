package com.instaclone.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class ModularSecurityConfig {

    /**
     * Tự động áp dụng các header bảo mật nâng cao mà không làm phình to cấu hình chính (LT.md L588-608).
     */
    @Bean
    public Customizer<HttpSecurity> securityHeadersCustomizer() {
        return http -> {
            try {
                http.headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; script-src 'self'")
                    )
                );
            } catch (Exception e) {
                throw new IllegalStateException("Failed to configure security headers", e);
            }
        };
    }
}
