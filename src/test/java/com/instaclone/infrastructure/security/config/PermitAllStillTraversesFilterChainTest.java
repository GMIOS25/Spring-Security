package com.instaclone.infrastructure.security.config;

import com.instaclone.presentation.controller.PublicFeedController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PermitAllStillTraversesFilterChainTest.BoomFilterConfig.class)
class PermitAllStillTraversesFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private PublicFeedController publicFeedController;

    @TestConfiguration
    static class BoomFilterConfig {
        @Bean
        public Customizer<HttpSecurity> boomCustomizer() {
            return http -> http.addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                        throws ServletException, IOException {
                    if (request.getHeader("X-Boom") != null) {
                        throw new RuntimeException("Boom in filter chain!");
                    }
                    filterChain.doFilter(request, response);
                }
            }, AuthorizationFilter.class);
        }
    }

    @Test
    @DisplayName("Chương 2 Q1 (L724): permitAll vẫn đi qua FilterChain - Filter ném lỗi thì Controller không bao giờ được gọi")
    void permitAllStillTraversesFilters() throws Exception {
        // Khi không có X-Boom: request đi qua filter bình thường tới Controller -> 200 OK
        mockMvc.perform(get("/api/v1/feed/public/explore"))
                .andExpect(status().isOk());

        verify(publicFeedController).explore();

        // Khi có X-Boom: Custom Filter ném RuntimeException trước khi tới Controller
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/feed/public/explore").header("X-Boom", "true")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Boom in filter chain!");

        // Controller explore() chỉ được gọi 1 lần từ request đầu tiên, không bị gọi ở request thứ 2
        verify(publicFeedController).explore();
    }
}
