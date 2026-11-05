package com.instaclone;

import com.instaclone.application.service.AsyncDemoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AsyncContextPropagationTest {

    @Autowired
    private AsyncDemoService asyncDemoService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bẫy 3: Mất thông tin SecurityContext khi dùng @Async tiêu chuẩn; truyền thành công khi dùng DelegatingSecurityContextAsyncTaskExecutor")
    void asyncContextPropagationDemonstration() throws Exception {
        // Thiết lập SecurityContext trên luồng chính (caller thread)
        SecurityContext mainThreadContext = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("alice", "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mainThreadContext.setAuthentication(auth);
        SecurityContextHolder.setContext(mainThreadContext);

        // 1. Luồng chạy với Standard Executor (Bẫy 3):
        // Do ThreadLocal không tự nhân bản sang Thread Pool khác, luồng async nhìn thấy null
        CompletableFuture<String> standardFuture = asyncDemoService.getPrincipalWithStandardExecutor();
        String standardPrincipal = standardFuture.get(5, TimeUnit.SECONDS);
        assertThat(standardPrincipal).isNull();

        // 2. Luồng chạy với DelegatingSecurityContextAsyncTaskExecutor (Giải pháp):
        // SecurityContext được ủy quyền và lan truyền chính xác sang luồng async
        CompletableFuture<String> securityFuture = asyncDemoService.getPrincipalWithSecurityExecutor();
        String securityPrincipal = securityFuture.get(5, TimeUnit.SECONDS);
        assertThat(securityPrincipal).isEqualTo("alice");
    }
}
