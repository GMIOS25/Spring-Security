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
| D21 | 1043 | Bẫy 1 nêu thông báo lỗi `There is no PasswordEncoder mapped for the id "null"` | Spring Security 7.0.8 cập nhật thông báo lỗi chi tiết hơn: `"Given that there is no default password encoder configured, each password must have a password encoding prefix..."` |
| D22 | B1, 909 | Sử dụng `tools.jackson.databind.ObjectMapper` thay vì `com.fasterxml.jackson.databind.ObjectMapper` trong Spring Boot 4 / Jackson 3 | Import và serialize JSON qua Jackson 3 APIs |
| ST-1 | 1211–1213 | Tuần tự hóa phiên bằng Jackson 3 (ST-1) | Mục tiêu mở rộng (Stretch goal) hoãn lại; app sử dụng cơ chế session in-memory chuẩn của Tomcat/Spring Security SessionRegistry |

---

## 2. Kết quả kiểm chứng giả thuyết (Phần D)

| Giả thuyết | Mô tả | Trạng thái | Bằng chứng thực nghiệm |
|---|---|---|---|
| H1 | Bean `Customizer<HttpSecurity>` tự áp dụng cho mọi chain | **ĐÚNG** | `ModularCustomizerTest` chứng minh `securityHeadersCustomizer` áp dụng header `Content-Security-Policy` tự động qua `HttpSecurityConfiguration` |
| H2 | Login tự viết không chống session fixation nếu không gọi strategy | **ĐÚNG** | `SessionFixationTest` chứng minh việc gọi `sessionAuthenticationStrategy.onAuthentication(...)` kích hoạt `ChangeSessionIdAuthenticationStrategy` làm thay đổi ID phiên sau đăng nhập (S2 ≠ S1), và request mang cookie ID cũ S1 bị từ chối 401. |
| H3 | `XorCsrfTokenRequestAttributeHandler`: header mang token thô bị 403, token che thì được | **ĐÚNG** | `CsrfProtectionTest` chứng minh token thô lấy từ cookie gửi trong header bị từ chối 403 (do handler không thể giải mã mask); trong khi token đã che lấy từ `/api/v1/csrf/token` trả về 201. Hai lần lấy token trong cùng phiên sinh ra 2 chuỗi khác nhau (chống BREACH) và đều hợp lệ. |
| H4 | `BCryptPasswordEncoder` 7.x ném lỗi khi mật khẩu > 72 byte | **ĐÚNG** | `PasswordEncodingTest#bcrypt72` chứng minh `BCryptPasswordEncoder` ném `IllegalArgumentException: password cannot be more than 72 bytes` khi mật khẩu 73 ký tự thay vì âm thầm cắt ngắn |
| H5 | `DaoAuthenticationProvider` tự re-encode khi có `UserDetailsPasswordService` | **ĐÚNG** | `EncodingUpgradeTest#daoAuthenticationProviderUpgradesEncodingOnSuccessfulLogin` chứng minh `DaoAuthenticationProvider` tự động băm lại và nâng cấp hash sang `{pbkdf2}` khi `default-id=pbkdf2` |
| H6 | `JwtDecoder` mặc định từ chối `alg=none`, sai chữ ký, hết hạn | **ĐÚNG** | `JwtVerificationTest` chứng minh token bị sửa payload, token `alg=none`, token ký bằng RSA key khác, token hết hạn quá `clockSkew`, token sai issuer, và token có `typ` khác JWT đều bị từ chối 401 Unauthorized. |
| H7 | `/oauth2/authorization/github` sinh `code_challenge` + `S256` PKCE | **ĐÚNG** | `OAuth2LoginPkceTest#testOAuth2AuthorizationRequestWithPkce` chứng minh request tới endpoint redirect chứa đầy đủ `code_challenge`, `code_challenge_method=S256`, `state`, `redirect_uri` tuyệt đối và không có `response_type=token`. |
| H8 | Đăng nhập mật khẩu + OTT cùng phiên tích lũy đủ 2 yếu tố MFA | **ĐÚNG** | `MfaDeleteAccountTest#testMfaAccountDeletionRequiresBothFactors` chứng minh chỉ mật khẩu hoặc chỉ OTT đều bị 403 Forbidden khi `DELETE /api/v1/users/me`. Khi hoàn tất cả hai bước trong cùng session, session tích lũy cả `FACTOR_PASSWORD` và `FACTOR_OTT`, cho phép xóa tài khoản thành công và dọn sạch dữ liệu. |
| H9 | Endpoint `/api/**` chưa xác thực trả 401 JSON thay vì 302 | **ĐÚNG** | `StatusSemanticsTest#unauthenticatedReturns401Not302` chứng minh ẩn danh `POST /api/v1/posts` trả 401 không chuyển hướng 302 |
| H10 | `@PreAuthorize` có hiệu lực với starter security + webmvc | **ĐÚNG** | `BolaDeleteTest` chứng minh `@PreAuthorize` với SpEL `@postSecurity.isOwner(#postId, authentication)` và `hasAuthority('POST_DELETE_ANY')` hoạt động chuẩn xác với starter security + webmvc và `@EnableMethodSecurity`, không cần thêm thư viện aspectj ngoài. |
| H11 | Slice `@WebMvcTest` cần import cấu hình khi có nhiều chain | **ĐÚNG** | `WebMvcSliceVsFullStackTest` (lớp lồng `WebMvcSliceTest`) chứng minh `@WebMvcTest` với `@MockitoBean PostService` bỏ qua AOP proxy của Spring Security, che giấu lỗi kiểm tra phân quyền; trong khi `FullStackTest` dùng `@SpringBootTest(webEnvironment = RANDOM_PORT)` kết hợp `HttpClient` đi qua toàn bộ chuỗi Servlet và kiểm thử chính xác các tầng bảo mật. |
| H12 | MockMvc `jwt()` không tự thêm header `Authorization` vào chain matcher | **ĐÚNG** | MockMvc `jwt()` post-processor chỉ nạp `SecurityContext` vào request giả lập mà không thêm header HTTP `Authorization: Bearer ...`. Trong kiến trúc 3 chain, request thiếu header sẽ rơi vào chain 3 (bị chặn bởi CSRF hoặc session). `PostControllerSecurityTest#deletePost_WithStatelessJwt_Returns200` chứng minh phải có header `Authorization: Bearer <token>` để `FilterChainProxy` định tuyến đúng vào stateless chain 2. |
