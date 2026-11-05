# DEVIATIONS — Ghi nhận sai lệch và kết quả kiểm chứng

Tài liệu này ghi lại các sai lệch có chủ đích so với `docs/LT.md` hoặc `docs/plan.md`, và kết quả thực nghiệm kiểm chứng các giả thuyết ở Phần D.

---

## 1. Sai lệch so với LT.md / PLAN (D#)

| Mã | Tham chiếu LT.md | Mô tả sai lệch và lý do | Giải pháp áp dụng |
|---|---|---|---|
| D1 | 519–572, 1683–1716, 1961–2026, 2336–2397, 2595–2666 | Trùng tên bean và không có securityMatcher gây lỗi khởi động app | Gộp thành 3 chain có thứ tự `@Order` rõ ràng theo B2/B6 |
| D2 | 2618 | `csrf.disable()` trong chain có cookie/session vi phạm nguyên tắc an toàn | Bật CSRF trên chain 3; test dùng `.with(csrf())` |
| D3 | 2718–2738 | `@MockBean` là kiểu Boot 3; Boot 4 chuyển sang `@MockitoBean` | Dùng `@MockitoBean` và `@MockitoSpyBean` |
| D4 | 2755–2765 | Mockito `doThrow` không đi qua AOP proxy của Spring Security | Dùng test tích hợp với bean thật và DB H2 |
| D5 | 2783–2787 | Test dùng `jwt()` mà không chỉ rõ chain | Xác định request JWT đi vào statelessApiFilterChain (chain 2) |
| D6 | 1756–1759, 1831–1834 | `isOwner` trả true khi bài không tồn tại; `@PostAuthorize` trả 403 làm lộ tài nguyên | Ném `ResourceNotFoundException` (404) cho bài không tồn tại hoặc không đủ quyền xem |
| D7 | 321, 1809–1814 | Service trả entity Post dẫn đến `LazyInitializationException` và lộ mật khẩu | Trả record `PostResponse` |
| D8 | 1990, 2036 | `redirectToHttps` bật cứng gây lỗi MockMvc và localhost | Điều kiện hóa bằng property `instaclone.security.require-https` |
| D9 | 1412–1449 vs 2005–2011 | Controller login tự viết không kích hoạt `SessionAuthenticationStrategy` | Gọi strategy tường minh trước khi `saveContext` |
| D10 | 639–686, 694–712 | Filter rate limit cũ không reset bộ đếm, rò rỉ bộ nhớ, tin `X-Forwarded-For` giả | Cửa sổ trượt 1 phút, inject `Clock`, dùng `getRemoteAddr()`, không khai báo `@Component` |
| D11 | 254–268, 972–999, 1031 | `User` thiếu getter/setter, `Set.of` bất biến trên ElementCollection, thiếu repo | Bổ sung đầy đủ entity, mutable collection, JpaRepository, RestControllerAdvice |
| D12 | 915 | Mật khẩu max=100 vượt ngưỡng 72 byte của BCrypt | Đặt validation `@Size(min = 8, max = 72)` |
| D13 | 2655–2656, 2572 | In token ra `System.out` và không thống nhất tham số OTT | Mock mailer in-memory, API nhận username và phản hồi trung tính |
| D14 | 558, 2369, 1794 vs 214 | Bất nhất `hasRole` vs `hasAuthority('POST_DELETE_ANY')` | Moderator và Admin đều có quyền `POST_DELETE_ANY` |
| D15 | 2286–2330 | Khóa RSA sinh mới ngẫu nhiên trong constructor | Tách cấu hình RSA, thêm issuer và jti blacklist validator |
| D16 | 2242–2252 vs 2428–2447 | Thiếu luồng refresh token và thu hồi token trong code mẫu | Triển khai `RefreshTokenService` lưu hash SHA-256 và `JtiBlacklist` |
| D17 | 414, 1454, 2630, 2686 | Lẫn lộn `/users/me` và `/auth/me` | Cung cấp cả hai endpoint |
| D18 | 2629–2638 | `oauth2Login` thiếu cấu hình client khiến app không khởi động | Khai báo dummy registration cho github; entry point 401 cho `/api/**` |
| D19 | 2560–2566 vs 2624, 2693 | Chưa kích hoạt cơ chế gộp yếu tố MFA | Dùng `@EnableMultiFactorAuthentication` và yêu cầu cả `FACTOR_PASSWORD` + `FACTOR_OTT` cho endpoint nhạy cảm |
| D20 | 1192 | Sơ đồ ghi sai `ProviderManager` gọi `SecurityContextHolder.setAuthentication` | Ghi nhận trong QA.md: Controller/Filter là nơi lưu SecurityContext |

---

## 2. Kết quả kiểm chứng giả thuyết (Phần D)

| Giả thuyết | Mô tả | Trạng thái | Bằng chứng thực nghiệm |
|---|---|---|---|
| H1 | Bean `Customizer<HttpSecurity>` tự áp dụng cho mọi chain | Đang chờ | |
| H2 | Login tự viết không chống session fixation nếu không gọi strategy | Đang chờ | |
| H3 | `XorCsrfTokenRequestAttributeHandler`: header mang token thô bị 403, token che thì được | Đang chờ | |
| H4 | `BCryptPasswordEncoder` 7.x ném lỗi khi mật khẩu > 72 byte | Đang chờ | |
| H5 | `DaoAuthenticationProvider` tự re-encode khi có `UserDetailsPasswordService` | Đang chờ | |
| H6 | `JwtDecoder` mặc định từ chối `alg=none`, sai chữ ký, hết hạn | Đang chờ | |
| H7 | `/oauth2/authorization/github` sinh `code_challenge` + `S256` PKCE | Đang chờ | |
| H8 | Đăng nhập mật khẩu + OTT cùng phiên tích lũy đủ 2 yếu tố MFA | Đang chờ | |
| H9 | Endpoint `/api/**` chưa xác thực trả 401 JSON thay vì 302 | Đang chờ | |
| H10 | `@PreAuthorize` có hiệu lực với starter security + webmvc | Đang chờ | |
| H11 | Slice `@WebMvcTest` cần import cấu hình khi có nhiều chain | Đang chờ | |
| H12 | MockMvc `jwt()` không tự thêm header `Authorization` vào chain matcher | Đang chờ | |
