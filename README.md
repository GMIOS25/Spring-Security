# InstaClone — Spring Security 7.x & Spring Boot 4 Demo

Dự án mẫu minh họa toàn diện các nguyên lý, cơ chế kiến trúc và giải pháp phòng thủ các lỗ hổng bảo mật kinh điển trong **Spring Security 7.x** (chạy trên **Spring Boot 4.0.x**, **Java 21**, **H2 in-memory DB**). Dự án hiện thực hóa và kiểm chứng toàn bộ 8 chương của tài liệu `docs/LT.md` theo chỉ dẫn tại `docs/plan.md`.

---

## 1. Cách chạy ứng dụng

### Yêu cầu môi trường
- **Java**: 21 trở lên (`java -version`)
- **Maven**: Dự án tích hợp sẵn Maven Wrapper (`./mvnw` trên Linux/macOS hoặc `mvnw.cmd` trên Windows)

### Khởi chạy môi trường Dev
```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows PowerShell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```
Ứng dụng sẽ khởi động tại cổng mặc định `http://localhost:8080`.
Giao diện người dùng Web Frontend (InstaClone UI) được phục vụ trực tiếp tại root URL:
👉 **`http://localhost:8080/`**

### Giao diện InstaClone Frontend (UI/UX Design)
- **Design System & Aesthetics**: Phong cách Instagram hiện đại, tối ưu độ tương phản (WCAG AA/AAA), hỗ trợ đầy đủ Dark Mode & Light Mode, typography Inter + JetBrains Mono, hiệu ứng Like tim bay (double-tap burst), story highlights bar có thanh tiến trình tự động.
- **Fast 1-Click Role Switcher**: Cho phép chuyển đổi tức thì giữa 5 tài khoản mẫu (`alice`, `bob`, `charlie`, `mod_alice`, `cuong_admin`) với mật khẩu tự động điền sẵn để kiểm thử quyền hạn.
- **Live Spring Security 7.x Telemetry HUD**: Ngăn kéo giám sát kiến trúc bảo mật thời gian thực:
  - Hiển thị trực quan 3 Filter Chains và đánh dấu Chain xử lý request vừa gửi.
  - Phân tích mã CSRF Token được che giấu (XOR masked) chống tấn công BREACH.
  - Bộ công cụ kiểm thử BOLA / IDOR thực nghiệm (minh họa cơ chế trả 404 thay vì 403 để giấu sự tồn tại của bài viết riêng tư).
  - Trạng thái xác thực đa yếu tố MFA (`FACTOR_PASSWORD` + `FACTOR_OTT`).
- **Interactive Features**: Đăng bài mới (chọn ảnh preset/URL, gắn hashtag, chọn visibility PUBLIC / PRIVATE / FOLLOWERS), xóa bài viết (kiểm tra phân quyền BOLA / POST_DELETE_ANY), bình luận trực tiếp, bookmark, và explore theo chủ đề.

### Chạy kiểm thử tự động
```bash
# Chạy toàn bộ 97 test
./mvnw test

# Kiểm tra verify toàn diện
./mvnw clean verify

# Chạy kịch bản smoke test
bash scripts/smoke.sh
```

---

## 2. Dữ liệu mẫu (Seed Data — profile `dev` & `test`)

Tất cả người dùng mẫu đều có mật khẩu chung: `Password123!` (được mã hóa với tiền tố `{bcrypt}` qua `DelegatingPasswordEncoder`).

| Username | Roles (`ROLE_*`) | Authorities bổ sung | Mục đích kiểm thử |
|---|---|---|---|
| `alice` | `ROLE_USER` | - | Người dùng thông thường (sở hữu 1 post PUBLIC, 1 PRIVATE, 1 FOLLOWERS_ONLY) |
| `bob` | `ROLE_USER` | - | Người dùng thông thường (sở hữu 1 post PUBLIC) dùng kiểm thử BOLA/IDOR |
| `charlie` | `ROLE_USER` | - | Người dùng kiểm thử CSRF, phân quyền và đa phiên |
| `mod_alice` | `ROLE_USER`, `ROLE_MODERATOR` | `POST_DELETE_ANY` | Kiểm duyệt viên (có quyền xóa bài vi phạm của người khác) |
| `cuong_admin` | `ROLE_USER`, `ROLE_ADMIN` | `POST_DELETE_ANY` | Quản trị viên hệ thống (quyền quản trị, moderation và xóa bài) |

---

## 3. Bản đồ URL & Phân luồng Endpoint (URL Map)

| Method | Path | Chain | Quy tắc bảo mật | Chương LT.md |
|---|---|---|---|---|
| `GET` | `/api/v1/feed/public/explore` | 1 | `permitAll`, ẩn danh | 2 |
| `POST` | `/api/v1/auth/register` | 2 | `permitAll`, mã hóa mật khẩu ở Service | 3 |
| `POST` | `/api/v1/auth/token` | 2 | `permitAll`, cấp phát Stateless Access + Refresh Token | 7 |
| `POST` | `/api/v1/auth/refresh` | 2 | `permitAll`, xoay vòng Refresh Token (Rotation & Reuse Detection) | 7 |
| `POST` | `/api/v1/auth/logout-all` | 2 | Cần Bearer JWT, thu hồi mọi refresh token và blacklist `jti` | 7 |
| `POST` | `/api/v1/auth/login` | 3 | `permitAll` (cần CSRF) → tạo session và đổi Session ID | 4, 6 |
| `GET` | `/api/v1/csrf/token` | 3 | `permitAll`, sinh mã CSRF token đã che (XOR masked) | 6 |
| `GET` | `/api/v1/auth/me`, `/api/v1/users/me` | 2 hoặc 3 | `authenticated` (đọc claim từ JWT hoặc Principal session) | 2, 4, 7 |
| `POST` | `/api/v1/posts` | 2 hoặc 3 | `authenticated` + Giới hạn tần suất (`InstaRateLimitFilter`) | 2, 6 |
| `GET` | `/api/v1/posts/{id}` | 2 hoặc 3 | `authenticated` + Chính sách ẩn tài nguyên (BOLA Bẫy 2: giấu tồn tại → 404) | 5 |
| `DELETE` | `/api/v1/posts/{id}` | 2 hoặc 3 | `@PreAuthorize("@postSecurity.isOwner(#postId, authentication) or hasAuthority('POST_DELETE_ANY')")` | 5, 8 |
| `POST` | `/api/v1/posts/{id}/comments` | 2 hoặc 3 | `authenticated` + Giới hạn tần suất | 2, 6 |
| `GET` | `/api/v1/moderation/reports` | 2 hoặc 3 | `hasAnyRole('MODERATOR', 'ADMIN')` | 2, 5, 7 |
| `POST` | `/ott/generate` | 3 | `permitAll` (cần CSRF), sinh One-Time Token gửi qua email | 8 |
| `GET` | `/login/ott` | 3 | `permitAll`, render trang xác nhận Magic Link (không tiêu thụ token) | 8 |
| `POST` | `/login/ott` | 3 | `permitAll`, tiêu thụ One-Time Token và cấp `FACTOR_OTT` | 8 |
| `GET` | `/oauth2/authorization/github` | 3 | `permitAll`, OAuth 2.1 với PKCE S256 và exact redirect URI | 8 |
| `DELETE` | `/api/v1/users/me` | 3 | Yêu cầu MFA đa yếu tố: `FACTOR_PASSWORD` + `FACTOR_OTT` | 8 |
| `GET` | `/uploads/private.txt` | 3 | `authenticated` (tài nguyên tĩnh được bảo vệ qua Filter) | 1 |

---

## 4. Kiến trúc 3 Chuỗi Lọc (Security Filter Chains)

Spring Security 7.x sử dụng `FilterChainProxy` để so khớp request theo thứ tự `@Order` tăng dần; chain đầu tiên khớp sẽ xử lý toàn bộ request:

```
                                  HTTP Request
                                       │
                                       ▼
                             ┌───────────────────┐
                             │ FilterChainProxy  │
                             └─────────┬─────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            │ Khớp: GET                │ Khớp: Bearer Header      │ Khớp: Mặc định
            │ /api/v1/feed/public/**   │ hoặc /auth/register,...  │ mọi request còn lại
            ▼                          ▼                          ▼
   ┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐
   │ Chain 1         │        │ Chain 2         │        │ Chain 3         │
   │ @Order(1)       │        │ @Order(2)       │        │ @Order(3)       │
   │ Public Feed     │        │ Stateless API   │        │ Web Session     │
   ├─────────────────┤        ├─────────────────┤        ├─────────────────┤
   │ • permitAll     │        │ • Bearer JWT    │        │ • Session / Form│
   │ • Anonymous     │        │ • CSRF: TẮT     │        │ • CSRF: BẬT     │
   │ • Session:      │        │ • Session:      │        │   (Cookie+XOR)  │
   │   STATELESS     │        │   STATELESS     │        │ • RateLimit     │
   │ • RateLimit     │        │ • RateLimit     │        │ • HttpBasic     │
   │                 │        │ • Blacklist JTI │        │ • OAuth2.1 PKCE │
   │                 │        │                 │        │ • One-Time Token│
   │                 │        │                 │        │ • Multi-Factor  │
   └─────────────────┘        └─────────────────┘        └─────────────────┘
```

### Chi tiết 3 Chain:
1. **Chain 1 (`publicFeedFilterChain`, @Order 1)**:
   - *Matcher*: `GET /api/v1/feed/public/**` (dùng `PathPatternRequestMatcher`)
   - *Cơ chế*: Cho phép truy cập công khai không cần xác thực (`permitAll`). Phiên `STATELESS`.
2. **Chain 2 (`statelessApiFilterChain`, @Order 2)**:
   - *Matcher*: Request có header `Authorization: Bearer ...` HOẶC các endpoint cấp phát credential (`/api/v1/auth/register`, `/api/v1/auth/token`, `/api/v1/auth/refresh`).
   - *Cơ chế*: Xác thực phi trạng thái bằng RSA JWT Resource Server (`NimbusJwtDecoder` + Public Key, xác thực `iss` và `JtiBlacklist`).
   - *CSRF*: Tắt an toàn (vì Bearer token trong header không tự động đính kèm bởi trình duyệt). Phiên `STATELESS`.
3. **Chain 3 (`webSessionFilterChain`, @Order 3)**:
   - *Matcher*: Mặc định (`/**`).
   - *Cơ chế*: Quản lý phiên (`IF_REQUIRED`, chống Session Fixation với `changeSessionId`, giới hạn tối đa 2 phiên đồng thời, `HttpSessionEventPublisher`), `httpBasic`, `oauth2Login` (chuẩn PKCE), `oneTimeTokenLogin` (Magic Link) và xác thực đa nhân tố MFA.
   - *CSRF*: Bật nghiêm ngặt bằng `CookieCsrfTokenRepository.withHttpOnlyFalse()` kết hợp `XorCsrfTokenRequestAttributeHandler` chống tấn công BREACH.
