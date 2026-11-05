package com.instaclone.infrastructure.security.antipattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AntiPatternInterceptorTest {

    private final FragileSecurityInterceptor interceptor = new FragileSecurityInterceptor();

    @Test
    @DisplayName("Sai lầm 1: DELETE /api/v1/posts thiếu dấu gạch chéo cuối bị lọt qua interceptor")
    void testMissingTrailingSlashBypassesInterceptor() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        // Do path.startsWith("/api/v1/posts/") là false nên request lọt qua (trả về true)
        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Sai lầm 2: Header X-User-Role: ADMIN giả mạo từ client được tin cậy và lọt qua")
    void testForgedRoleHeaderBypassesInterceptor() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/posts/102");
        request.addHeader("X-User-Role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
