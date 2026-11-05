package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChainCsrfPolicyTest {

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Test
    @DisplayName("Chương 6 Bẫy 1: Chain 3 (Web session) có CsrfFilter; Chain 2 (Stateless API) không có CsrfFilter")
    void verifyCsrfPolicyAcrossChains() {
        List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
        assertThat(chains).hasSize(3);

        SecurityFilterChain chain2Stateless = chains.get(1);
        SecurityFilterChain chain3Web = chains.get(2);

        // Chain 2 (Stateless API): Không có CsrfFilter vì là stateless (Bẫy 1 L2090)
        boolean chain2HasCsrf = chain2Stateless.getFilters().stream()
                .anyMatch(f -> f instanceof CsrfFilter);
        assertThat(chain2HasCsrf).isFalse();

        // Chain 3 (Web Session): Bắt buộc phải có CsrfFilter để chống CSRF
        boolean chain3HasCsrf = chain3Web.getFilters().stream()
                .anyMatch(f -> f instanceof CsrfFilter);
        assertThat(chain3HasCsrf).isTrue();
    }
}
