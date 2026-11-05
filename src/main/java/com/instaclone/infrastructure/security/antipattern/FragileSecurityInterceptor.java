package com.instaclone.infrastructure.security.antipattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * CẢNH BÁO: Đây là Anti-pattern. Tuyệt đối KHÔNG dùng HandlerInterceptor
 * để thay thế Security Filter Chain trong kiến trúc bảo mật cấp doanh nghiệp.
 */
public class FragileSecurityInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // SAI LẦM 1: So khớp chuỗi thủ công rất dễ sót trường hợp
        // Kẻ tấn công có thể dùng Path Traversal, URL Encoding hoặc Matrix Variables để vượt qua
        if (path.startsWith("/api/v1/posts/") && "DELETE".equalsIgnoreCase(method)) {
            String roleHeader = request.getHeader("X-User-Role"); // Giả lập đọc header

            // SAI LẦM 2: Dựa vào thông tin chưa được kiểm chứng từ Client
            if (roleHeader == null || !roleHeader.contains("ADMIN")) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Insufficient privileges");
                return false;
            }
        }

        // SAI LẦM 3: Nếu request không map trúng bất kỳ @Controller nào, interceptor này bị bỏ qua,
        // nhưng request vẫn có thể rơi vào các ResourceHandler phục vụ file tĩnh chứa dữ liệu nhạy cảm.
        return true;
    }
}
