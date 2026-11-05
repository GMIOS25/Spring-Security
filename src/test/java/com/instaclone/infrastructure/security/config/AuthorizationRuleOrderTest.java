package com.instaclone.infrastructure.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({AuthorizationRuleOrderTest.OrderTestController.class, AuthorizationRuleOrderTest.OrderChainConfig.class})
class AuthorizationRuleOrderTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class OrderTestController {
        @GetMapping("/test/flawed/admin")
        public ResponseEntity<String> flawedAdmin() {
            return ResponseEntity.ok("flawed admin reached");
        }

        @GetMapping("/test/correct/admin")
        public ResponseEntity<String> correctAdmin() {
            return ResponseEntity.ok("correct admin reached");
        }
    }

    @TestConfiguration
    static class OrderChainConfig {

        @Bean
        @Order(0)
        public SecurityFilterChain flawedChain(HttpSecurity http) throws Exception {
            // SAI LẦM: permitAll tổng quát đặt TRƯỚC rule cụ thể
            http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/test/flawed/**"))
                .authorizeHttpRequests(a -> a
                    .requestMatchers("/test/flawed/**").permitAll()
                    .requestMatchers("/test/flawed/admin").hasRole("ADMIN")
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }

        @Bean
        @Order(-1)
        public SecurityFilterChain correctChain(HttpSecurity http) throws Exception {
            // CHUẨN MỰC: rule cụ thể hasRole đặt TRƯỚC rule tổng quát
            http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/test/correct/**"))
                .authorizeHttpRequests(a -> a
                    .requestMatchers("/test/correct/admin").hasRole("ADMIN")
                    .requestMatchers("/test/correct/**").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Test
    @DisplayName("Chương 2 Bẫy 2 (L714): Thứ tự rule sai (tổng quát trước cụ thể) làm lọt ẩn danh; đúng thứ tự thì 401")
    void testRuleOrderAffectsAuthorization() throws Exception {
        // Chain sai thứ tự: request /test/flawed/admin thỏa mãn permitAll("/test/flawed/**") trước -> 200 OK (lọt lỗi!)
        mockMvc.perform(get("/test/flawed/admin"))
                .andExpect(status().isOk());

        // Chain đúng thứ tự: rule hasRole("ADMIN") khớp trước -> chặn ẩn danh với 401
        mockMvc.perform(get("/test/correct/admin"))
                .andExpect(status().isUnauthorized());
    }
}
