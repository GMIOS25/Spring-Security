package com.instaclone;

import com.instaclone.application.service.PostService;
import com.instaclone.infrastructure.security.config.SecurityChainsConfig;
import com.instaclone.infrastructure.security.filter.InstaRateLimitFilter;
import com.instaclone.infrastructure.security.jwt.resource.JwtResourceConfig;
import com.instaclone.infrastructure.security.ott.MagicLinkMailer;
import com.instaclone.infrastructure.security.service.InstaCloneUserDetailsService;
import com.instaclone.presentation.controller.PostController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WebMvcSliceVsFullStackTest {

    @Nested
    @WebMvcTest(controllers = PostController.class)
    @Import({SecurityChainsConfig.class, JwtResourceConfig.class})
    @ActiveProfiles("test")
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    class WebMvcSliceTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PostService postService;

        @MockitoBean
        private InstaCloneUserDetailsService userDetailsService;

        @MockitoBean
        private MagicLinkMailer magicLinkMailer;

        @MockitoBean
        private JwtDecoder jwtDecoder;

        @MockitoBean
        private JwtAuthenticationConverter jwtAuthenticationConverter;

        @MockitoBean
        private InstaRateLimitFilter rateLimitFilter;

        @MockitoBean
        private Clock clock;

        @Test
        @DisplayName("Q1 & H11: @WebMvcTest với @MockitoBean che giấu lỗi thiếu @PreAuthorize vì mock bypass AOP proxy")
        @WithMockUser(username = "attacker_bob", roles = {"USER"})
        void testMockBypassesMethodSecurityAop() throws Exception {
            // PostService là Mockito mock, doNothing() khiến method trả về mà không đi qua AOP proxy của Spring Security!
            doNothing().when(postService).deletePost(105L);

            // Test này VẪN XANH (200 OK) chứng minh mock làm mất tác dụng bảo vệ của @PreAuthorize!
            mockMvc.perform(delete("/api/v1/posts/105").with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("test")
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    class FullStackTest {

        @LocalServerPort
        private int port;

        @Test
        @DisplayName("Full Stack: HttpClient gọi qua toàn bộ chuỗi Servlet thật, DispatcherServlet và Security")
        void testFullStackViaRealHttpClient() throws Exception {
            HttpClient client = HttpClient.newHttpClient();

            // 1. Feed công khai qua toàn bộ chuỗi Servlet thật -> 200 OK
            HttpRequest publicReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/feed/public/explore"))
                    .GET()
                    .build();
            HttpResponse<String> publicResp = client.send(publicReq, HttpResponse.BodyHandlers.ofString());
            assertThat(publicResp.statusCode()).isEqualTo(200);

            // 2. Thao tác xóa khi chưa đăng nhập qua socket thật -> FilterChain trả 401 JSON
            HttpRequest unauthReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/posts/999"))
                    .DELETE()
                    .build();
            HttpResponse<String> unauthResp = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());
            assertThat(unauthResp.statusCode()).isEqualTo(401);
            assertThat(unauthResp.body()).contains("UNAUTHORIZED");
        }
    }
}
