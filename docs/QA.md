# GIẢI ĐÁP CÁC CÂU HỎI TƯ DUY & PHẢN BIỆN (QA)

Tài liệu này tổng hợp giải đáp cho 16 câu hỏi tư duy chuyên sâu trong `docs/LT.md`, hệ thống hóa các thuật ngữ bảo mật nền tảng và làm rõ sai lệch sơ đồ D20.

---

## 0. Hệ Thống Thuật Ngữ Nền Tảng (Chương 1 §2.1) & Ghi Chú D20

- **Identification (Định danh):** Quá trình client tuyên bố mình là ai bằng một định danh duy nhất (VD: `username` trong `LoginRequest`).
- **Credentials (Thông tin chứng thực):** Bằng chứng bí mật dùng để chứng minh danh xưng (VD: mật khẩu thô `Password123!`, JWT Bearer token).
- **Authentication (Xác thực):** Tiến trình thẩm định tính hợp lệ của Credentials, chuyển đổi từ trạng thái chưa tin cậy sang đối tượng `Authentication` đã xác thực (`AuthenticationManager.authenticate()` trong `AuthenticationController.java`).
- **Principal (Chủ thể):** Thực thể danh tính đại diện cho người dùng sau khi xác thực thành công (`InstaCloneUserDetails` chứa `id`, `username`, `email`).
- **Authority (Quyền hạn):** Quyền thao tác cụ thể được cấp cho Principal (VD: `ROLE_USER`, `ROLE_ADMIN`, `POST_DELETE_ANY`).
- **Trust Boundary (Ranh giới tin cậy):** Ranh giới phân chia giữa vùng mạng bên ngoài và ứng dụng nội bộ, được bảo vệ bởi chuỗi Servlet Filter trong `SecurityChainsConfig.java`.
- **Ghi chú D20 (Sơ đồ L1192 trong LT.md):** Sơ đồ tại dòng 1192 trong `LT.md` thể hiện `ProviderManager` gọi `SecurityContextHolder.setAuthentication(...)`. Thực tế, `ProviderManager` chỉ thực thi các `AuthenticationProvider` và trả về đối tượng `Authentication`. Việc thiết lập context vào `SecurityContextHolder` và lưu trữ vào repository thuộc trách nhiệm của Filter (`SecurityContextHolderFilter`, `AbstractAuthenticationProcessingFilter`) hoặc Controller trong luồng login thủ công (`AuthenticationController.java:75-85`, kiểm chứng trong `LoginFlowTest.java` và `ForgetfulLoginTest.java`).

---

## 1. Chương 1: Ranh Giới Tin Cậy & Ngữ Nghĩa 401/403

### Câu 1.1: Phân tích trạng thái bảo mật khi POST /posts trả 401 còn DELETE /posts/45 trả 403
- **Trả lời:** Mã 401 Unauthorized phản ánh trạng thái người dùng chưa xác thực (Unauthenticated) — hệ thống chưa biết người gọi là ai; người dùng cần đăng nhập. Ngược lại, mã 403 Forbidden phản ánh trạng thái người dùng đã xác thực thành công (Authenticated), nhưng Principal không có đủ quyền hạn (Unauthorized / Insufficient Privileges) trên tài nguyên yêu cầu (ví dụ: user thường cố xóa bài của người khác hoặc truy cập khu vực kiểm duyệt).
- **Dẫn chiếu file & test:** `SecurityChainsConfig.java` (chain 3), `PostSecurityEvaluator.java`; kiểm chứng thực nghiệm tại `StatusSemanticsTest.java` và `PostControllerSecurityTest.java` (kịch bản 2 và 3).

### Câu 1.2: Rủi ro khi đặt Custom Servlet Filter bảo mật sau DispatcherServlet
- **Trả lời:** Đặt Filter bảo mật sau DispatcherServlet phá vỡ hoàn toàn nguyên lý Defense-in-Depth: request độc hại đã vượt qua ranh giới ngoài, kích hoạt Controller, HandlerMapping, DataBinder và Interceptor trước khi bị kiểm tra. Ngoài ra, Filter đặt sau DispatcherServlet hoàn toàn không thể bảo vệ các tài nguyên tĩnh (`/uploads/**`), servlet khác hoặc endpoint quản trị framework. Spring Security luôn đứng đầu chuỗi lọc để bảo vệ tài nguyên ngay từ cửa ngõ.
- **Dẫn chiếu file & test:** `SecurityConfig.java`, `StaticResourceProtectedTest.java`, `AntiPatternInterceptorTest.java`, `FirewallVsInterceptorTest.java`.

---

## 2. Chương 2: Filter Chain & Cấu Hình Hiện Đại

### Câu 2.1: Request tới permitAll() có đi qua Filter Chain không? Filter ném RuntimeException thì sao?
- **Trả lời:** `permitAll()` KHÔNG hề bỏ qua chuỗi lọc; request vẫn đi qua tuần tự từng Servlet Filter trong `SecurityFilterChain`. `permitAll()` chỉ chỉ thị cho `AuthorizationFilter` ở cuối chuỗi cho phép request đi tiếp mà không đòi hỏi Authentication. Do đó, nếu một Filter đứng trước ném `RuntimeException`, luồng xử lý bị chặn đứng lập tức, Controller không bao giờ được gọi và client nhận lỗi server.
- **Dẫn chiếu file & test:** `SecurityChainsConfig.java:publicFeedFilterChain`; kiểm chứng thực nghiệm tại `PermitAllStillTraversesFilterChainTest.java` (sử dụng customizer test-only ném exception khi gặp header `X-Boom`).

### Câu 2.2: Cơ chế FilterChainProxy chọn chain và hậu quả khi securityMatcher trùng lặp
- **Trả lời:** `FilterChainProxy` duyệt qua danh sách `SecurityFilterChain` theo thứ tự ưu tiên `@Order` tăng dần và chọn chain đầu tiên có `securityMatcher.matches(request) == true` (First-Match-Wins); toàn bộ các chain phía sau bị bỏ qua. Nếu hai chain có matcher trùng lặp, chain có `@Order` nhỏ hơn sẽ nuốt trọn request, biến chain thứ hai thành mã chết (Dead Configuration) không bao giờ được kích hoạt.
- **Dẫn chiếu file & test:** `SecurityChainsConfig.java` (Order 1, 2, 3); kiểm chứng thực nghiệm tại `FilterChainIntrospectionTest.java` và `AuthorizationRuleOrderTest.java`.

---

## 3. Chương 3: Quản Lý Danh Tính & Mã Hóa Mật Khẩu

### Câu 3.1: Hậu quả khi re-encode mật khẩu khi cập nhật thông tin cá nhân (Bio, Email)
- **Trả lời:** Khi cập nhật profile mà không đổi mật khẩu, nếu Service vô tình lấy mật khẩu hiện tại trong database (đã là chuỗi băm BCrypt) và gọi lại `passwordEncoder.encode(user.getPassword())`, chuỗi băm sẽ bị mã hóa hai lần (Double Encoding). Khi người dùng đăng nhập lại bằng mật khẩu gốc, phép so sánh `matches(rawPassword, doubleHash)` sẽ thất bại, khiến người dùng bị khóa tài khoản vĩnh viễn.
- **Dẫn chiếu file & test:** `UserService.java:updateProfile`; kiểm chứng thực nghiệm tại `DoubleEncodingTest.java` (chứng minh mật khẩu double-encode làm mất khả năng đăng nhập).

### Câu 3.2: Lỗ hổng Timing Attack của password.equals() và giải pháp MessageDigest.isEqual()
- **Trả lời:** `String.equals()` dừng so sánh ngay khi gặp ký tự sai lệch đầu tiên (Early Exit), tạo ra sự chênh lệch thời gian thực thi đo đạc được ở mức nano-giây (Side-Channel Timing Attack) để kẻ tấn công đoán từng ký tự. `MessageDigest.isEqual()` giải quyết bằng thuật toán thời gian cố định (Constant-Time): luôn duyệt qua toàn bộ mảng byte và tích lũy sai khác bằng toán tử XOR (`result |= a[i] ^ b[i]`), triệt tiêu hoàn toàn rò rỉ thời gian.
- **Dẫn chiếu file & test:** `PasswordEncoderConfig.java`; kiểm chứng thực nghiệm tại `PasswordEncodingTest.java`.

---

## 4. Chương 4: Luồng Xác Thực & SecurityContext

### Câu 4.1: Tại sao UserDetailsService.loadUserByUsername chỉ nhận username mà không nhận password?
- **Trả lời:** Theo nguyên lý phân tách trách nhiệm (Separation of Concerns), `UserDetailsService` chỉ là hợp đồng truy xuất dữ liệu người dùng (Data Access Contract) từ kho lưu trữ. Việc kiểm tra và so sánh mật khẩu thuộc trách nhiệm của `AuthenticationProvider` (cụ thể là `DaoAuthenticationProvider`) phối hợp với `PasswordEncoder`. Thiết kế này cho phép thay đổi thuật toán mã hóa hoặc tích hợp MFA/LDAP mà không ảnh hưởng tới tầng truy xuất dữ liệu.
- **Dẫn chiếu file & test:** `InstaCloneUserDetailsService.java`, `AuthenticationController.java`; kiểm chứng thực nghiệm tại `LoginFlowTest.java`.

### Câu 4.2: Cơ chế xóa credentials sau đăng nhập và rủi ro khi giữ lại trong bộ nhớ
- **Trả lời:** Sau khi xác thực thành công, `ProviderManager` tự động gọi `CredentialsContainer.eraseCredentials()` để xóa sạch mật khẩu gốc (gán `null`) khỏi đối tượng `Authentication`. Nếu giữ lại mật khẩu thô trong memory, nguy cơ rò rỉ rất cao thông qua Heap Dump, Error Logs, giám sát APM, hoặc khi tuần tự hóa `SecurityContext` lưu vào Session/Redis.
- **Dẫn chiếu file & test:** `AuthenticationManagerConfig.java`; kiểm chứng thực nghiệm tại `CredentialsErasedTest.java` (chứng minh `authentication.getCredentials() == null`).

---

## 5. Chương 5: Phân Quyền Đa Tầng & Chống BOLA/IDOR

### Câu 5.1: Tại sao @PreAuthorize trên phương thức private vô hiệu? Giải pháp AspectJ
- **Trả lời:** Spring AOP mặc định sử dụng cơ chế Dynamic Proxy (CGLIB hoặc JDK dynamic proxy) bọc bên ngoài Spring Bean để chặn lời gọi phương thức từ bên ngoài. Proxy chỉ có thể override các phương thức `public`; phương thức `private`/`protected` không thể bị override nên proxy bị bỏ qua hoàn toàn. Để bảo vệ phương thức `private`, phải sử dụng AspectJ Bytecode Weaving (chèn mã kiểm tra phân quyền trực tiếp vào file class lúc biên dịch hoặc load-time).
- **Dẫn chiếu file & test:** `SelfInvocationDemo.java`; kiểm chứng thực nghiệm tại `SelfInvocationTest.java` và `WebMvcSliceMockBypassesAopTest.java`.

### Câu 5.2: Chi phí hiệu năng của SpEL khi tải 100,000 req/s và giải pháp tối ưu DB lookup
- **Trả lời:** SpEL đánh giá biểu thức động qua Reflection làm tốn CPU; việc truy vấn DB trong `PostSecurityEvaluator.isOwner()` ở mỗi request gây nghẽn kết nối database. Giải pháp tối ưu: (1) Bật SpEL Compiler (`SpelCompilerMode.IMMEDIATE`) để biên dịch biểu thức thành bytecode; (2) Sử dụng bộ nhớ đệm phân tán L2 Cache (Redis/Caffeine) cho quan hệ `postId -> ownerId`; (3) Nhúng trực tiếp quyền sở hữu vào token hoặc kiểm tra quyền sở hữu ở tầng truy vấn SQL (`WHERE id = :id AND author = :user`).
- **Dẫn chiếu file & test:** `PostSecurityEvaluator.java`, `PostService.java`; kiểm chứng thực nghiệm tại `BolaDeleteTest.java`.

---

## 6. Chương 6: Quản Lý Session & Phòng Thủ Lỗ Hổng Web

### Câu 6.1: CORS chặt chẽ có thay thế được CSRF không? Bản chất khác biệt
- **Trả lời:** Lập luận đó hoàn toàn SAI. CORS là chính sách bảo vệ phía trình duyệt ngăn không cho website độc hại ĐỌC (read) dữ liệu từ origin khác. CORS KHÔNG ngăn chặn trình duyệt GỬI (write) request thay đổi trạng thái kèm cookie session tự động. Kẻ tấn công có thể dùng thẻ `<form action="...">` trên trang độc để trigger request ghi mà không cần đọc kết quả. Do đó, `CsrfFilter` là bắt buộc để xác thực ý chí thực sự của người dùng.
- **Dẫn chiếu file & test:** `SecurityChainsConfig.java:webSessionFilterChain`; kiểm chứng thực nghiệm tại `CsrfAttackSimulationTest.java` và `CorsPreflightTest.java`.

### Câu 6.2: Tại sao JWT trong Authorization Header miễn nhiễm CSRF, còn trong Cookie thì không?
- **Trả lời:** CSRF khai thác cơ chế tự động gửi Cookie của trình duyệt (Ambient Authority). Khi JWT được đặt trong Header `Authorization: Bearer <token>`, trình duyệt không bao giờ tự động đính kèm header này; chỉ có mã script cùng nguồn (Same-Origin) của ứng dụng mới có thể can thiệp header. Ngược lại, nếu lưu JWT trong `HttpOnly Cookie`, trình duyệt lại tự động đính kèm cookie trong các request cross-site, khiến ứng dụng đối mặt với CSRF y như session truyền thống.
- **Dẫn chiếu file & test:** `SecurityChainsConfig.java:statelessApiFilterChain`; kiểm chứng thực nghiệm tại `StatelessChainTest.java` và `ChainCsrfPolicyTest.java`.

---

## 7. Chương 7: Stateless JWT Resource Server

### Câu 7.1: Tại sao service vệ tinh chỉ giữ RSA Public Key? Rủi ro khi lộ Private Key
- **Trả lời:** Trong mật mã bất đối xứng (Asymmetric Cryptography), Auth Service giữ Private Key để ký token; các Resource Server vệ tinh chỉ cần Public Key để kiểm tra chữ ký số (`NimbusJwtDecoder.withPublicKey(pub)`). Nếu lộ Private Key ở một service vệ tinh, kẻ tấn công có thể tự tạo và ký token giả mạo danh tính của bất kỳ người dùng nào (kể cả Super Admin), làm sụp đổ hoàn toàn ranh giới tin cậy của toàn bộ hệ sinh thái.
- **Dẫn chiếu file & test:** `infrastructure.security.jwt.auth` và `infrastructure.security.jwt.resource`; kiểm chứng thực nghiệm tại `ResourceServerKeyTest.java`.

### Câu 7.2: So sánh JWT validation offline vs online blacklist; giải pháp cho thao tác nhạy cảm
- **Trả lời:** Xác thực offline (chữ ký số) có ưu điểm tốc độ tức thì (0ms network latency), scale không giới hạn, nhưng không thể thu hồi token trước hạn (`exp`). Xác thực online (kiểm tra Redis blacklist hoặc Introspection) giải quyết được thu hồi nhưng phát sinh độ trễ mạng. Với InstaClone, giải pháp kết hợp tối ưu: kiểm tra chữ ký offline cho request thông thường, và tra cứu `InMemoryJtiBlacklist` / Redis cho các hành vi nhạy cảm (đổi mật khẩu, xóa tài khoản, `logout-all`).
- **Dẫn chiếu file & test:** `InMemoryJtiBlacklist.java`, `AuthTokenController.java`; kiểm chứng thực nghiệm tại `LogoutAllTest.java` và `JwtVerificationTest.java`.

---

## 8. Chương 8: OAuth 2.1, One-Time Token & Kiểm Thử

### Câu 8.1: Khác biệt giữa @WebMvcTest và @SpringBootTest(webEnvironment = RANDOM_PORT)
- **Trả lời:** `@WebMvcTest` là kiểm thử lát cắt (Slice Test), chỉ nạp Controller và MockMvc; bean nghiệp vụ phải mock bằng `@MockitoBean`. Mockito mock bypass hoàn toàn AOP Proxy của Spring Security, che giấu lỗi thiếu `@PreAuthorize`. Ngược lại, `@SpringBootTest(webEnvironment = RANDOM_PORT)` khởi chạy toàn bộ ApplicationContext, nhúng Tomcat thật và Servlet Filter Chain thật, kết nối HTTP Socket thật bằng `HttpClient`, phản ánh 100% môi trường runtime.
- **Dẫn chiếu file & test:** `WebMvcSliceMockBypassesAopTest.java`; kiểm chứng thực nghiệm tại `WebMvcSliceVsFullStackTest.java`.

### Câu 8.2: Rủi ro OTT qua GET Magic Link vs POST body, rò rỉ log, giải pháp Spring Security 7.x
- **Trả lời:** Token gửi qua URL query trong GET (`/login/ott?token=XYZ`) sẽ bị lưu nguyên văn trong Web Server Access Logs, Proxy Logs, Browser History và rò rỉ qua header `Referer`. Spring Security 7.x giải quyết bằng luồng 2 bước: Bước 1 (GET `/login/ott?token=...`) chỉ render form xác nhận và CHƯA tiêu thụ token; Bước 2 (POST `/login/ott`) gửi token trong body để tiêu thụ và vô hiệu hóa token ngay lập tức (chống replay).
- **Dẫn chiếu file & test:** `SecurityChainsConfig.java`, `MagicLinkMailer.java`; kiểm chứng thực nghiệm tại `OttLoginFlowTest.java`.
