# PLAN — Xây demo InstaClone thoả mãn `docs/LT.md`

> Đây là **chỉ dẫn thực thi** cho model lập trình (Gemini). Đặt tại `docs/PLAN.md`.
> Nguồn lý thuyết: `docs/LT.md` (8 chương, 2.631 dòng, Spring Security 7.x / Spring Boot 4.x / Java 21).
> Kết quả cuối: một ứng dụng Spring Boot chạy được, có test tự động chứng minh từng khái niệm, từng "Bẫy" và từng câu hỏi tư duy trong LT.md.

---

## 0. Cách dùng (dành cho người)

1. Commit file này vào `docs/PLAN.md`.
2. Lượt đầu, gửi Gemini **Prompt khởi động**.
3. Mỗi phase xong, Gemini nộp _báo cáo phase_ rồi dừng. Bạn tự chạy `./mvnw -q test` để xác nhận (đừng chỉ tin báo cáo), rồi gửi **Prompt tiếp tục**.
4. Kẹt quá 3 lần cùng một lỗi → Gemini ghi `docs/BLOCKERS.md` và dừng.

**Prompt khởi động**

```text
Bạn là kỹ sư Spring Security. Repo GMIOS25/Spring-Security đã nằm trong thư mục làm việc.
Hãy đọc toàn bộ docs/PLAN.md (Phần A, B, C, D) rồi thực hiện Phase 0.
Quy tắc cứng: chỉ đọc docs/LT.md theo đúng dòng được chỉ định; kiểm chứng API bằng javap thay vì đoán;
kết thúc bằng báo cáo theo mẫu A7 và DỪNG. Không làm phase kế tiếp cho tới khi tôi nhắn "Tiếp tục Phase N".
```

**Prompt tiếp tục**

```text
Tiếp tục Phase {N}. Đọc lại Phần A–D và mục Phase {N} trong docs/PLAN.md, đọc LT.md đúng các dòng được chỉ định,
làm theo vòng lặp A3, nộp báo cáo theo mẫu A7 rồi dừng.
```

**Ký hiệu ưu tiên trong các phase:** `[M]` = bắt buộc (khái niệm cốt lõi), `[S]` = nên có (tái hiện "Bẫy"/câu hỏi tư duy). Nếu thiếu thời gian, cắt `[S]` trước, không bao giờ cắt `[M]`.

---

## PHẦN A — LUẬT CHƠI

### A1. Thế nào là "thoả mãn LT.md"

Một khái niệm / bẫy / câu hỏi trong LT.md được coi là **thoả mãn** khi có đủ ba thứ:

1. Code chạy trong `src/main`.
2. Test tự động xanh chứng minh hành vi.
3. Một dòng trong `docs/TRACEABILITY.md` trỏ tới cả hai.

Với mỗi **Bẫy (Gotcha)** cần chứng minh cả hai phía: hành vi sai (tái hiện bằng code _test-only_) và hành vi đúng (code chính).

### A2. Thứ tự nguồn chân lý

1. `docs/LT.md` — khái niệm và hành vi mong đợi.
2. PLAN này — quyết định kiến trúc và cách sửa lỗi code mẫu.
3. Tài liệu chính thức `https://docs.spring.io/spring-security/reference/` (bản 7.x) nếu có mạng.
4. Trí nhớ của bạn — thấp nhất. Spring Boot 4 / Spring Security 7 ra mắt tháng 11/2025; nếu dữ liệu huấn luyện của bạn cũ hơn, hãy mặc định rằng API bạn nhớ từ 5.x/6.x **có thể đã đổi**.

Code trong LT.md là **bản phác thảo** có lỗi và mâu thuẫn (xem Phần C). Khi code mẫu xung đột với khái niệm lý thuyết: giữ khái niệm, sửa code, ghi vào `docs/DEVIATIONS.md`.

### A3. Vòng lặp mỗi phase (đúng thứ tự)

1. Đọc đúng đoạn LT.md: `sed -n 'A,Bp' docs/LT.md`. **Không** `cat` cả file (147 KB).
2. Viết test trước hoặc cùng lúc với code.
3. `./mvnw -q test` đến khi xanh.
4. Cập nhật `docs/TRACEABILITY.md` và `docs/DEVIATIONS.md`.
5. Nộp báo cáo theo A7.
6. `git add -A && git commit -m "<message của phase>"`.
7. **DỪNG.**

### A4. Kiểm chứng API, không đoán

- Gặp `cannot find symbol` / `package does not exist`: không đoán lại. Chạy
  `./mvnw -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt`
  rồi `javap -cp "$(cat /tmp/cp.txt)" <FQCN>` hoặc `unzip -l <jar> | grep <TênLớp>`.
- Mỗi giả thuyết ở Phần D phải được kiểm chứng bằng **test chạy được**. Ghi kết quả (đúng/sai + bằng chứng một dòng) vào `docs/DEVIATIONS.md`, mục "Kết quả kiểm chứng".
- Cùng một lỗi sửa quá 3 lần chưa xong: ghi `docs/BLOCKERS.md` (lỗi, các cách đã thử, giả thuyết), chuyển sang việc khác hoặc dừng và hỏi người dùng.
- Không `@Disabled` test. Không sửa test cho khớp code sai.

### A5. Chuẩn code

- Java 21, constructor injection, `record` cho DTO, không dùng Lombok (LT.md dùng getter viết tay).
- Package theo LT.md: `com.instaclone.{domain.entity, domain.repository, application.dto.request, application.dto.response, application.service, infrastructure.security.{config,filter,model,service,evaluator,antipattern}, presentation.controller}`. Được thêm: `infrastructure.security.jwt`, `infrastructure.security.ott`, `presentation.advice`.
- Spring Security 7: **chỉ Lambda DSL**. Cấm `.and()`, `WebSecurityConfigurerAdapter`, `AntPathRequestMatcher`, `MvcRequestMatcher`, `AccessDecisionManager`, `AccessDecisionVoter`.
- Controller **không bao giờ trả Entity** (tránh lộ `User.password`, tránh lazy loading). Đặt `spring.jpa.open-in-view=false`.
- Không log mật khẩu / token / hash. Không commit secret thật; OAuth client dùng biến môi trường với giá trị dummy mặc định.
- Test: `@DisplayName` tiếng Việt như LT.md; mỗi quy tắc bảo mật có test cho **cả cho phép lẫn từ chối**; lấy ID bài đăng từ DB, không hard-code `105`.

### A6. Điều cấm

- Tắt CSRF cho chain dùng cookie; `allowedOrigins("*")` cùng `allowCredentials(true)`; `permitAll()` rộng kiểu `/api/**`.
- Đổi URL, tên bean hay hành vi đã nêu trong PLAN/LT.md mà không ghi DEVIATIONS.
- Thêm dependency ngoài danh sách B1 mà không ghi DEVIATIONS (kèm lý do).
- Để code "phản mẫu" (interceptor yếu, bean tự gọi nội bộ, `@PostAuthorize` trên thao tác ghi) ở trạng thái **được kích hoạt** trong `src/main`. Chúng chỉ được nằm trong `src/test`, hoặc trong `...security.antipattern` nhưng **không phải bean**.

### A7. Mẫu báo cáo phase

```markdown
## Báo cáo Phase N — <tên>

- Build: <dán 5 dòng cuối `./mvnw test`: Tests run / Failures / Errors / Skipped>
- File thêm/sửa: ...
- Test thêm (tên lớp): ...
- Tiêu chí hoàn thành: [x] ... [ ] ... (mục chưa xong phải nêu lý do)
- Giả thuyết đã kiểm chứng: Hx = đúng/sai — bằng chứng
- Sai lệch so với LT.md/PLAN: DEVIATIONS #...
- Rủi ro / việc còn nợ: ...
```

---

## PHẦN B — QUYẾT ĐỊNH KIẾN TRÚC (ADR) — bắt buộc tuân thủ

### B1. Stack và dependency

- Java 21, Maven Wrapper, Spring Boot **4.0.x** bản GA mới nhất (không dùng milestone/RC). **Không khai version Spring Security**: BOM của Boot quản lý (Security 7.x).
- DB: H2 in-memory (không cần Docker), `ddl-auto=create-drop`.
- Boot 4 đã module hoá và **đổi tên starter**. Tên dưới đây cần xác nhận bằng build:
  - main: `spring-boot-starter-webmvc` (không còn là `-web`), `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security-oauth2-resource-server`, `spring-boot-starter-security-oauth2-client`, `com.h2database:h2`.
  - test: `spring-boot-starter-security-test` (cung cấp `@WithMockUser`), `spring-boot-starter-webmvc-test`, thêm `spring-boot-starter-data-jpa-test` nếu cần.
  - nếu `@PreAuthorize` không có hiệu lực: thêm `spring-boot-starter-aspectj` (giả thuyết H10).
- Jackson 3: package `tools.jackson`, annotation vẫn ở `com.fasterxml.jackson.annotation`.
- Có mạng thì tạo khung bằng `https://start.spring.io` (Boot 4.0.x, Java 21, Maven); không thì tự viết `pom.xml`.

### B2. Ba `SecurityFilterChain` (thay cho 5 chain mâu thuẫn trong LT.md — xem D1)

`FilterChainProxy` chọn chain **khớp đầu tiên** (LT.md L359), nên thứ tự `@Order` là một phần của thiết kế.

| Order | Bean                      | `securityMatcher`                                                                      | Cơ chế                                                      | CSRF                                                                                             | Session                                          |
| ----- | ------------------------- | -------------------------------------------------------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ------------------------------------------------ |
| 1     | `publicFeedFilterChain`   | `GET /api/v1/feed/public/**` (dùng `PathPatternRequestMatcher`)                        | `permitAll`, anonymous                                      | mặc định                                                                                         | `STATELESS`                                      |
| 2     | `statelessApiFilterChain` | header `Authorization: Bearer …` **hoặc** `POST /api/v1/auth/{register,token,refresh}` | `oauth2ResourceServer().jwt()`                              | **tắt** (hợp lệ: chain không dùng cookie/session — LT.md Ch6 Bẫy 1)                              | `STATELESS`                                      |
| 3     | `webSessionFilterChain`   | mặc định (`/**`)                                                                       | session + `httpBasic` + `oauth2Login` + `oneTimeTokenLogin` | **bật**, `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `XorCsrfTokenRequestAttributeHandler` | `IF_REQUIRED`, `changeSessionId`, tối đa 2 phiên |

Lý do: cùng một API `/api/v1/posts/**` phục vụ hai kiểu credential (cookie cho Web, Bearer cho Mobile). Request mang Bearer đi chain 2 (miễn nhiễm CSRF vì không dựa cookie); request còn lại đi chain 3 (bắt buộc CSRF). Các endpoint phát credential không dùng cookie (`register`, `token`, `refresh`) đặt ở chain 2.

### B3. Bảng endpoint

| Method | Path                                   | Chain  | Quy tắc                                                    | Chương |
| ------ | -------------------------------------- | ------ | ---------------------------------------------------------- | ------ |
| GET    | `/api/v1/feed/public/explore`          | 1      | permitAll                                                  | 2      |
| POST   | `/api/v1/auth/register`                | 2      | permitAll                                                  | 3      |
| POST   | `/api/v1/auth/login`                   | 3      | permitAll (cần CSRF) → tạo session                         | 4      |
| GET    | `/api/v1/auth/me`, `/api/v1/users/me`  | 3 và 2 | authenticated                                              | 2, 4   |
| GET    | `/api/v1/csrf/token`                   | 3      | permitAll                                                  | 6      |
| POST   | `/api/v1/auth/token`                   | 2      | permitAll → trả access + refresh token                     | 7      |
| POST   | `/api/v1/auth/refresh`                 | 2      | permitAll (refresh token trong body)                       | 7      |
| POST   | `/api/v1/auth/logout-all`              | 2      | cần JWT                                                    | 7      |
| POST   | `/api/v1/posts`                        | 2/3    | authenticated + rate limit                                 | 2      |
| GET    | `/api/v1/posts/{id}`                   | 2/3    | authenticated + chính sách hiển thị (không đủ quyền → 404) | 5      |
| DELETE | `/api/v1/posts/{id}`                   | 2/3    | `isOwner or hasAuthority('POST_DELETE_ANY')`               | 5      |
| POST   | `/api/v1/posts/{id}/comments`          | 2/3    | authenticated + rate limit                                 | 2, 6   |
| GET    | `/api/v1/moderation/reports`           | 2/3    | `hasAnyRole('MODERATOR','ADMIN')`                          | 2      |
| POST   | `/ott/generate`, GET/POST `/login/ott` | 3      | One-Time Token                                             | 8      |
| GET    | `/oauth2/authorization/github`         | 3      | OAuth2 login (PKCE)                                        | 8      |
| DELETE | `/api/v1/users/me`                     | 3      | MFA: `FACTOR_PASSWORD` + `FACTOR_OTT`                      | 8      |
| GET    | `/uploads/private.txt` (static)        | 3      | authenticated                                              | 1      |

### B4. Dữ liệu mẫu (chỉ profile `dev`, `test`; không bao giờ `prod`)

Mật khẩu mọi user: `Password123!` (băm qua `PasswordEncoder`, có tiền tố `{bcrypt}`).

| username                  | roles (lưu dạng `ROLE_*`)     |
| ------------------------- | ----------------------------- |
| `alice`, `bob`, `charlie` | `ROLE_USER`                   |
| `mod_alice`               | `ROLE_USER`, `ROLE_MODERATOR` |
| `cuong_admin`             | `ROLE_USER`, `ROLE_ADMIN`     |

Bài đăng: `alice` có 1 `PUBLIC` + 1 `PRIVATE` + 1 `FOLLOWERS_ONLY` (coi như không công khai: chỉ tác giả xem được); `bob` có 1 `PUBLIC`.

Role → authority (Ch1 §2.3, Ch5 §2.2): `ROLE_MODERATOR` và `ROLE_ADMIN` đều kèm authority `POST_DELETE_ANY`; `ROLE_USER` thì không. `InstaCloneUserDetails` cấp cả `ROLE_*` lẫn authority; claim `roles` của JWT mang cả hai loại.

### B5. Cấu hình qua property

| Property                                                      | Mặc định                       | Ghi chú                                       |
| ------------------------------------------------------------- | ------------------------------ | --------------------------------------------- |
| `instaclone.security.require-https`                           | `false` (`true` ở `prod`)      | bật `redirectToHttps` (D8)                    |
| `instaclone.security.cors.allowed-origins`                    | `http://localhost:3000` (dev)  | danh sách tường minh                          |
| `instaclone.ratelimit.max-per-minute`                         | `60` (test: `3`)               | D10                                           |
| `instaclone.jwt.issuer`                                       | `https://api.instaclone.com`   |                                               |
| `instaclone.jwt.access-ttl` / `refresh-ttl`                   | `PT15M` / `P7D`                |                                               |
| `instaclone.security.password.default-id`                     | `bcrypt`                       | đổi sang `pbkdf2` để demo nâng cấp thuật toán |
| `instaclone.security.password.bcrypt-strength`                | `10`                           |                                               |
| `spring.security.oauth2.client.registration.github.client-id` | `${GITHUB_CLIENT_ID:dummy-id}` | D18                                           |
| `spring.jpa.open-in-view`                                     | `false`                        | A5                                            |

### B6. Phác thảo cấu hình (CHƯA biên dịch — chỉ là hướng dẫn, phải kiểm chứng bằng build)

```java
@Configuration @EnableWebSecurity @EnableMethodSecurity
@EnableMultiFactorAuthentication(authorities = {})   // Phase 8: bật cơ chế gộp yếu tố, không ép yếu tố cho mọi rule
class SecurityChainsConfig {

  @Bean @Order(1)
  SecurityFilterChain publicFeedFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/feed/public/**"))
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
  }

  @Bean @Order(2)
  SecurityFilterChain statelessApiFilterChain(HttpSecurity http, JwtAuthenticationConverter conv) throws Exception {
    http.securityMatcher(new OrRequestMatcher(bearerHeaderMatcher(), credentialIssuingEndpoints()))
        .csrf(c -> c.disable())                       // hợp lệ: chain này không dùng cookie
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/token", "/api/v1/auth/refresh").permitAll()
            .requestMatchers("/api/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conv)))
        .addFilterBefore(new InstaRateLimitFilter(/*...*/), BearerTokenAuthenticationFilter.class); // D10: không @Component
    return http.build();
  }

  @Bean @Order(3)
  SecurityFilterChain webSessionFilterChain(HttpSecurity http /*, ... */) throws Exception {
    // csrf(cookie + Xor) · cors(properties) · headers · sessionManagement(fixation, maximumSessions(2).sessionRegistry(reg))
    // authorizeHttpRequests(specific → generic) · httpBasic · exceptionHandling(401 JSON cho /api/**)
    // oauth2Login · oneTimeTokenLogin · redirectToHttps (có điều kiện) · addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
    return http.build();
  }
}

// Session cho login tự viết (D9): dùng CÙNG SessionRegistry với DSL
@Bean SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
@Bean HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }
@Bean SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry reg) {
  var concurrency = new ConcurrentSessionControlAuthenticationStrategy(reg);
  concurrency.setMaximumSessions(2);
  concurrency.setExceptionIfMaximumExceeded(false);
  return new CompositeSessionAuthenticationStrategy(List.of(
      concurrency, new ChangeSessionIdAuthenticationStrategy(), new RegisterSessionAuthenticationStrategy(reg)));
}
// AuthenticationController.login(): gọi sessionAuthenticationStrategy.onAuthentication(auth, req, res)
// TRƯỚC securityContextRepository.saveContext(...)
```

---

## PHẦN C — LỖI / MÂU THUẪN TRONG LT.md PHẢI XỬ LÝ

Số dòng tham chiếu `docs/LT.md`. Mỗi mục xử lý xong phải có dòng trong `docs/DEVIATIONS.md` (mã `D#`).

| #   | Dòng LT.md                                          | Vấn đề                                                                                                                                                                                                                                                 | Xử lý bắt buộc                                                                                                                                                                                                                                                                                        |
| --- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| D1  | 391–444, 1519–1552, 1785–1850, 2148–2209, 2395–2466 | Có 5 bean `SecurityFilterChain`; hai bean trùng tên `securityFilterChain`; không cái nào có `securityMatcher` → app không khởi động (bean override / "a filter chain that matches any request has already been configured").                           | Gộp thành 3 chain theo B2. Giữ nguyên _quy tắc_ từng đoạn, bỏ cấu trúc cũ.                                                                                                                                                                                                                            |
| D2  | 2418                                                | `csrf.disable()` trong chain có cookie/session (oauth2Login, OTT, httpBasic) — mâu thuẫn Chương 6 Bẫy 1 (L1908–1914).                                                                                                                                  | CSRF luôn bật ở chain 3; test dùng `.with(csrf())`.                                                                                                                                                                                                                                                   |
| D3  | 2518–2538                                           | `@MockBean` và import `org.springframework.boot.test.*` là kiểu Boot 3. Boot 4 bỏ `@MockBean` (dùng `@MockitoBean`) và chuyển package test-autoconfigure theo module.                                                                                  | Sửa theo lỗi biên dịch, ghi DEVIATIONS.                                                                                                                                                                                                                                                               |
| D4  | 2555–2565                                           | Test 3 mock `PostService` rồi `doThrow(AccessDeniedException)` → chỉ chứng minh Mockito, **không** chứng minh `@PreAuthorize`/chống BOLA (mock không đi qua AOP proxy).                                                                                | Thay bằng test tích hợp dùng `PostService` + `PostSecurityEvaluator` thật + H2, assert bài vẫn còn trong DB (Phase 5, 8).                                                                                                                                                                             |
| D5  | 2583–2587                                           | Test 5 dùng `jwt()` mà không nói rõ chain nào xử lý.                                                                                                                                                                                                   | Xác định theo B2; xem H12.                                                                                                                                                                                                                                                                            |
| D6  | 1592–1595, 1667–1670                                | `isOwner` trả `true` khi bài không tồn tại → service ném `IllegalArgumentException` (mặc định thành 500). `@PostAuthorize` trả 403 cho bài PRIVATE của người khác → lộ sự tồn tại (chính là Bẫy 2).                                                    | `ResourceNotFoundException` → 404. Chính sách "ẩn": người không đủ quyền xem → 404. Giữ biến thể `@PostAuthorize` thuần trong test-only để chứng minh bẫy.                                                                                                                                            |
| D7  | 205, 1645–1650                                      | Service trả entity `Post`: `author` là LAZY; `@PostAuthorize` đọc `returnObject.author.username` có nguy cơ `LazyInitializationException`; serialize entity lộ hash mật khẩu của `User`.                                                               | Service trả `PostResponse` (record: id, caption, imageUrl, authorUsername, visibility). SpEL dùng gọi hàm `returnObject.visibility()`. Truy vấn bằng `join fetch` hoặc `@EntityGraph`.                                                                                                                |
| D8  | 1814, 1860                                          | `redirectToHttps` bật vô điều kiện → mọi request HTTP (localhost, MockMvc) bị 302 sang https. Origin CORS cứng theo domain production.                                                                                                                 | Điều kiện hoá bằng `instaclone.security.require-https`; origins đọc từ `instaclone.security.cors.allowed-origins`.                                                                                                                                                                                    |
| D9  | 1260–1297 vs 1829–1835                              | Login tự viết trong Controller không đi qua `SessionAuthenticationStrategy` (vốn do `AbstractAuthenticationProcessingFilter` gọi) → `changeSessionId` / `maximumSessions` có nguy cơ không có tác dụng.                                                | Gọi strategy thủ công trước `saveContext` (xem B6). Chứng minh bằng test Phase 6 (H2).                                                                                                                                                                                                                |
| D10 | 511–558, 566–584                                    | Rate limit: bộ đếm không bao giờ reset (khoá vĩnh viễn sau 60 request); tin `X-Forwarded-For` do client gửi (giả mạo để né giới hạn); map phình vô hạn. "Cách 1" của Bẫy 1 (`@Bean`) vẫn bị Boot tự đăng ký vì **mọi** bean `Filter` đều được đăng ký. | Cửa sổ cố định 1 phút (inject `Clock`), dọn entry cũ, dùng `getRemoteAddr()` (sau proxy: `server.forward-headers-strategy`), ngưỡng cấu hình được. Không `@Component`/`@Bean Filter`: dùng `new` trong cấu hình chain, hoặc `FilterRegistrationBean(enabled=false)`. Test: chạy đúng 1 lần/request.   |
| D11 | 138–152, 832–859, 891                               | `User` thiếu `getEmail()` và setter; `Set.of(..)` bất biến gán vào `@ElementCollection`; thiếu `UserRepository`/`PostRepository`; `IllegalArgumentException` → 500.                                                                                    | Bổ sung; dùng `new HashSet<>(Set.of(..))`; `@RestControllerAdvice` map 400/401/403/404/409.                                                                                                                                                                                                           |
| D12 | 775                                                 | `@Size(max=100)` cho mật khẩu nhưng BCrypt giới hạn 72 byte.                                                                                                                                                                                           | Kiểm chứng H4; hạ `max` theo kết quả (đề xuất 72) + test.                                                                                                                                                                                                                                             |
| D13 | 2455–2456, 2372                                     | In token ra `System.out`. LT.md nói `/ott/generate` "kèm email" nhưng filter của Spring đọc tham số `username`.                                                                                                                                        | `MagicLinkMailer` giả (lưu hộp thư trong bộ nhớ cho test; log dạng che `abc***`). API nhận `username`, gửi tới email của user đó; phản hồi **giống nhau** dù user có tồn tại hay không.                                                                                                               |
| D14 | 430, 2181, 1630 vs 98                               | Moderator/Admin lúc `hasAnyRole`, lúc `hasRole("MODERATOR")`; Chương 1 nói quyền `POST_DELETE_ANY` thuộc cả MODERATOR lẫn ADMIN.                                                                                                                       | Authority `POST_DELETE_ANY` map từ role (B4). URL moderation: `hasAnyRole('MODERATOR','ADMIN')`. Xoá bài: `isOwner or hasAuthority('POST_DELETE_ANY')`.                                                                                                                                               |
| D15 | 2098–2142                                           | Khoá RSA sinh trong constructor → restart làm mất hiệu lực mọi token; `JwtDecoder` không kiểm `iss`, không kiểm blacklist; `kid` ngẫu nhiên mỗi lần.                                                                                                   | Thêm `JwtIssuerValidator` và `JtiBlacklistValidator` qua `JwtValidators.createDefaultWithValidators(...)`. Tách `...security.jwt.auth` (encoder + private key) và `...security.jwt.resource` (decoder + public key). (Stretch) nạp khoá từ PEM.                                                       |
| D16 | 2054–2064 vs 2240–2259                              | Lý thuyết có Refresh Token, "đăng xuất mọi thiết bị", blacklist `jti` + TTL; code chỉ có `generateAccessToken`.                                                                                                                                        | Hiện thực đầy đủ ở Phase 7.                                                                                                                                                                                                                                                                           |
| D17 | 286, 1302, 2430, 2486                               | `/users/me` và `/auth/me` dùng lẫn lộn.                                                                                                                                                                                                                | Cung cấp cả hai (`/auth/me` là alias).                                                                                                                                                                                                                                                                |
| D18 | 2429–2438                                           | `oauth2Login()` bắt buộc có `ClientRegistrationRepository` (thiếu → app không khởi động); `oneTimeTokenLogin` cần `UserDetailsService`; cùng `httpBasic` có thể làm request chưa xác thực bị 302 `/login` thay vì 401.                                 | `application.yml` đăng ký `github` với giá trị dummy từ env. Entry point tường minh trả 401 JSON cho `/api/**` (H9).                                                                                                                                                                                  |
| D19 | 2360–2366 vs 2424, 2493                             | Lý thuyết nêu `@EnableMultiFactorAuthentication` và `AllRequiredFactorsAuthorizationManager`; code chỉ có `hasAuthority("FACTOR_OTT")` → không ép yếu tố mật khẩu, không bật cơ chế gộp yếu tố.                                                        | `@EnableMultiFactorAuthentication(authorities = {})` + `AuthorizationManagerFactories.multiFactor().requireFactors(PASSWORD_AUTHORITY, OTT_AUTHORITY)` **chỉ** cho `DELETE /api/v1/users/me`. Kiểm tên lớp bằng `javap`; nếu `AllRequiredFactorsAuthorizationManager` không tồn tại → ghi DEVIATIONS. |
| D20 | 1040                                                | Sơ đồ ghi `ProviderManager` gọi `SecurityContextHolder.setAuthentication`.                                                                                                                                                                             | Thực tế filter/controller làm việc đó (chính code L1273–1278). Nêu trong `docs/QA.md`, không sửa LT.md.                                                                                                                                                                                               |

---

## PHẦN D — GIẢ THUYẾT CẦN KIỂM CHỨNG BẰNG TEST

Đây là những điểm tôi **chưa kiểm chứng được** (không build được trong môi trường soạn plan). Đừng coi là sự thật: viết test, chạy, ghi kết quả vào `docs/DEVIATIONS.md` mục "Kết quả kiểm chứng".

| #   | Giả thuyết                                                                                                                                                                       | Test                                                                    | Nếu sai                                                                                                                                                                     |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| H1  | Bean `Customizer<HttpSecurity>` được tự áp dụng cho mọi chain (L448–480).                                                                                                        | `ModularCustomizerTest`: header `Content-Security-Policy` xuất hiện.    | Tiêm `List<Customizer<HttpSecurity>>` và gọi `customize(http)` tường minh trong từng chain.                                                                                 |
| H2  | Login tự viết không chống session fixation nếu không gọi strategy (D9).                                                                                                          | `SessionFixationTest`: ID phiên trước ≠ sau đăng nhập; cookie cũ → 401. | Luôn phải đạt "ID đổi"; chỉnh cách gọi strategy.                                                                                                                            |
| H3  | Với `XorCsrfTokenRequestAttributeHandler` + `CookieCsrfTokenRepository`: header chứa giá trị **thô** của cookie bị 403, giá trị **đã che** lấy từ `/api/v1/csrf/token` thì được. | `CsrfProtectionTest`                                                    | Nếu giá trị thô cũng qua: ghi nhận, không đổi code.                                                                                                                         |
| H4  | `BCryptPasswordEncoder` 7.x ném lỗi khi mật khẩu > 72 byte (thay vì cắt im lặng).                                                                                                | `PasswordEncodingTest#bcrypt72`                                         | Dựa kết quả để đặt `max` (D12).                                                                                                                                             |
| H5  | `DaoAuthenticationProvider` tự băm lại khi `UserDetailsService` cài thêm `UserDetailsPasswordService` và `upgradeEncoding()==true`.                                              | `EncodingUpgradeTest`                                                   | Tự băm lại sau đăng nhập thành công (listener `AuthenticationSuccessEvent`).                                                                                                |
| H6  | `JwtDecoder` mặc định: từ chối `alg=none`, chữ ký khác khoá, token hết hạn (quá `clockSkew` 60 giây), `typ` ngoài `JWT`; **không** kiểm `iss` nếu ta không thêm validator.       | `JwtVerificationTest` (từng ca một)                                     | Ghi kết quả từng ca.                                                                                                                                                        |
| H7  | `/oauth2/authorization/github` có `code_challenge` + `S256` ngay cả với client confidential.                                                                                     | `OAuth2LoginPkceTest`                                                   | Bật bằng `OAuth2AuthorizationRequestCustomizers.withPkce()` trên resolver (kiểm tên bằng javap).                                                                            |
| H8  | Đăng nhập mật khẩu tự thêm `FACTOR_PASSWORD`; sau OTT trong cùng phiên, hai yếu tố cùng tồn tại khi MFA bật.                                                                     | `MfaDeleteAccountTest`                                                  | Đọc tài liệu MFA 7.x, điều chỉnh, ghi lại.                                                                                                                                  |
| H9  | Với oauth2Login + OTT + httpBasic cùng cấu hình, request `/api/**` chưa xác thực vẫn trả 401 (không 302), có hay không có header `Accept`.                                       | `StatusSemanticsTest`                                                   | Thêm `exceptionHandling.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(UNAUTHORIZED), <match /api/**>)`.                                                       |
| H10 | `@PreAuthorize` có hiệu lực chỉ với starter security + webmvc.                                                                                                                   | `BolaDeleteTest` (Bob xoá bài Alice phải 403)                           | Thêm `spring-boot-starter-aspectj`.                                                                                                                                         |
| H11 | Slice `@WebMvcTest` cần import nhiều bean hơn khi có 3 chain.                                                                                                                    | `WebMvcSliceVsFullStackTest`                                            | Test bảo mật chính dùng `@SpringBootTest` + `@AutoConfigureMockMvc`.                                                                                                        |
| H12 | `jwt()` post-processor của MockMvc **không** thêm header `Authorization`.                                                                                                        | một test nhỏ                                                            | Nếu không có header → request rơi vào chain 3. Test chain 2 phải thêm `Authorization: Bearer …` kèm (a) token thật từ `JwtTokenService` hoặc (b) `@MockitoBean JwtDecoder`. |

---

## PHẦN E — CÁC PHASE

Mỗi phase: **Đọc** (dòng LT.md) → **Làm** → **Test** → **Hoàn thành khi** → **Commit**. Tên lớp test là cố định để khớp ma trận ở Phần F.

### Phase 0 — Khởi tạo và nền móng

**Đọc:** `sed -n '1,5p'` (chỉ để lấy phiên bản nền tảng).

**Làm**

- Kiểm tra `java -version` (≥ 21) và `./mvnw -v`. Tạo khung dự án theo B1; lớp `InstaCloneApplication`; cấu trúc package theo A5.
- `application.yml` với các profile `dev`, `test`, `prod` và các property ở B5 (`open-in-view=false`).
- Dọn repo: `git rm --cached "docs/~\$LT.docx"` (file khoá tạm của Word bị commit nhầm); `.gitignore` thêm `target/`, `.idea/`, `*.iml`, `.vscode/`, `~$*` (giữ dòng `.codex-work`).
- Tạo `docs/DEVIATIONS.md` (hai mục: "Sai lệch" và "Kết quả kiểm chứng"), `docs/TRACEABILITY.md` (chép bảng ở Phần F), `docs/BLOCKERS.md` (rỗng).

**Test:** `[M]` `ApplicationContextTest` — context khởi động được ở profile `test`.

**Hoàn thành khi:** `./mvnw -q test` xanh; `git status` sạch sau commit.

**Commit:** `chore: bootstrap Spring Boot 4 project and tracking docs`

---

### Phase 1 — Chương 1: Ranh giới tin cậy và ngữ nghĩa 401/403

**Đọc:** `sed -n '7,274p'` (code ở 102–250, bẫy 253–263, câu hỏi 267–270).

**Làm**

- Domain: `User` (bổ sung `getEmail()`, setter cần thiết — D11), `Post`, `Comment` tối thiểu; `UserRepository` (`existsByUsername`, `existsByEmail`, `findByUsername`), `PostRepository`, `CommentRepository`.
- `FragileSecurityInterceptor` nguyên văn L213–247 vào `...security.antipattern` — **không** đăng ký thành bean/`WebMvcConfigurer`.
- `@RestControllerAdvice` (`presentation.advice`): 400 validation, 401, 403, 404 (`ResourceNotFoundException`), 409; body JSON thống nhất `{error, message}`.
- Chain tạm thời duy nhất: `anyRequest().authenticated()` + `httpBasic` (Phase 2 sẽ thay thế).
- Tài nguyên tĩnh `src/main/resources/static/uploads/private.txt`.

**Test**

- `[M]` `AntiPatternInterceptorTest` — gọi `preHandle` trực tiếp bằng `MockHttpServletRequest`: (a) `DELETE /api/v1/posts` (không có dấu `/` cuối) → `true` (lọt, SAI LẦM 1); (b) header giả `X-User-Role: ADMIN` → `true` (SAI LẦM 2).
- `[M]` `StatusSemanticsTest` — ẩn danh `POST /api/v1/posts` → **401**; user thường gọi `GET /api/v1/moderation/reports` → **403** (kiểm chứng H9: không đăng nhập thì 401, không 302).
- `[M]` `DenyByDefaultTest` — ẩn danh vào đường dẫn không tồn tại `/api/v1/anything` → 401 (chặn mặc định, không lộ 404).
- `[M]` `StaticResourceProtectedTest` — ẩn danh `GET /uploads/private.txt` → 401 (Filter bảo vệ được cả thứ nằm ngoài Controller mà Interceptor không với tới — L23, L243–245).
- `[S]` `FirewallVsInterceptorTest` — URL kiểu `/api/v1/posts/..;/x`, `//api/v1/posts/1` bị `StrictHttpFirewall` từ chối (ghi mã trạng thái thực tế) trong khi so khớp chuỗi thủ công của interceptor thì sót (L231–232).

**Hoàn thành khi:** test xanh; TRACEABILITY có đủ hàng Chương 1.

**Commit:** `feat(ch1): domain model, anti-pattern demo, 401/403 semantics`

---

### Phase 2 — Chương 2: Filter Chain và cấu hình hiện đại

**Đọc:** `sed -n '276,599p'` (code 385–558, bẫy 564–590, câu hỏi 594–597) và `sed -n '2072,2142p'` (khoá RSA tối thiểu để chain 2 biên dịch được).

**Làm**

- Thay chain tạm bằng `publicFeedFilterChain` (Order 1) và `webSessionFilterChain` (Order 3) theo B2/B6; thêm khung `statelessApiFilterChain` (Order 2) dùng `JwtCryptographyConfig` bản tối thiểu (sẽ nâng cấp ở Phase 7).
- `requestMatchers(...)` dạng chuỗi (PathPattern); `securityMatcher` của chain 1 dùng `PathPatternRequestMatcher` tường minh. Quy tắc từ **cụ thể** đến **tổng quát** (L586–590).
- `InstaRateLimitFilter` đã sửa theo D10, kế thừa `OncePerRequestFilter`.
- `ModularSecurityConfig` với `Customizer<HttpSecurity>` đặt CSP (L448–480) — kiểm chứng H1.
- Endpoint tối thiểu: `GET /api/v1/feed/public/explore`, `POST /api/v1/posts` (stub trả 201 sau khi có `PostService` ở Phase 5), `POST /api/v1/posts/{id}/comments`.

**Test**

- `[M]` `FilterChainIntrospectionTest` — bean `springSecurityFilterChain` là `FilterChainProxy`; liệt kê `getFilterChains()` đúng thứ tự (số chain hiện có; **cập nhật khi thêm chain 2 ở Phase 7**); với mỗi URL mẫu, chain được chọn là chain đầu tiên khớp; chain 3 chứa `CsrfFilter`, `BasicAuthenticationFilter` và rate-limit filter, chain 1 không chứa `BasicAuthenticationFilter`; `AuthorizationFilter` đứng cuối mỗi chain (L365).
- `[M]` `PermitAllStillTraversesFilterChainTest` (Q1 Ch2) — `GET /api/v1/feed/public/explore` vẫn đi qua các filter của chain 1. Dùng một `Customizer<HttpSecurity>` test-only thêm filter ném `RuntimeException` khi có header `X-Boom`; khi đó controller **không** được gọi (`verifyNoInteractions`).
- `[M]` `RateLimitFilterTest` — với `max-per-minute=3`: lần thứ 4 `POST /api/v1/posts` cùng IP → 429 JSON; sau khi đẩy `Clock` thêm 61 giây → lại 201; header `X-Forwarded-For` giả **không** né được giới hạn; `GET` không bị đếm.
- `[M]` `FilterRunsOnceTest` (Bẫy 1) — `@MockitoSpyBean`/bộ đếm xác nhận filter chạy **đúng 1 lần** mỗi request (nếu để `@Component` + `addFilterBefore` thì sẽ là 2).
- `[M]` `RemovedApisClaimsTest` (L290–296) — quét `src/main/java` bằng regex để chắc chắn không còn `WebSecurityConfigurerAdapter|AntPathRequestMatcher|MvcRequestMatcher|\.and\(\)`; đồng thời `Class.forName` cho 3 lớp đầu **ném** `ClassNotFoundException` trên classpath hiện tại (nếu lớp vẫn tồn tại → ghi DEVIATIONS rằng LT.md nói quá).
- `[S]` `AuthorizationRuleOrderTest` (Bẫy 2) — chain test-only trên prefix riêng với `permitAll("/x/**")` đặt **trước** rule `hasRole` → ẩn danh lọt (tái hiện lỗi); chain đúng thứ tự → 401.
- `[M]` `ModularCustomizerTest` — kết quả H1.

**Hoàn thành khi:** test xanh; `docs/DEVIATIONS.md` có D1, D10 và kết quả H1.

**Commit:** `feat(ch2): 3 security filter chains, rate limit filter, modular customizer`

---

### Phase 3 — Chương 3: Mật khẩu và đăng ký

**Đọc:** `sed -n '601,929p'` (code 730–897, bẫy 901–919, câu hỏi 923–926).

**Làm**

- `PasswordEncoderConfig`: dựng `DelegatingPasswordEncoder` tường minh với `bcrypt` (strength từ property) và `pbkdf2`; `defaultId` từ property B5. Không có `noop`.
- `RegisterRequest` (mật khẩu 8–72, D12), `UserProfileResponse`, `UserService.registerUser` (băm **một lần** ở Service — L914), `AuthRegistrationController` (`POST /api/v1/auth/register`, thuộc chain 2).
- `UserService.updateProfile(...)` **không** động đến mật khẩu (Q1 L925).
- `DataSeeder` (B4) cho profile `dev`/`test`.

**Test**

- `[M]` `PasswordEncodingTest` — hai user cùng `Password123!` → hash khác nhau; hash bắt đầu `{bcrypt}$2a$10$`; phần sau tiền tố dài 60 ký tự và tách được 4 phần (phiên bản / cost / salt 22 ký tự / hash 31 ký tự) đúng L715–718; `matches` đúng/sai; `bcrypt72` (H4).
- `[M]` `RegisterApiTest` — 201; JSON trả về **không** chứa `password` hay `$2a$`; DB lưu bắt đầu `{bcrypt}`; trùng username → 409; email sai → 400.
- `[M]` `EncodingUpgradeTest` (H5, L686) — đặt `default-id=pbkdf2`, user seed dùng `{bcrypt}` → đăng nhập thành công → DB giờ bắt đầu `{pbkdf2}` và vẫn đăng nhập lại được. Cài `UserDetailsPasswordService` trong `InstaCloneUserDetailsService` (Phase 4) rồi hoàn thiện test tại đó.
- `[S]` `MissingPrefixTest` (Bẫy 1) — `matches("x", "$2a$10$…")` ném `IllegalArgumentException` có chuỗi `There is no PasswordEncoder mapped for the id "null"`; sau `setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder())` thì không còn ném.
- `[S]` `DoubleEncodingTest` (Bẫy 2) — `encode(encode(p))` làm `matches(p)` thành `false`; và sau `updateProfile` user vẫn đăng nhập được.
- `[S]` `CostFactorBenchmarkTest` (Bẫy 3, tag `benchmark`, mặc định không chạy) — in thời gian `encode` ở strength 10/12/14; chỉ assert strength 10 dưới ngưỡng rộng (≤ 1500 ms) để không flaky; ghi số liệu vào DEVIATIONS.

**Hoàn thành khi:** test xanh (trừ `EncodingUpgradeTest` có thể hoàn tất ở Phase 4 — ghi rõ trong báo cáo).

**Commit:** `feat(ch3): delegating password encoder and registration`

---

### Phase 4 — Chương 4: Luồng xác thực và SecurityContext

**Đọc:** `sed -n '932,1357p'` (lõi 959–1062, code 1065–1317, bẫy 1323–1347, câu hỏi 1351–1355).

**Làm**

- `InstaCloneUserDetails` (cấp `ROLE_*` + authority suy ra từ role — B4), `InstaCloneUserDetailsService implements UserDetailsService, UserDetailsPasswordService`, bean `AuthenticationManager`, `LoginRequest`.
- `AuthenticationController.login`: `authenticate` → tạo context bằng `SecurityContextHolderStrategy` → **gọi `SessionAuthenticationStrategy`** (B6, D9) → `saveContext` tường minh → trả thông báo **chung** khi thất bại (L1283–1289). `GET /api/v1/auth/me` và `GET /api/v1/users/me` (với JWT thì đọc từ claim; Phase 7).
- `AsyncConfig`: `@EnableAsync` + `DelegatingSecurityContextAsyncTaskExecutor`; một `@Async` demo trả `CompletableFuture<String>` là tên principal.

**Test**

- `[M]` `LoginFlowTest` — `GET /api/v1/csrf/token` (Phase 6 hoàn thiện; tạm dùng `.with(csrf())`) → login → 200 + cookie `JSESSIONID` → `GET /api/v1/auth/me` với session đó → 200 và `username` đúng; không có session → 401.
- `[M]` `UsernameEnumerationTest` (Bẫy 2) — user không tồn tại và user đúng + sai mật khẩu cho **cùng** status 401 và **cùng** body.
- `[M]` `CredentialsErasedTest` (Q2) — sau `authenticate`, `getCredentials()` là `null`.
- `[M]` `SecurityContextIsolationTest` — sau mỗi request, `SecurityContextHolder` sạch (hai request tuần tự không rò rỉ user, L1057).
- `[S]` `ForgetfulLoginTest` (Bẫy 1) — controller test-only đặt `Authentication` mà **không** `saveContext` → request kế tiếp 401, trong khi login thật thì 200.
- `[S]` `AsyncContextPropagationTest` (Bẫy 3) — `@Async` với executor mặc định thấy `null`; với `DelegatingSecurityContextAsyncTaskExecutor` thấy đúng username.
- `[M]` hoàn tất `EncodingUpgradeTest` (H5).

**Hoàn thành khi:** test xanh; ghi kết quả H5.

**Commit:** `feat(ch4): authentication flow with explicit save and security context`

---

### Phase 5 — Chương 5: Phân quyền đa tầng và chống BOLA/IDOR

**Đọc:** `sed -n '1359,1685p'` (kiến trúc 1393–1493, code 1497–1651, bẫy 1657–1675, câu hỏi 1679–1683).

**Làm**

- `@EnableMethodSecurity`; `PostSecurityEvaluator` (`@Component("postSecurity")`); `PostService` (tạo/xoá/xem chi tiết) trả `PostResponse` (D7); `PostController`, `CommentController`, `ModerationController` (`GET /api/v1/moderation/reports` trả danh sách giả).
- Xoá bài: `@PreAuthorize("@postSecurity.isOwner(#postId, authentication) or hasAuthority('POST_DELETE_ANY')")`. Bài không tồn tại → 404 (D6).
- Xem chi tiết: bài PRIVATE/FOLLOWERS_ONLY của người khác → **404** (chính sách ẩn, Bẫy 2).
- Test-only (trong `src/test`): bean `SelfInvocationDemo` và bean `PostAuthorizeOnWriteDemo` để tái hiện Bẫy 1 và Bẫy 3.

**Test** (tích hợp `@SpringBootTest` + `@AutoConfigureMockMvc` + H2 + `@WithUserDetails`; mọi request ghi dùng `.with(csrf())`)

- `[M]` `BolaDeleteTest` — Bob xoá bài của Alice → **403** và `postRepository.existsById(id)` vẫn `true`; Alice xoá → thành công và bài biến mất; `mod_alice` và `cuong_admin` xoá bài của Bob → thành công; ẩn danh → 401; id không tồn tại (Bob) → **404**. (Kiểm chứng H10.)
- `[M]` `PostVisibilityTest` — Bob xem bài PUBLIC của Alice → 200; Bob xem bài PRIVATE của Alice → **404**; Alice xem bài PRIVATE của chính mình → 200. Thêm biến thể `@PostAuthorize` thuần (test-only) trả **403** để chứng minh Bẫy 2 (lộ sự tồn tại).
- `[M]` `HasRoleVsHasAuthorityTest` (L1455–1457) — bean test-only có `hasRole('ADMIN')` và `hasAuthority('ADMIN')`: `@WithMockUser(authorities="ADMIN")` qua cái sau, **không** qua cái đầu; `@WithMockUser(roles="ADMIN")` (→ `ROLE_ADMIN`) ngược lại.
- `[M]` `AuthorizationManagerClaimsTest` (L1406, 1449) — `AccessDecisionManager`, `AccessDecisionVoter`, `FilterSecurityInterceptor` **không** còn trên classpath; `AuthorizationManager` có `authorize(Supplier, T)`; ghi lại nếu `check(...)` vẫn còn (LT.md khẳng định đã bị xoá).
- `[S]` `SelfInvocationTest` (Bẫy 1) — bean test-only gọi `this.deletePost(id)` để Bob xoá được bài của Alice (bypass); sau khi tách sang bean khác thì bị chặn.
- `[S]` `PostAuthorizeOnWriteTest` (Bẫy 3) — phương thức ghi gắn `@PostAuthorize("false")`: ném `AccessDeniedException` **nhưng dữ liệu đã bị xoá**.
- `[M]` `ResponseHasNoSensitiveDataTest` — JSON bài đăng không chứa `password` hay `$2a$`.

**Hoàn thành khi:** test xanh; DEVIATIONS có D6, D7, D14 và kết quả H10.

**Commit:** `feat(ch5): method security and BOLA/IDOR defense`

---

### Phase 6 — Chương 6: Session, CSRF, CORS, Headers, HTTPS

**Đọc:** `sed -n '1689,1936p'` (kiến trúc 1720–1775, code 1779–1900, bẫy 1906–1926, câu hỏi 1930–1934).

**Làm**

- Hoàn thiện chain 3 theo B2: CSRF cookie + Xor handler, CORS từ property, headers (`frameOptions deny`, CSP đã có), `sessionManagement` (fixation `changeSessionId`, `maximumSessions(2)`, `maxSessionsPreventsLogin(false)`, **cùng** `SessionRegistry` như B6), `HttpSessionEventPublisher`, `redirectToHttps` điều kiện hoá (D8).
- Phiên hết hạn do bị đá → trả 401 JSON (không redirect).
- `CsrfController` (`GET /api/v1/csrf/token`).
- Nếu giữ `ignoringRequestMatchers("/api/v1/webhooks/**")` (L1825) thì phải có endpoint webhook thật xác thực bằng secret header; nếu không thì **bỏ** (cấu hình chết là mùi code).

**Test**

- `[M]` `CsrfProtectionTest` — `POST /api/v1/posts` có session nhưng **không** token → 403; có token lấy từ `/api/v1/csrf/token` → 201; hai lần gọi `/csrf/token` cho hai giá trị **khác nhau** (XOR masking, L1755) nhưng đều hợp lệ. Kết quả H3.
- `[M]` `CsrfAttackSimulationTest` — mô phỏng trang độc: cookie session hợp lệ + `Origin: https://evil-giveaway.com` + không token → 403.
- `[M]` `SessionFixationTest` (H2) — tạo session ẩn danh S1 → đăng nhập bằng S1 → ID mới S2 ≠ S1; dùng cookie S1 → 401.
- `[M]` `ConcurrentSessionTest` — đăng nhập 3 lần (3 phiên khác nhau) với tối đa 2: phiên đầu mất hiệu lực (401), phiên 2 và 3 còn dùng được.
- `[M]` `CorsPreflightTest` — `OPTIONS` từ origin hợp lệ (`http://localhost:3000`, method `DELETE`) → thành công, **không** cần đăng nhập, `Access-Control-Allow-Origin` là origin cụ thể (không phải `*`), `Allow-Credentials: true`; origin lạ → bị từ chối (Bẫy 2, 3).
- `[M]` `ChainCsrfPolicyTest` (Bẫy 1) — chain 3 có `CsrfFilter`; chain 2 không có và không tạo session/cookie.
- `[S]` `SecurityHeadersTest` — `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, CSP, `X-XSS-Protection` ở giá trị mặc định của Security 7.
- `[S]` `HttpsRedirectTest` — `require-https=true`: request `http://` → 302 tới `https://…`; mặc định `false` → không chuyển hướng.

**Hoàn thành khi:** test xanh; DEVIATIONS có D8, D9 và kết quả H2, H3.

**Commit:** `feat(ch6): csrf, session management, cors, headers, https`

---

### Phase 7 — Chương 7: Stateless JWT Resource Server

**Đọc:** `sed -n '1938,2294p'` (kiến trúc 1962–2064, code 2068–2260, bẫy 2266–2285, câu hỏi 2289–2292).

**Làm**

- `jwt.auth`: `JwtEncoder` + private key, `JwtTokenService` (access token 15 phút, claim `sub`, `iss`, `iat`, `exp`, `jti`, `userId`, `roles` — **không** thêm email/phone, Bẫy 3). `jwt.resource`: `JwtDecoder` chỉ có public key, `JwtValidators.createDefaultWithValidators(issuer, JtiBlacklistValidator)` (D15).
- `RefreshTokenService`: chuỗi ngẫu nhiên 256 bit, **chỉ lưu SHA-256** trong bảng `refresh_tokens` (userId, tokenHash, expiresAt, revoked); xoay vòng (rotation); dùng lại token cũ → thu hồi toàn bộ.
- `JtiBlacklist` (interface) + `InMemoryJtiBlacklist` (TTL = thời gian còn lại của token, inject `Clock`).
- `AuthTokenController`: `POST /api/v1/auth/token`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout-all` (xoá mọi refresh token **và** đưa `jti` hiện tại vào blacklist).
- `JwtAuthenticationConverter` đọc claim `roles`, `authorityPrefix=""` (L2198–2208). Thêm chain 2 hoàn chỉnh (B2/B6) và `GET /api/v1/users/me` đọc **chỉ từ claim**.
- Cập nhật `FilterChainIntrospectionTest` thành 3 chain.

**Test**

- `[M]` `JwtStructureTest` — token có 3 phần; header `alg=RS256`, `typ=JWT`; payload giải Base64URL ra **đúng** tập claim `{sub, iss, iat, exp, jti, userId, roles}`, không có email/password; `exp - iat = 15 phút`.
- `[M]` `JwtVerificationTest` (H6) — sửa payload giữ nguyên chữ ký → 401; `alg=none` → 401; ký bằng khoá RSA khác → 401; hết hạn quá `clockSkew` → 401; sai `iss` → 401; `typ` khác `JWT` → ghi kết quả. Dựng token giả trực tiếp bằng Nimbus.
- `[M]` `StatelessChainTest` (Bẫy 1) — request dùng Bearer **không** sinh `Set-Cookie: JSESSIONID`, `request.getSession(false)` là `null`; `POST /api/v1/posts` với Bearer hợp lệ thành công **không cần** CSRF; Bearer sai → 401.
- `[M]` `ClaimMappingTest` (Bẫy 2) — `roles:["ROLE_MODERATOR"]` → `/api/v1/moderation/reports` 200; `roles:["ROLE_USER"]` → 403; thêm một test đơn vị cho thấy `JwtGrantedAuthoritiesConverter` mặc định chỉ sinh `SCOPE_*` và bỏ qua `roles`.
- `[M]` `RefreshFlowTest` — `token` → (access, refresh); `refresh` → cặp mới, refresh cũ bị từ chối; dùng lại refresh cũ → toàn bộ refresh của user bị thu hồi; refresh hết hạn → 401; dùng refresh token làm Bearer → 401.
- `[M]` `LogoutAllTest` — sau `logout-all`: refresh token bị xoá và **access token hiện tại bị từ chối ngay** (blacklist `jti`); token của user khác không bị ảnh hưởng; mục blacklist hết hạn sau TTL (đẩy `Clock`).
- `[M]` `StatelessVerificationNoDbTest` (L1958) — `@MockitoSpyBean UserRepository`: gọi `GET /api/v1/users/me` bằng JWT → `verifyNoInteractions(userRepository)`.
- `[M]` `ResourceServerKeyTest` (Q1) — `NimbusJwtDecoder.withPublicKey(pub)` xác minh được token; quét package `jwt.resource` không tham chiếu `RSAPrivateKey`/`JwtEncoder`.
- `[M]` `FilterChainIntrospectionTest` — cập nhật: đúng 3 chain.

**Hoàn thành khi:** test xanh; DEVIATIONS có D15, D16 và kết quả H6.

**Commit:** `feat(ch7): stateless jwt resource server with refresh and revocation`

---

### Phase 8 — Chương 8: OAuth 2.1, MFA, One-Time Token và kiểm thử bảo mật

**Đọc:** `sed -n '2298,2631p'` (kiến trúc 2325–2385, code 2389–2590, bẫy 2596–2624, câu hỏi 2628–2631).

**Làm**

- Chain 3: `oauth2Login` (github, giá trị dummy từ env, `redirect-uri` khớp tuyệt đối), `oneTimeTokenLogin` (`/ott/generate`, handler gọi `MagicLinkMailer` — D13, **không** `System.out`), `@EnableMultiFactorAuthentication(authorities = {})` + `AuthorizationManagerFactory` yêu cầu `FACTOR_PASSWORD` + `FACTOR_OTT` **chỉ** cho `DELETE /api/v1/users/me` (D19); entry point 401 JSON cho `/api/**` (D18).
- `UserManagementController.deleteMyAccount` xoá thật (user, bài đăng, refresh token); giữ `@PreAuthorize("hasAuthority('FACTOR_OTT')")` làm lớp phòng thủ thứ hai (L2493).
- Viết lại `PostControllerSecurityTest` (L2506–2590) dựa trên stack thật (D4); **giữ nguyên 5 `@DisplayName`** của LT.md.

**Test**

- `[M]` `OAuth2LoginPkceTest` (H7) — `GET /oauth2/authorization/github` → 302 tới `github.com`, có `code_challenge`, `code_challenge_method=S256`, `state`, `redirect_uri` khớp tuyệt đối với cấu hình, **không** có `response_type=token`. Dùng `oauth2Login()` post-processor để kiểm tra principal OAuth2 vào được `/api/v1/users/me`. Ghi chú trong DEMO-SCRIPT: ứng dụng không cung cấp Password Grant.
- `[M]` `OttLoginFlowTest` (L2372–2375, Q2) — `POST /ott/generate` (username, CSRF) → token xuất hiện trong hộp thư giả; `GET /login/ott?token=…` chỉ hiện form và **chưa** tiêu thụ token; `POST /login/ott` → đăng nhập, có authority `FACTOR_OTT`; dùng lại token → thất bại (replay); token hết hạn → thất bại; username lạ cho phản hồi **giống** username thật.
- `[M]` `MfaDeleteAccountTest` (H8) — chỉ mật khẩu → `DELETE /api/v1/users/me` **403**; chỉ OTT → 403; mật khẩu **và** OTT trong cùng phiên → 200, user + bài + refresh token biến mất, authorities chứa cả `FACTOR_PASSWORD` và `FACTOR_OTT`.
- `[M]` `PostControllerSecurityTest` — 5 kịch bản của LT.md (feed công khai 200; xoá khi chưa đăng nhập 401; Bob xoá bài Alice 403; moderator xoá 200; JWT admin 200 — H12), nay chạy trên stack thật.
- `[M]` `WebMvcSliceVsFullStackTest` (Q1, H11) — một test slice `@WebMvcTest` + `@MockitoBean PostService` **vẫn xanh dù service không có bảo mật** (làm rõ mock che mất AOP) và một test `@SpringBootTest(webEnvironment = RANDOM_PORT)` gọi bằng `java.net.http.HttpClient` đi qua toàn bộ chuỗi Servlet.
- `[S]` `TestPrincipalTypesTest` (Bẫy 2) — `@WithUserDetails("cuong_admin", userDetailsServiceBeanName="instaCloneUserDetailsService")` cho principal kiểu `InstaCloneUserDetails` (có `id`), `@WithMockUser` thì không.
- `[S]` `CsrfInTestsTest` (Bẫy 1) — `DELETE` thiếu `.with(csrf())` → 403 dù đủ quyền; có `.with(csrf())` → qua.

**Hoàn thành khi:** test xanh; DEVIATIONS có D2, D3, D4, D13, D18, D19 và kết quả H7, H8, H9, H11, H12.

**Commit:** `feat(ch8): oauth2 login, one-time token, mfa and security test suite`

---

### Phase 9 — Đóng gói demo

**Đọc:** không cần LT.md, trừ các câu hỏi (L270, 596, 925, 1353, 1681, 1932, 2291, 2630).

**Làm**

- `README.md`: cách chạy (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`), user mẫu (B4), bản đồ URL (B3), sơ đồ 3 chain.
- `docs/DEMO-SCRIPT.md`: kịch bản theo chương; mỗi bước có lệnh `curl`, kết quả mong đợi và liên kết tới mục LT.md. Tối thiểu: (1) 401 vs 403; (2) Bob xoá bài Alice → 403, bài PRIVATE → 404; (3) CSRF thiếu token → 403; (4) lấy JWT → gọi API → `logout-all` → token cũ bị từ chối; (5) OTT hai bước; (6) so sánh hash của hai user cùng mật khẩu.
- `scripts/smoke.sh`: khởi động app profile `dev`, chạy khoảng 12 lệnh `curl`, **assert** mã trạng thái, thoát khác 0 nếu sai.
- `docs/QA.md`: trả lời 16 câu hỏi tư duy (mỗi câu ≤ 10 dòng, dẫn chiếu file + test cụ thể). Nêu D20.
- Hoàn thiện `docs/TRACEABILITY.md` (mọi hàng ✅ hoặc ghi lý do) và `docs/DEVIATIONS.md`.

**Hoàn thành khi:** `./mvnw clean verify` xanh; `scripts/smoke.sh` thoát 0; checklist Phần G đạt.

**Commit:** `docs: demo script, smoke test, QA and final traceability`

---

### Stretch (chỉ làm khi mọi `[M]` đã xong)

- **ST-1** `Jackson3SessionSerializationTest` (L1059–1061): tuần tự hoá/giải tuần tự `SecurityContext` bằng module bảo mật của Jackson 3 (`SecurityJacksonModules`, kiểm tên bằng javap); principal tuỳ biến `InstaCloneUserDetails` cần mixin/allowlist — chứng minh lớp lạ bị từ chối (chống RCE giải tuần tự).
- **ST-2** `RedisJtiBlacklist` sau profile `redis` + `docker-compose.yml`; cùng bộ test `LogoutAllTest`.
- **ST-3** Nạp khoá RSA từ file PEM (`instaclone.jwt.key-location`); test: restart không làm mất hiệu lực token.
- **ST-4** Spring Authorization Server (đã nằm trong Spring Security 7) để chứng minh Authorization Code + PKCE end-to-end thay cho OAuth provider bên ngoài.

---

## PHẦN F — MA TRẬN TRUY VẾT (LT.md → code → test)

Chép bảng này vào `docs/TRACEABILITY.md` ở Phase 0; mỗi phase cập nhật cột trạng thái (⬜ → ✅). Cột "Hiện thực" Gemini điền đường dẫn file thật.

| Chương / mục                | Khái niệm trong LT.md                                                                           | Test chứng minh                                                                          | TT  |
| --------------------------- | ----------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- | --- |
| 1 §2.1 (L34–47)             | Identification, Credentials, Authentication, Principal, Authority, Trust Boundary               | `LoginFlowTest` + mục thuật ngữ trong `QA.md`                                            | ⬜  |
| 1 §2.2 (L49–90)             | Servlet Filter đứng trước DispatcherServlet; Interceptor không bảo vệ được tài nguyên ngoài MVC | `AntiPatternInterceptorTest`, `StaticResourceProtectedTest`, `FirewallVsInterceptorTest` | ⬜  |
| 1 §2.3 (L92–98)             | Least privilege, deny-by-default, `POST_DELETE_ANY`                                             | `DenyByDefaultTest`, `BolaDeleteTest`                                                    | ⬜  |
| 1 Bẫy 1 (L255)              | 401 ≠ 403                                                                                       | `StatusSemanticsTest`                                                                    | ⬜  |
| 1 Bẫy 2 (L260)              | Không tin Frontend → BOLA                                                                       | `BolaDeleteTest`                                                                         | ⬜  |
| 1 Q1, Q2 (L269–270)         | 401 vs 403; filter đặt sau DispatcherServlet                                                    | `StatusSemanticsTest`; `FilterChainIntrospectionTest` + `QA.md`                          | ⬜  |
| 2 §2.1 (L306–365)           | DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain, khớp chain đầu tiên             | `FilterChainIntrospectionTest`                                                           | ⬜  |
| 2 §2.2 (L369–381)           | PathPatternRequestMatcher; Lambda DSL, không `.and()`; `Customizer<HttpSecurity>`               | `RemovedApisClaimsTest`, `ModularCustomizerTest`                                         | ⬜  |
| 2 §3.3 (L484–558)           | Custom Filter kế thừa `OncePerRequestFilter` (rate limit)                                       | `RateLimitFilterTest`                                                                    | ⬜  |
| 2 Bẫy 1 (L566)              | Filter chạy đúp                                                                                 | `FilterRunsOnceTest`                                                                     | ⬜  |
| 2 Bẫy 2 (L586)              | Thứ tự rule: cụ thể trước, tổng quát sau                                                        | `AuthorizationRuleOrderTest`                                                             | ⬜  |
| 2 Q1, Q2 (L596–597)         | `permitAll` vẫn đi qua filter; nhiều chain + `@Order`                                           | `PermitAllStillTraversesFilterChainTest`; `FilterChainIntrospectionTest`                 | ⬜  |
| 3 §2.1 (L627–636)           | Salt ngẫu nhiên, slow hashing, work factor                                                      | `PasswordEncodingTest`, `CostFactorBenchmarkTest`                                        | ⬜  |
| 3 §2.2 (L638–686)           | `DelegatingPasswordEncoder`, tiền tố `{id}`, `upgradeEncoding`                                  | `EncodingUpgradeTest`                                                                    | ⬜  |
| 3 §2.3 (L688–726)           | Cấu trúc 60 ký tự của BCrypt                                                                    | `PasswordEncodingTest`                                                                   | ⬜  |
| 3 §3 (L730–897)             | Đăng ký + mã hoá một lần ở Service                                                              | `RegisterApiTest`                                                                        | ⬜  |
| 3 Bẫy 1, 2, 3 (L903–919)    | Thiếu prefix; double encoding; cost quá cao                                                     | `MissingPrefixTest`; `DoubleEncodingTest`; `CostFactorBenchmarkTest`                     | ⬜  |
| 3 Q1, Q2 (L925–926)         | Re-encode khi cập nhật hồ sơ; timing attack                                                     | `DoubleEncodingTest`; `QA.md`                                                            | ⬜  |
| 4 §2.1–2.2 (L961–1042)      | Authentication, ProviderManager, DaoAuthenticationProvider, UDS, PasswordEncoder                | `LoginFlowTest`, `CredentialsErasedTest` (+ D20 trong `QA.md`)                           | ⬜  |
| 4 §2.3 (L1044–1057)         | `ThreadLocal`, `SecurityContextHolderFilter`, Explicit Save, dọn context                        | `SecurityContextIsolationTest`, `ForgetfulLoginTest`                                     | ⬜  |
| 4 §2.3 (L1059–1061)         | Tuần tự hoá phiên bằng Jackson 3                                                                | ST-1 `Jackson3SessionSerializationTest` (stretch)                                        | ⬜  |
| 4 Bẫy 1, 2, 3 (L1325–1347)  | Quên Explicit Save; Username Enumeration; mất context khi `@Async`                              | `ForgetfulLoginTest`; `UsernameEnumerationTest`; `AsyncContextPropagationTest`           | ⬜  |
| 4 Q1, Q2 (L1353–1355)       | UDS chỉ nhận username; credentials bị xoá                                                       | `QA.md`; `CredentialsErasedTest`                                                         | ⬜  |
| 5 §2.1 (L1395–1449)         | `AuthorizationManager` thay `AccessDecisionManager`                                             | `AuthorizationManagerClaimsTest`                                                         | ⬜  |
| 5 §2.2 (L1451–1457)         | `hasRole` (tự thêm `ROLE_`) vs `hasAuthority`                                                   | `HasRoleVsHasAuthorityTest`                                                              | ⬜  |
| 5 §2.3, §3 (L1459–1651)     | Method security qua AOP; chống BOLA bằng `@PreAuthorize`                                        | `BolaDeleteTest`, `PostVisibilityTest`, `ResponseHasNoSensitiveDataTest`                 | ⬜  |
| 5 Bẫy 1, 2, 3 (L1659–1675)  | Self-invocation; 403 vs 404; `@PostAuthorize` trên thao tác ghi                                 | `SelfInvocationTest`; `PostVisibilityTest`; `PostAuthorizeOnWriteTest`                   | ⬜  |
| 5 Q1, Q2 (L1681–1683)       | Phương thức private + AOP; chi phí SpEL                                                         | `QA.md`                                                                                  | ⬜  |
| 6 §2.1 (L1722–1755)         | Synchronizer Token; XOR masking chống BREACH                                                    | `CsrfProtectionTest`, `CsrfAttackSimulationTest`                                         | ⬜  |
| 6 §2.2 (L1757–1763)         | Session fixation; giới hạn phiên đồng thời                                                      | `SessionFixationTest`, `ConcurrentSessionTest`                                           | ⬜  |
| 6 §2.3 (L1765–1771)         | CORS, preflight, thứ tự `CorsFilter`                                                            | `CorsPreflightTest`                                                                      | ⬜  |
| 6 §2.4 (L1773–1775)         | `redirectToHttps` thay `requiresChannel`                                                        | `HttpsRedirectTest`                                                                      | ⬜  |
| 6 §3.1 (L1837–1841)         | Security headers                                                                                | `SecurityHeadersTest`                                                                    | ⬜  |
| 6 Bẫy 1, 2, 3 (L1908–1926)  | Tắt CSRF bừa bãi; `*` + credentials; preflight bị 403                                           | `ChainCsrfPolicyTest`; `CorsPreflightTest`                                               | ⬜  |
| 6 Q1, Q2 (L1932–1934)       | CORS ≠ CSRF; JWT header vs JWT cookie                                                           | `CsrfAttackSimulationTest`; `StatelessChainTest` + `QA.md`                               | ⬜  |
| 7 §2.1 (L1964–2009)         | Cấu trúc JWT, chữ ký bất đối xứng, payload không bí mật                                         | `JwtStructureTest`, `JwtVerificationTest`, `ResourceServerKeyTest`                       | ⬜  |
| 7 §2.2 (L2011–2052)         | `BearerTokenAuthenticationFilter`, `JwtDecoder`, `JwtAuthenticationConverter`, kiểm `typ`       | `JwtVerificationTest`, `ClaimMappingTest`                                                | ⬜  |
| 7 §2.3 (L2054–2064)         | Access/Refresh token; thu hồi bằng blacklist `jti` + TTL                                        | `RefreshFlowTest`, `LogoutAllTest`                                                       | ⬜  |
| 7 Câu hỏi trọng tâm (L1958) | Xác thực stateless không cần DB                                                                 | `StatelessVerificationNoDbTest`                                                          | ⬜  |
| 7 Bẫy 1, 2, 3 (L2268–2285)  | Quên `STATELESS`; claim `roles` vs `scope`; lộ dữ liệu trong payload                            | `StatelessChainTest`; `ClaimMappingTest`; `JwtStructureTest`                             | ⬜  |
| 7 Q1, Q2 (L2291–2292)       | Chỉ giữ public key; offline vs introspection/blacklist                                          | `ResourceServerKeyTest`; `LogoutAllTest` + `QA.md`                                       | ⬜  |
| 8 §2.1 (L2327–2355)         | OAuth 2.1: PKCE bắt buộc, không Password Grant                                                  | `OAuth2LoginPkceTest`                                                                    | ⬜  |
| 8 §2.2 (L2357–2366)         | MFA: `FactorGrantedAuthority`, yêu cầu đủ yếu tố                                                | `MfaDeleteAccountTest`                                                                   | ⬜  |
| 8 §2.3 (L2368–2375)         | One-Time Token, chống replay                                                                    | `OttLoginFlowTest`                                                                       | ⬜  |
| 8 §2.4, §3.3 (L2377–2590)   | `@WithMockUser`, `@WithUserDetails`, `jwt()`, MockMvc                                           | `PostControllerSecurityTest`, `TestPrincipalTypesTest`                                   | ⬜  |
| 8 Bẫy 1, 2, 3 (L2598–2624)  | Thiếu CSRF trong test; sai kiểu principal; redirect URI lỏng                                    | `CsrfInTestsTest`; `TestPrincipalTypesTest`; `OAuth2LoginPkceTest`                       | ⬜  |
| 8 Q1, Q2 (L2630–2631)       | `@WebMvcTest` vs `@SpringBootTest`; Magic Link GET vs POST                                      | `WebMvcSliceVsFullStackTest`; `OttLoginFlowTest` + `QA.md`                               | ⬜  |

---

## PHẦN G — CHECKLIST NGHIỆM THU CUỐI

- [ ] `./mvnw clean verify` xanh; không có `@Disabled`; không test bị bỏ qua (trừ tag `benchmark`).
- [ ] Mọi hàng của Phần F đạt ✅ (hoặc có lý do trong `DEVIATIONS.md`).
- [ ] D1–D20 đều có dòng trong `DEVIATIONS.md` (áp dụng / không áp dụng + lý do); H1–H12 đều có kết quả.
- [ ] `grep -rEn 'WebSecurityConfigurerAdapter|AntPathRequestMatcher|MvcRequestMatcher|\.and\(\)' src/main` → rỗng.
- [ ] `FragileSecurityInterceptor` không được đăng ký ở đâu (`grep -rn addInterceptors src/main` → rỗng).
- [ ] `csrf(... disable())` chỉ xuất hiện trong chain 2.
- [ ] Không có `System.out`; không log token, mật khẩu, hash; không có client secret hay khoá thật trong repo.
- [ ] `README.md`, `docs/DEMO-SCRIPT.md`, `docs/QA.md`, `scripts/smoke.sh` tồn tại; `scripts/smoke.sh` thoát mã 0.
- [ ] `docs/PLAN.md` không bị sửa (mọi thay đổi ý định nằm trong `DEVIATIONS.md`).

---

## PHẦN H — GIỚI HẠN CỦA PLAN NÀY

- Plan được soạn từ commit `14e250d` của repo; số dòng LT.md có thể lệch nếu file đổi. Nếu lệch, lấy lại mốc bằng `grep -n '^#' docs/LT.md`.
- Người soạn plan **không build được** dự án (môi trường soạn không có Maven/JDK và mạng bị hạn chế), nên: phác thảo B6 chưa được biên dịch; tên starter ở B1 và các tên lớp/phương thức Security 7 phải được xác nhận bằng build và `javap`; mọi điều ở Phần D là giả thuyết, không phải sự thật.
- Đã đối chiếu với tài liệu chính thức Spring: `@EnableMultiFactorAuthentication(authorities = …)` cùng `FactorGrantedAuthority.PASSWORD_AUTHORITY / OTT_AUTHORITY` và `AuthorizationManagerFactories.multiFactor()`; `JwtTypeValidator` trong Security 7; việc đổi tên starter của Boot 4 (`-webmvc`, `-security-oauth2-*`).
- Phần LT.md nói rằng Spring Security 7 "bắt buộc" kiểm `typ` và PKCE cho mọi client: tài liệu chính thức cho thấy `typ` mặc định chấp nhận `JWT` **hoặc vắng mặt**. Hãy để test (H6, H7) quyết định, đừng giả định.
