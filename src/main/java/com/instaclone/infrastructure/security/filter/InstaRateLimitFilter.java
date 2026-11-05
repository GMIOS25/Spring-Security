package com.instaclone.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter rate limiting:
 * - Kế thừa OncePerRequestFilter (chạy đúng 1 lần/request).
 * - Fixed window 1 phút (inject Clock để test đẩy thời gian).
 * - Dùng getRemoteAddr() chống giả mạo X-Forwarded-For (D10).
 * - Tự dọn dẹp các entry của phút cũ.
 * - Không có @Component để tránh bị Spring Boot tự động đăng ký vào Servlet Container (Bẫy 1).
 */
public class InstaRateLimitFilter extends OncePerRequestFilter {

    private final int maxRequestsPerMinute;
    private final Clock clock;
    private final Map<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    private static class WindowCounter {
        final long minuteEpoch;
        final AtomicInteger count;

        WindowCounter(long minuteEpoch) {
            this.minuteEpoch = minuteEpoch;
            this.count = new AtomicInteger(0);
        }
    }

    public InstaRateLimitFilter(int maxRequestsPerMinute) {
        this(maxRequestsPerMinute, Clock.systemUTC());
    }

    public InstaRateLimitFilter(int maxRequestsPerMinute, Clock clock) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Chỉ áp dụng giới hạn tần suất cho POST /api/v1/posts hoặc /api/v1/posts/...
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/posts")) {
            String clientIp = request.getRemoteAddr();
            long currentMinute = clock.instant().getEpochSecond() / 60;

            WindowCounter counter = requestCounts.compute(clientIp, (ip, existing) -> {
                if (existing == null || existing.minuteEpoch != currentMinute) {
                    return new WindowCounter(currentMinute);
                }
                return existing;
            });

            // Dọn dẹp entry cũ khi dung lượng lớn
            if (requestCounts.size() > 1000) {
                requestCounts.entrySet().removeIf(e -> e.getValue().minuteEpoch < currentMinute);
            }

            if (counter.count.incrementAndGet() > maxRequestsPerMinute) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("""
                    {"error":"TOO_MANY_REQUESTS","message":"InstaClone rate limit exceeded. Please wait a moment before trying again."}
                    """);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
