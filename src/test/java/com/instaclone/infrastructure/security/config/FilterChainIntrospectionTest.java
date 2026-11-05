package com.instaclone.infrastructure.security.config;

import com.instaclone.infrastructure.security.filter.InstaRateLimitFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FilterChainIntrospectionTest {

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Test
    @DisplayName("Chương 2 §2.1 & L487-493: Bean springSecurityFilterChain là FilterChainProxy quản lý đúng 3 chain và AuthorizationFilter đứng cuối mỗi chain")
    void testFilterChainsStructureAndOrder() {
        assertThat(filterChainProxy).isNotNull();

        List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
        assertThat(chains).hasSize(3);

        SecurityFilterChain chain1 = chains.get(0); // publicFeedFilterChain
        SecurityFilterChain chain2 = chains.get(1); // statelessApiFilterChain
        SecurityFilterChain chain3 = chains.get(2); // webSessionFilterChain

        // AuthorizationFilter đứng cuối mỗi chain (L493)
        for (SecurityFilterChain chain : chains) {
            List<Filter> filters = chain.getFilters();
            assertThat(filters.get(filters.size() - 1)).isInstanceOf(AuthorizationFilter.class);
        }

        // Chain 3 chứa CsrfFilter, BasicAuthenticationFilter và InstaRateLimitFilter
        List<Class<? extends Filter>> chain3Classes = chain3.getFilters().stream().map(Filter::getClass).toList();
        assertThat(chain3Classes).contains(CsrfFilter.class, BasicAuthenticationFilter.class, InstaRateLimitFilter.class);

        // Chain 1 không chứa BasicAuthenticationFilter
        List<Class<? extends Filter>> chain1Classes = chain1.getFilters().stream().map(Filter::getClass).toList();
        assertThat(chain1Classes).doesNotContain(BasicAuthenticationFilter.class);
    }

    @Test
    @DisplayName("Chương 2 §2.1: First-match wins - Mỗi URL mẫu được chọn bởi chain đầu tiên khớp")
    void testFirstMatchWins() {
        List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();

        // 1. GET /api/v1/feed/public/explore -> khớp Chain 1 đầu tiên
        MockHttpServletRequest reqPublic = new MockHttpServletRequest("GET", "/api/v1/feed/public/explore");
        assertThat(chains.get(0).matches(reqPublic)).isTrue();

        // 2. Request mang header Authorization: Bearer ... -> khớp Chain 2
        MockHttpServletRequest reqBearer = new MockHttpServletRequest("GET", "/api/v1/posts");
        reqBearer.addHeader("Authorization", "Bearer valid.jwt.token");
        assertThat(chains.get(0).matches(reqBearer)).isFalse();
        assertThat(chains.get(1).matches(reqBearer)).isTrue();

        // 3. Endpoint phát credential POST /api/v1/auth/token -> khớp Chain 2
        MockHttpServletRequest reqToken = new MockHttpServletRequest("POST", "/api/v1/auth/token");
        assertThat(chains.get(0).matches(reqToken)).isFalse();
        assertThat(chains.get(1).matches(reqToken)).isTrue();

        // 4. Request thông thường không mang Bearer -> rơi vào Chain 3 mặc định
        MockHttpServletRequest reqNormal = new MockHttpServletRequest("GET", "/api/v1/posts/1");
        assertThat(chains.get(0).matches(reqNormal)).isFalse();
        assertThat(chains.get(1).matches(reqNormal)).isFalse();
        assertThat(chains.get(2).matches(reqNormal)).isTrue();
    }
}
