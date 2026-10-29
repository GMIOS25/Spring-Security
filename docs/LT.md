# BỘ GIÁO TRÌNH LÝ THUYẾT & KIẾN TRÚC CHUYÊN SÂU: SPRING SECURITY 7.x

_(Nền tảng: Java 21+ | Spring Boot 4.x | Spring Framework 7.x | Spring Security 7.x)_

---

## CHƯƠNG 1: NỀN TẢNG TƯ DUY BẢO MẬT WEB & RANH GIỚI KIẾN TRÚC

### 1. ĐẶT VẤN ĐỀ & CÂU HỎI TRỌNG TÂM

#### Bài toán thực tế trên mạng xã hội InstaClone

Trong giai đoạn đầu phát triển nền tảng mạng xã hội InstaClone, đội ngũ kỹ thuật triển khai các tính năng cốt lõi: đăng bài viết (`POST /api/v1/posts`), xem bảng tin khám phá (`GET /api/v1/feed/public`), bình luận (`POST /api/v1/posts/{id}/comments`) và xóa bài đăng vi phạm (`DELETE /api/v1/posts/{id}`).

Để kiểm soát quyền truy cập trước khi đưa framework bảo mật vào, lập trình viên thường chọn giải pháp nhanh:

1. Viết mã kiểm tra thủ công bằng câu lệnh điều kiện `if/else` rải rác ngay bên trong các tầng `Service` hoặc `Controller`.
2. Sử dụng `HandlerInterceptor` của Spring MVC để chặn các request đến dựa trên đường dẫn URL.

**Thảm họa kiến trúc và an ninh xảy ra:**

- **Ô nhiễm mã nguồn (Code Pollution) & Vi phạm SRP:** Logic kiểm tra phiên làm việc và vai trò bị cài cắm khắp nơi. Khi nghiệp vụ thay đổi hoặc cần bổ sung kiểm toán (audit log), toàn bộ codebase bị ảnh hưởng.
- **Lọt ranh giới tin cậy (Trust Boundary Bypass):** `HandlerInterceptor` nằm hoàn toàn bên trong `DispatcherServlet`. Khi một lập trình viên cấu hình thêm một Servlet phục vụ file media tĩnh (`/uploads/**`) hoặc một endpoint kiểm tra sức khỏe của container/Actuator nằm ngoài tầm quét của Spring MVC, toàn bộ cơ chế bảo vệ bị vô hiệu hóa hoàn toàn. Kẻ tấn công có thể truy cập trực tiếp tài nguyên mà không cần thông qua interceptor.
- **Lỗ hổng nhầm lẫn phương thức HTTP:** Việc khớp chuỗi lỏng lẻo trong interceptor (ví dụ: chỉ kiểm tra `/api/v1/posts/*` mà quên ràng buộc HTTP Verb `DELETE`) cho phép người dùng thông thường gửi request sửa đổi/xóa bài viết của người khác.

#### Câu hỏi tư duy trọng tâm

> _Tại sao việc đặt logic bảo vệ người dùng tại Spring MVC HandlerInterceptor hoặc AOP Service lại không bao giờ được coi là "tường lửa vững chắc" (Perimeter Defense), và bản chất kiến trúc Servlet Filter giải quyết triệt để ranh giới tin cậy (Trust Boundary) như thế nào trước khi request chạm tới tầng ứng dụng?_

---

### 2. PHÂN TÍCH KIẾN TRÚC & CƠ CHẾ NỘI TẠI (UNDER THE HOOD)

#### 2.1. Phân định các khái niệm cốt lõi (Core Terminology)

Để thiết kế một hệ thống chuẩn mực, các ranh giới khái niệm phải được phân tách rõ ràng:

- **Identification (Định danh):** Quá trình Client tuyên bố mình là ai (ví dụ: gửi `username`, địa chỉ email hoặc `API-Key`).
- **Credentials (Chứng thư / Bằng chứng):** Bằng chứng chứng minh danh tính đó là có thật và chính chủ (mật khẩu, chữ ký số private key, mã OTP, sinh trắc học).
- **Authentication (Xác thực):** Hành động đối chiếu Credentials với cơ sở dữ liệu xác thực để khẳng định danh tính. Trả lời câu hỏi: _"Bạn có đúng là người bạn nhận hay không?"_.

- **Principal:** Đối tượng đại diện cho danh tính đã được xác thực thành công trong hệ thống (thường là một thực thể `UserDetails` chứa `userId`, `username`).
- **GrantedAuthority & Roles (Quyền hạn và Vai trò):** Quyền hạn chi tiết được cấp phát cho Principal (ví dụ: `POST_DELETE`, `COMMENT_READ`). Role là một tập hợp các quyền mang tính ngữ nghĩa nghiệp vụ cao hơn (ví dụ: `ROLE_MODERATOR`, `ROLE_ADMIN`).

- **Authorization (Phân quyền / Ủy quyền):** Quá trình quyết định xem Principal hiện tại có đủ đặc quyền để thực thi thao tác trên một tài nguyên cụ thể hay không. Trả lời câu hỏi: _"Bạn có được phép làm điều này không?"_.

- **Security Boundary & Trust Boundary:** Ranh giới ngăn cách giữa môi trường không tin cậy (mạng công cộng, Client phía trình duyệt/mobile) và vùng tin cậy nội bộ (Database, Business Services, File Storage). Mọi request phải được lọc sạch tại ranh giới trước khi vào vùng tin cậy.

#### 2.2. Ranh giới kiến trúc: Servlet Filter vs. Spring MVC HandlerInterceptor

Sự khác biệt mang tính sống còn giữa hai cơ chế nằm ở vị trí thực thi trong vòng đời của một HTTP Request trên Web Server:

```
[ HTTP Request từ Internet ]
               │
               ▼
   ┌────────────────────────────────────────────────────────┐
   │             Tomcat / Jetty Web Container               │
   │                                                        │
   │   ┌────────────────────────────────────────────────┐   │
   │   │  Servlet Filter Chain (Standard Servlet API)   │   │
   │   │                                                │   │
   │   │  [Filter 1] ──> [Spring Security Filters] ──── │───┼──> TRUST BOUNDARY
   │   │                                                │   │    (Ngăn chặn từ cửa ngõ)
   │   └───────────────────────┬────────────────────────┘   │
   │                           │                            │
   │                           ▼                            │
   │   ┌────────────────────────────────────────────────┐   │
   │   │        DispatcherServlet (Spring MVC)          │   │
   │   │                                                │   │
   │   │   ┌────────────────────────────────────────┐   │   │
   │   │   │       Spring MVC HandlerInterceptor    │   │   │
   │   │   │   (Chỉ áp dụng cho Controllers)       │   │   │
   │   │   └───────────────────┬────────────────────┘   │   │
   │   │                       ▼                        │   │
   │   │   ┌────────────────────────────────────────┐   │   │
   │   │   │       @RestController (InstaClone)     │   │   │
   │   │   └────────────────────────────────────────┘   │   │
   │   └────────────────────────────────────────────────┘   │
   └────────────────────────────────────────────────────────┘

```

- **Servlet Filter (Cơ chế của Spring Security):** Nằm ở tầng Servlet Container chuẩn (Jakarta EE). Bộ lọc này chặn request **trước** khi `DispatcherServlet` được kích hoạt. Nếu một request không hợp lệ hoặc có dấu hiệu tấn công, Filter có thể phản hồi mã lỗi `401 Unauthorized` hoặc `403 Forbidden` ngay lập tức, hoàn toàn không tốn tài nguyên khởi tạo context của Spring MVC, không gọi Reflection và ngăn ngừa tuyệt đối các cuộc tấn công khai thác vào tầng Controller.
- **Spring MVC HandlerInterceptor:** Nằm bên trong `DispatcherServlet`. Interceptor chỉ được kích hoạt khi request đã được `HandlerMapping` xác định là thuộc về một `@Controller` cụ thể. Do đó:
- Không thể bảo vệ các tài nguyên không thuộc `DispatcherServlet` (ví dụ: Direct Servlets, Static Resources được cấu hình độc lập, WebSocket handshake sơ khai, Filter-based endpoints).
- Xử lý ngoại lệ bảo mật trong Interceptor phức tạp hơn vì nó chịu sự chi phối của cơ chế xử lý lỗi của Spring MVC (`@ControllerAdvice`).

#### 2.3. Nguyên tắc đặc quyền tối thiểu (Least Privilege)

Trong InstaClone, một tài khoản người dùng bình thường (`ROLE_USER`) tuyệt đối không được cấp bất kỳ quyền hạn nào vượt quá phạm vi hoạt động của họ. Khi thiết kế hệ thống:

1. Mọi endpoint mặc định phải bị **chặn truy cập** (`deny-by-default` hoặc `authenticated`), chỉ mở công khai những endpoint thực sự cần thiết như xem feed chung (`/api/v1/feed/public/**`).

2. Quyền xóa bài viết của người khác (`POST_DELETE_ANY`) chỉ dành riêng cho `ROLE_MODERATOR` và `ROLE_ADMIN`.

---

### 3. MINH HỌA CODE CHUẨN MỰC TRÊN INSTACLONE

Dưới đây là mô hình phân ranh giới chuẩn mực cho InstaClone trên nền tảng Java 21, minh họa rõ sự tách biệt giữa DTO, Domain Model và việc định nghĩa quyền.

#### 3.1. Domain Entities & Role Definitions

```java
package com.instaclone.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roles = new HashSet<>();

    // Constructors, Getters, Setters chuẩn mực
    public User() {}

    public User(String username, String password, String email, Set<String> roles) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Set<String> getRoles() { return roles; }
}

```

```java
package com.instaclone.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String caption;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum PostVisibility {
        PUBLIC, FOLLOWERS_ONLY, PRIVATE
    }

    public Post() {}

    public Post(String caption, String imageUrl, User author, PostVisibility visibility) {
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.author = author;
        this.visibility = visibility;
    }

    public Long getId() { return id; }
    public String getCaption() { return caption; }
    public String getImageUrl() { return imageUrl; }
    public User getAuthor() { return author; }
    public PostVisibility getVisibility() { return visibility; }
}

```

#### 3.2. Chống mẫu chống thiết kế (Anti-Pattern Demo: HandlerInterceptor)

Dưới đây là ví dụ minh họa cách làm **sai lầm** mà một số lập trình viên cố gắng áp dụng và lý do nó thất bại trong việc thiết lập Trust Boundary vững chắc:

```java
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

```

---

### 4. BẪY LỖI KINH ĐIỂN & RỦI RO BẢO MẬT (GOTCHAS & PITFALLS)

- **Bẫy 1: Nhầm lẫn giữa Authentication (401) và Authorization (403)**
- _Hiện tượng:_ Trả về mã lỗi `403 Forbidden` khi người dùng chưa đăng nhập, hoặc trả về `401 Unauthorized` khi người dùng đã đăng nhập nhưng cố tình truy cập vào tài nguyên của Moderator.
- _Hệ quả:_ Làm sai lệch hành vi của Frontend (Client không biết khi nào cần điều hướng sang trang Login, khi nào cần báo lỗi quyền hạn) và vi phạm chuẩn thiết kế RESTful.
- _Khắc phục:_ Nhớ quy tắc bất biến: `401 Unauthorized` nghĩa là "Chưa xác thực danh tính (Unauthenticated) - Hãy đưa bằng chứng danh tính hợp lệ"; `403 Forbidden` nghĩa là "Đã biết danh tính nhưng không đủ thẩm quyền (Unauthorized Access) - Không được phép truy cập".

- **Bẫy 2: Tin tưởng tuyệt đối vào cơ chế lọc ở tầng Frontend**
- _Hiện tượng:_ Lập trình viên ẩn nút "Xóa bài viết" trên ứng dụng Web/Mobile đối với người dùng không phải chủ bài đăng hoặc Moderator, nhưng ở Backend Controller lại không kiểm tra quyền sở hữu bài viết khi nhận request `DELETE /api/v1/posts/102`.
- _Hệ quả:_ Gây ra lỗ hổng nghiêm trọng **BOLA (Broken Object Level Authorization)** / **IDOR**. Bất kỳ ai sử dụng Postman hoặc `curl` đều có thể xóa sạch bài viết trên hệ thống.
- _Khắc phục:_ Frontend chỉ quản lý trải nghiệm người dùng (UX), Backend bắt buộc phải tái thẩm định quyền ở mọi request.

---

### 5. BỘ CÂU HỎI TƯ DUY & PHẢN BIỆN (DEEP-DIVE QUESTIONS)

1. _Một request gửi tới endpoint `POST /api/v1/posts` bị từ chối với mã lỗi HTTP 401, trong khi một request khác gửi tới `DELETE /api/v1/posts/45` bị từ chối với mã lỗi HTTP 403. Hãy phân tích trạng thái bảo mật của hệ thống và trạng thái của người dùng trong hai tình huống trên._
2. _Nếu một lập trình viên cấu hình bảo mật bằng một Servlet Filter tự viết độc lập và đặt thứ tự ưu tiên (Order) chạy sau `DispatcherServlet`, kiến trúc này có gặp rủi ro gì không? Tại sao Spring Security luôn tìm cách đứng ở đầu chuỗi lọc?_

---

---

## CHƯƠNG 2: KIẾN TRÚC FILTER CHAIN & CẤU HÌNH HIỆN ĐẠI SPRING SECURITY 7.x

### 1. ĐẶT VẤN ĐỀ & CÂU HỎI TRỌNG TÂM

#### Bài toán thực tế trên mạng xã hội InstaClone

Khi InstaClone tích hợp Spring Security, hệ thống phải đáp ứng đồng thời nhiều yêu cầu:

1. Endpoint bảng tin công khai `GET /api/v1/feed/public/**` phải mở cho tất cả mọi người (kể cả khách vãng lai chưa đăng ký tài khoản).

2. Endpoint quản lý bài đăng `POST /api/v1/posts/**` và thông tin cá nhân `GET /api/v1/users/me` bắt buộc phải được xác thực danh tính.

3. Hệ thống phải sẵn sàng kiểm soát tần suất gửi request (Rate Limiting) để chống spam bình luận mà không làm ảnh hưởng đến hiệu năng của các API khác.

Nhiều lập trình viên khi nâng cấp từ các phiên bản cũ (Spring Security 5.x/6.x) lên **Spring Security 7.x** (chạy trên Spring Framework 7 và Spring Boot 4) thường gặp lỗi biên dịch nghiêm trọng do thói quen:

- Tìm cách kế thừa lớp `WebSecurityConfigurerAdapter` (đã bị xóa bỏ hoàn toàn).

- Sử dụng nối chuỗi `.and()` (đã bị xóa bỏ).

- Khai báo `AntPathRequestMatcher` hoặc `MvcRequestMatcher` (đã bị loại bỏ khỏi Spring Security 7.0 để thống nhất sang công nghệ khớp URL mới).

#### Câu hỏi tư duy trọng tâm

> _Làm cách nào mà một Spring Bean (nằm trong Spring ApplicationContext) có thể chặn và xử lý các HTTP Request được quản lý bởi Web Container (như Apache Tomcat) vốn nằm ngoài ranh giới của Spring IoC? Và tại sao kiến trúc cấu hình của Spring Security 7.x lại bắt buộc sử dụng Lambda DSL kết hợp PathPatternRequestMatcher?_

---

### 2. PHÂN TÍCH KIẾN TRÚC & CƠ CHẾ NỘI TẠI (UNDER THE HOOD)

#### 2.1. Cầu nối giữa Servlet Container và Spring IoC: DelegatingFilterProxy & FilterChainProxy

Web Container (như Tomcat) quản lý vòng đời của các Servlet Filter thông qua đặc tả Servlet API chuẩn. Tuy nhiên, các Filter bảo mật cần tương tác với Database, cấu hình và các Spring Bean khác. Để giải quyết mâu thuẫn này, Spring Security sử dụng mẫu thiết kế Proxy:

```
                                [ CLIENT / BROWSER ]
                                          │
                                          ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                Tomcat Web Container                                    │
│                                                                                        │
│   ┌────────────────────────────────────────────────────────────────────────────────┐   │
│   │                      Standard Servlet Filter Chain                             │   │
│   │                                                                                │   │
│   │   [StandardFilter] ──> [DelegatingFilterProxy] ──> [OtherServletFilter]        │   │
│   └───────────────────────────────────┬────────────────────────────────────────────┘   │
└───────────────────────────────────────┼────────────────────────────────────────────────┘
                                        │ (Ủy quyền xử lý request)
                                        ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              Spring ApplicationContext                                 │
│                                                                                        │
│   ┌────────────────────────────────────────────────────────────────────────────────┐   │
│   │  Bean "springSecurityFilterChain" (Thực thể của FilterChainProxy)              │   │
│   │                                                                                │   │
│   │   ┌────────────────────────────────────────────────────────────────────────┐   │   │
│   │   │ SecurityFilterChain 1: Matches /api/v1/feed/public/**                  │   │   │
│   │   │ (SecurityContextHolderFilter -> AnonymousAuthenticationFilter -> ...)  │   │   │
│   │   └────────────────────────────────────────────────────────────────────────┘   │   │
│   │   ┌────────────────────────────────────────────────────────────────────────┐   │   │
│   │   │ SecurityFilterChain 2: Matches /** (Mặc định cho toàn bộ ứng dụng)     │   │   │
│   │   │ (SecurityContextHolderFilter -> InstaRateLimitFilter ->                │   │   │
│   │   │  UsernamePasswordAuthenticationFilter -> AuthorizationFilter -> ...)   │   │   │
│   │   └────────────────────────────────────────────────────────────────────────┘   │   │
│   └────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────┘

```

1. **`DelegatingFilterProxy`:**

- Là một Servlet Filter tiêu chuẩn được đăng ký với Servlet Container.

- Nhiệm vụ duy nhất của nó là nhận `HttpServletRequest` từ Container, sau đó tìm kiếm một Spring Bean có tên chuẩn là `springSecurityFilterChain` trong Spring IoC Context và ủy quyền toàn bộ việc xử lý cho Bean này.

2. **`FilterChainProxy`:**

- Là một Spring Bean thực thụ, đóng vai trò là điểm điều phối trung tâm của toàn bộ hệ thống bảo mật.

- `FilterChainProxy` quản lý một danh sách gồm một hoặc nhiều `SecurityFilterChain`.

- Khi nhận request từ `DelegatingFilterProxy`, `FilterChainProxy` sẽ duyệt qua danh sách các `SecurityFilterChain` và chọn chuỗi lọc **đầu tiên** có cấu hình khớp với URL và HTTP Method của request đó.

3. **`SecurityFilterChain`:**

- Là một Interface chứa danh sách các Filter bảo mật cụ thể (`List<Filter>`) cùng một `RequestMatcher` để kiểm tra điều kiện kích hoạt.

- Các Filter bên trong được sắp xếp theo một thứ tự nghiêm ngặt (ví dụ: `SecurityContextHolderFilter` luôn chạy trước các Authentication Filters, và `AuthorizationFilter` luôn chạy ở cuối chuỗi).

#### 2.2. Sự đột phá kiến trúc trong Spring Security 7.x

##### Loại bỏ hoàn toàn AntPathRequestMatcher và MvcRequestMatcher để chuẩn hóa PathPatternRequestMatcher

- _Lịch sử:_ Ở các phiên bản trước, việc khớp đường dẫn URL thường xảy ra bất đồng bộ giữa cơ chế của Spring MVC (sử dụng `PathPatternParser` hiệu năng cao) và Spring Security (sử dụng `AntPathMatcher` cổ điển). Điều này từng dẫn đến các lỗ hổng bảo mật nghiêm trọng liên quan đến cách hiểu URL khác nhau (ví dụ: `/api/v1/posts` khác `/api/v1/posts/`).
- _Chuẩn Spring Security 7.x:_ Loại bỏ toàn bộ `AntPathRequestMatcher` và `MvcRequestMatcher`. Mọi thao tác so khớp URL đều được chuẩn hóa và thực thi thông qua **`PathPatternRequestMatcher`** dựa trên nền tảng `PathPatternParser` của Spring Framework 7, mang lại tốc độ biên dịch regex tối ưu và triệt tiêu hoàn toàn rủi ro sai lệch khớp mẫu URL.

##### Lambda DSL bắt buộc & Xóa bỏ chuỗi `.and()`

- Phương thức `.and()` tạo ra các chuỗi cấu hình dài, khó bảo trì, dễ gây nhầm lẫn scope giữa các nhánh con.
- Spring Security 7.x bắt buộc 100% sử dụng **Lambda DSL**. Mọi cấu hình thành phần (`authorizeHttpRequests`, `csrf`, `sessionManagement`) đều nhận một `Customizer<T>` functional interface, giúp cấu trúc mã nguồn thụt lề rõ ràng, độc lập và dễ đọc.

##### Cấu hình phân mô-đun với `Customizer<HttpSecurity>`

Spring Security 7.x khuyến khích kiến trúc cấu hình dạng mô-đun (Component-based Configuration). Thay vì nhồi nhét tất cả logic vào một phương thức `securityFilterChain` khổng lồ, lập trình viên có thể định nghĩa các Bean `Customizer<HttpSecurity>` chuyên biệt để tùy chỉnh từng phần mà không ghi đè cấu hình bảo mật mặc định của Spring Boot.

---

### 3. MINH HỌA CODE CHUẨN MỰC TRÊN INSTACLONE

Dưới đây là mã nguồn cấu hình hoàn chỉnh theo chuẩn Spring Security 7.x và cách viết Custom Filter chuẩn mực.

#### 3.1. Cấu hình `SecurityConfig` hiện đại với Lambda DSL

```java
package com.instaclone.infrastructure.security.config;

import com.instaclone.infrastructure.security.filter.InstaRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final InstaRateLimitFilter rateLimitFilter;

    // Dependency Injection chuẩn cho Custom Filter
    public SecurityConfig(InstaRateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * Cấu hình SecurityFilterChain chính cho toàn bộ API của InstaClone.
     * Sử dụng 100% cú pháp Lambda DSL và chuẩn khớp mẫu Spring Security 7.x.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Phân quyền ở mức Request Matchers
            .authorizeHttpRequests(authorize -> authorize
                // Công khai bảng tin chung và đăng ký/đăng nhập
                .requestMatchers(HttpMethod.GET, "/api/v1/feed/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                // Quyền quản lý kiểm duyệt dành cho Moderator và Admin
                .requestMatchers("/api/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")

                // Mọi request còn lại đều bắt buộc phải được xác thực
                .anyRequest().authenticated()
            )

            // 2. Kích hoạt HTTP Basic Authentication với thiết lập chuẩn mặc định
            .httpBasic(Customizer.withDefaults())

            // 3. Chèn Custom Rate Limit Filter vào trước Filter xác thực người dùng
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

```

#### 3.2. Cấu hình Mô-đun với `Customizer<HttpSecurity>`

Trong các hệ thống phân tán hoặc dự án lớn, ta có thể viết các mô-đun bảo mật riêng biệt (ví dụ: Audit Log, CORS policy) và xuất ra dưới dạng Bean:

```java
package com.instaclone.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class ModularSecurityConfig {

    /**
     * Tự động áp dụng các header bảo mật nâng cao mà không làm phình to hàm config chính.
     */
    @Bean
    public Customizer<HttpSecurity> securityHeadersCustomizer() {
        return http -> {
            try {
                http.headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; script-src 'self'")
                    )
                );
            } catch (Exception e) {
                throw new IllegalStateException("Failed to configure security headers", e);
            }
        };
    }
}

```

#### 3.3. Custom Filter kiểm soát tần suất: `InstaRateLimitFilter`

Khi tạo Custom Filter, khuyến khích kế thừa `OncePerRequestFilter` để đảm bảo mỗi request chỉ đi qua bộ lọc chính xác một lần (ngăn ngừa trường hợp đi qua nhiều lần khi có forward hoặc include trong Servlet Container).

```java
package com.instaclone.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter bảo vệ InstaClone trước các hành vi Spam bình luận hoặc Brute Force.
 * Kế thừa OncePerRequestFilter đảm bảo tính duy nhất khi thực thi.
 */
@Component
public class InstaRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InstaRateLimitFilter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    // Lưu trữ tạm bộ đếm request theo IP (Trong thực tế nên dùng Redis)
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Chỉ áp dụng giới hạn tần suất cho các API bình luận hoặc đăng bài
        if (path.startsWith("/api/v1/posts") && "POST".equalsIgnoreCase(request.getMethod())) {
            AtomicInteger counter = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));

            if (counter.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, path);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("""
                    {
                        "error": "Too Many Requests",
                        "message": "InstaClone rate limit exceeded. Please wait a moment before trying again."
                    }
                    """);
                return; // Ngắt chuỗi Filter, không cho request đi tiếp
            }
        }

        // Cho phép request tiếp tục đi qua các Filter tiếp theo trong SecurityFilterChain
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

```

---

### 4. BẪY LỖI KINH ĐIỂN & RỦI RO BẢO MẬT (GOTCHAS & PITFALLS)

- **Bẫy 1: Hiện tượng chạy đúp bộ lọc (Filter Duplicate Execution)**
- _Hiện tượng:_ Bạn đánh dấu `InstaRateLimitFilter` bằng `@Component` và đồng thời đăng ký nó trong cấu hình bằng `http.addFilterBefore(rateLimitFilter, ...)`. Khi chạy, bộ lọc bị kích hoạt 2 lần cho mỗi request!

- _Nguyên nhân:_ Spring Boot có cơ chế tự động phát hiện tất cả các Bean kế thừa `jakarta.servlet.Filter` và tự ý đăng ký chúng vào Servlet Filter Chain tổng của Web Container. Sau đó, Spring Security lại đưa filter đó vào `SecurityFilterChain` của riêng mình.

- _Cách debug & sửa triệt để:_

1. _Cách 1 (Khuyên dùng):_ Không đánh dấu `@Component` trên Filter mà khởi tạo trực tiếp thông qua constructor hoặc `@Bean` trong `SecurityConfig`.
2. _Cách 2:_ Nếu bắt buộc phải để `@Component` (để inject dependencies), hãy đăng ký một Bean `FilterRegistrationBean` với cấu hình `setEnabled(false)` để bảo Web Container không tự động nhúng filter đó vào Servlet Filter Chain ngoài:

```java
@Bean
public FilterRegistrationBean<InstaRateLimitFilter> disableAutomaticRegistration(InstaRateLimitFilter filter) {
    FilterRegistrationBean<InstaRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false); // Ngăn không cho Servlet Container đăng ký trực tiếp
    return registration;
}

```

- **Bẫy 2: Sắp xếp sai thứ tự các quy tắc trong `authorizeHttpRequests**`
- _Hiện tượng:_ Cấu hình `anyRequest().authenticated()` hoặc quy tắc chung `requestMatchers("/api/v1/**").permitAll()` đặt trước các quy tắc chi tiết bảo vệ admin.
- _Nguyên nhân:_ Cơ chế đối soát URL hoạt động theo thứ tự từ trên xuống dưới (First-match wins). Khi một request thỏa mãn quy tắc đầu tiên, các quy tắc bảo vệ nghiêm ngặt bên dưới sẽ bị bỏ qua hoàn toàn.

- _Cách sửa:_ Luôn tuân thủ nguyên tắc: **Cụ thể đặt ở trên (Specific First), Tổng quát đặt ở dưới (Broad/Generic Last)**.

---

### 5. BỘ CÂU HỎI TƯ DUY & PHẢN BIỆN (DEEP-DIVE QUESTIONS)

1. _Một request gửi tới endpoint `GET /api/v1/feed/public/explore` đã được khai báo `.permitAll()`. Request này có đi qua các Filter trong `SecurityFilterChain` hay không? Giả sử trong chuỗi lọc có một Custom Filter ném ra ngoại lệ `RuntimeException`, request đó có nhận được kết quả trả về từ Controller không? Giải thích bản chất của `permitAll()`._
2. _Trong một hệ thống Microservices hoặc hệ thống lớn phức tạp, nếu ta cấu hình nhiều Bean `SecurityFilterChain` với các `@Order` khác nhau, cơ chế nào bên trong `FilterChainProxy` quyết định chọn chain nào để thực thi? Điều gì xảy ra nếu hai `SecurityFilterChain` có điều kiện `securityMatcher` trùng lặp nhau?_

---

## CHƯƠNG 3: QUẢN LÝ DANH TÍNH & MÃ HÓA MẬT KHẨU AN TOÀN

### 1. ĐẶT VẤN ĐỀ & CÂU HỎI TRỌNG TÂM

#### Bài toán thực tế trên mạng xã hội InstaClone

Trong giai đoạn mở rộng người dùng cho InstaClone, hệ thống bắt đầu đón nhận hàng trăm nghìn tài khoản đăng ký mới. Để lưu trữ thông tin đăng nhập, lập trình viên non kinh nghiệm thường lựa chọn một trong hai phương án sai lầm:

1. Lưu mật khẩu dưới dạng văn bản thuần (Plaintext) trực tiếp vào bảng `users` trong cơ sở dữ liệu.

2. Băm mật khẩu bằng các thuật toán băm nhanh một chiều cổ điển như MD5, SHA-1 hoặc SHA-256 thuần túy không kèm muối (Salt).

**Thảm họa an ninh xảy ra:**

- **Rò rỉ dữ liệu (Data Breach):** Nếu cơ sở dữ liệu bị lộ lọt (thông qua SQL Injection hoặc rò rỉ bản sao lưu backup), mật khẩu dạng plaintext sẽ biến toàn bộ tài khoản người dùng thành "mồi ngon" ngay lập tức. Kẻ tấn công có thể sử dụng các mật khẩu này để chiếm đoạt tài khoản trên diện rộng thông qua kỹ thuật Credential Stuffing.

- **Tấn công Rainbow Table & Băm siêu tốc bằng GPU:** Đối với MD5 hoặc SHA-256 thuần túy, kẻ tấn công sử dụng các bảng tra cứu tính toán trước (Rainbow Tables) chứa hàng tỷ giá trị hash phổ biến để giải mã ngược mật khẩu chỉ trong vài mili-giây. Thậm chí với phần cứng GPU hiện đại, kẻ tấn công có thể thử hàng tỷ phép tính hash SHA-256 mỗi giây để bẻ khóa mật khẩu người dùng mà không gặp bất kỳ rào cản nào về mặt tính toán.

#### Câu hỏi tư duy trọng tâm

> _Tại sao một hàm băm mật mã học được coi là xuất sắc cho việc kiểm tra toàn vẹn tệp tin (như SHA-256) lại là một giải pháp tồi tệ khi dùng để lưu trữ mật khẩu người dùng? Bản chất kiến trúc của "Slow Hashing", Salt ngẫu nhiên và Work Factor giải quyết triệt để rủi ro này như thế nào trong Spring Security 7.x?_

---

### 2. PHÂN TÍCH KIẾN TRÚC & CƠ CHẾ NỘI TẠI (UNDER THE HOOD)

#### 2.1. Bản chất của Salt ngẫu nhiên và Thuật toán băm chậm (Slow Hashing)

Để vô hiệu hóa các cuộc tấn công Rainbow Table và Brute-force diện rộng, mật khẩu bắt buộc phải tuân theo hai nguyên lý cốt lõi:

1. **Salt ngẫu nhiên (Cryptographic Salt):** Là một chuỗi byte ngẫu nhiên được sinh ra cho mỗi lần mã hóa mật khẩu. Khi băm, chuỗi Salt này được ghép nối cùng với mật khẩu gốc (`hash(password + salt)`). Dù hai người dùng khác nhau trên InstaClone đặt cùng một mật khẩu `"Password123!"`, chuỗi hash lưu trong database của họ vẫn hoàn toàn khác biệt. Điều này làm cho các bảng tra cứu Rainbow Table tính toán sẵn trở nên vô dụng.

2. **Work Factor / Cost Factor (Độ phức tạp tính toán):** Các thuật toán chuyên dụng cho mật khẩu như BCrypt, PBKDF2 hay Argon2 được thiết kế có chủ đích để chạy **chậm** (Slow Hashing). Độ phức tạp tính toán được điều chỉnh thông qua thông số Cost Factor.

- Ví dụ: Với BCrypt cost factor = 10 ($2^{10} = 1024$ vòng lặp biến đổi), việc tính toán mất khoảng 80–150ms trên máy chủ CPU thông thường.
- Với người dùng đăng nhập hợp lệ một lần, độ trễ 100ms là không đáng kể về mặt trải nghiệm. Nhưng đối với kẻ tấn công cố gắng thử hàng triệu mật khẩu mỗi giây bằng dàn GPU, tốc độ dò quét bị kéo tụt xuống mức không khả thi về mặt thời gian và kinh tế.

#### 2.2. Kiến trúc `PasswordEncoder` & `DelegatingPasswordEncoder`

Spring Security trừu tượng hóa toàn bộ nghiệp vụ xử lý mật khẩu qua interface `PasswordEncoder`:

- `String encode(CharSequence rawPassword)`: Băm mật khẩu gốc và nhúng Salt vào chuỗi kết quả.

- `boolean matches(CharSequence rawPassword, String encodedPassword)`: Kiểm tra tính hợp lệ của mật khẩu gốc với chuỗi hash đã lưu.

- `default boolean upgradeEncoding(String encodedPassword)`: Kiểm tra xem chuỗi hash hiện tại có cần được nâng cấp lên thuật toán mạnh hơn hoặc Cost Factor cao hơn hay không.

```
                     +---------------------------------------+
                     |    <<interface>> PasswordEncoder      |
                     +---------------------------------------+
                     | + encode(rawPassword)                 |
                     | + matches(rawPassword, encoded)       |
                     | + upgradeEncoding(encoded)            |
                     +---------------------------------------+
                                         ▲
                                         │ implements
            +----------------------------┴----------------------------+
            |                                                         |
+------------------------------+                        +------------------------------+
|   BCryptPasswordEncoder      |                        |  DelegatingPasswordEncoder   |
+------------------------------+                        +------------------------------+
| - version: "$2a$" / "$2b$"   |                        | - defaultPasswordEncoderForId|
| - strength: 4..31 (cost)     |                        | - idToPasswordEncoder (Map)  |
+------------------------------+                        +------------------------------+

```

##### Cơ chế tiền tố thuật toán (Algorithm Prefix)

`DelegatingPasswordEncoder` sử dụng cơ chế tiền tố định danh dạng `{id}` ở đầu chuỗi hash để xác định thuật toán đã dùng để băm:

- BCrypt: `{bcrypt}$2a$10$e0MYzXyjpJS7Pd0RVvHwHe...`

- Argon2: `{argon2}$v=19$m=16384,t=2,p=1$...`

- PBKDF2: `{pbkdf2}$...`

**Ý nghĩa kiến trúc:**
Khi hệ thống InstaClone hoạt động qua nhiều năm và muốn nâng cấp thuật toán từ BCrypt sang Argon2 (thuật toán chống lại việc bẻ khóa bằng chip ASIC và GPU nhờ cơ chế Memory-Hard), ta chỉ cần đổi `defaultPasswordEncoderForId` sang Argon2. Khi người dùng cũ đăng nhập, `DelegatingPasswordEncoder` sẽ đọc prefix `{bcrypt}`, dùng `BCryptPasswordEncoder` để kiểm tra `matches()`. Nếu mật khẩu đúng, hàm `upgradeEncoding()` trả về `true`, hệ thống lập tức băm lại mật khẩu bằng Argon2 mới và cập nhật ngược vào database mà người dùng không hề bị gián đoạn hay bắt buộc phải đặt lại mật khẩu.

#### 2.3. Cấu trúc nội tại của chuỗi băm BCrypt

Một chuỗi băm BCrypt chuẩn mực có độ dài 60 ký tự, cấu trúc thành 4 phần riêng biệt:

```
  $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
  ├───┼───┼──────────────────────┼─────────────────────────────┤
   (1) (2)          (3)                        (4)

```

1. `$2a$`: Định danh phiên bản thuật toán BCrypt.
2. `10$`: Cost factor (Work factor) ở mức $2^{10} = 1024$ vòng lặp tính toán.
3. `N9qo8uLOickgx2ZMRZoMye`: 22 ký tự biểu diễn chuỗi **Salt** (16 bytes sinh ngẫu nhiên được mã hóa Radix-64).
4. `IjZAgcfl7p92ldGxad68LJZdL17lhWy`: 31 ký tự biểu diễn kết quả băm mã hóa cuối cùng.

Khi gọi hàm `passwordEncoder.matches(rawPassword, encodedPassword)`:

1. Thuật toán tự động trích xuất chuỗi Salt (phần 3) và Cost Factor (phần 2) trực tiếp từ `encodedPassword`.

2. Thuật toán lấy `rawPassword` người dùng vừa nhập, băm lại với đúng chuỗi Salt và Cost Factor vừa trích xuất.

3. So sánh chuỗi hash vừa tạo với phần băm cũ (phần 4) bằng thuật toán so sánh chuỗi có thời gian cố định (**Constant-time comparison**) nhằm chống tấn công phân tích thời gian thực thi (Timing Attack).

---

### 3. MINH HỌA CODE CHUẨN MỰC TRÊN INSTACLONE

Dưới đây là mã nguồn cấu hình mã hóa mật khẩu và API đăng ký tài khoản cho InstaClone sử dụng Java 21, Spring Boot 4.x và Spring Data JPA.

#### 3.1. Cấu hình Bean `PasswordEncoder`

```java
package com.instaclone.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Tạo Bean PasswordEncoder mặc định của Spring Security (DelegatingPasswordEncoder).
     * Mặc định sử dụng BCrypt với prefix {bcrypt}, tự động hỗ trợ tương thích ngược
     * và nâng cấp thuật toán mã hóa trong tương lai mà không làm gián đoạn hệ thống.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

```

#### 3.2. DTO Đăng ký tài khoản

```java
package com.instaclone.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 100, message = "Password must have at least 8 characters")
    String password,

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email format")
    String email
) {}

```

#### 3.3. DTO Phản hồi thông tin người dùng

```java
package com.instaclone.application.dto.response;

import java.util.Set;

public record UserProfileResponse(
    Long id,
    String username,
    String email,
    Set<String> roles
) {}

```

#### 3.4. Triển khai Service Đăng ký tài khoản và Mã hóa

```java
package com.instaclone.application.service;

import com.instaclone.application.dto.request.RegisterRequest;
import com.instaclone.application.dto.response.UserProfileResponse;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfileResponse registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        // BẮT BUỘC: Mã hóa mật khẩu thông qua DelegatingPasswordEncoder trước khi lưu
        String encodedPassword = passwordEncoder.encode(request.password());
        log.debug("Encoded password generated with prefix for user: {}", request.username());

        User newUser = new User(
            request.username(),
            encodedPassword,
            request.email(),
            Set.of("ROLE_USER")
        );

        User savedUser = userRepository.save(newUser);

        return new UserProfileResponse(
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getRoles()
        );
    }
}

```

#### 3.5. REST Controller Tiếp nhận Đăng ký

```java
package com.instaclone.presentation.controller;

import com.instaclone.application.dto.request.RegisterRequest;
import com.instaclone.application.dto.response.UserProfileResponse;
import com.instaclone.application.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRegistrationController {

    private final UserService userService;

    public AuthRegistrationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

```

---

### 4. BẪY LỖI KINH ĐIỂN & RỦI RO BẢO MẬT (GOTCHAS & PITFALLS)

- **Bẫy 1: Ngoại lệ `IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"**`

- _Hiện tượng:_ Người dùng không thể đăng nhập và hệ thống ném ra lỗi trên khi gọi hàm `matches()`.

- _Nguyên nhân:_ Database chứa dữ liệu mật khẩu cũ được lưu dưới dạng Plaintext hoặc chuỗi hash không có tiền tố `{id}` (ví dụ: `$2a$10$...` thay vì `{bcrypt}$2a$10$...`). Khi `DelegatingPasswordEncoder` đọc chuỗi hash, nó tìm kiếm tiền tố nằm giữa cặp dấu `{}`. Nếu không tìm thấy, id sẽ là `null` và hệ thống từ chối xử lý để đảm bảo an toàn.

- _Cách khắc phục:_ Viết một migration script (Flyway/Liquibase) để bổ sung tiền tố `{bcrypt}` vào trước toàn bộ chuỗi hash hiện có, hoặc cấu hình `setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder())` trên `DelegatingPasswordEncoder` để xử lý chuỗi không có tiền tố.

- **Bẫy 2: Mã hóa mật khẩu nhiều lần (Double Encoding)**
- _Hiện tượng:_ Người dùng đăng ký thành công nhưng không bao giờ đăng nhập được với mật khẩu vừa tạo; hàm `matches()` luôn trả về `false`.
- _Nguyên nhân:_ Lập trình viên gọi `passwordEncoder.encode()` ở tầng Controller hoặc DTO Mapper, sau đó ở tầng Service lại tiếp tục gọi `passwordEncoder.encode()` một lần nữa trên chuỗi đã bị mã hóa.
- _Cách khắc phục:_ Duy trì quy tắc kiến trúc duy nhất: Việc mã hóa mật khẩu chỉ được diễn ra **duy nhất một lần** tại ranh giới tầng Business Service ngay trước khi gắn vào Entity.

- **Bẫy 3: Đặt Cost Factor quá cao làm nghẽn CPU (Denial of Service tự gây ra)**
- _Hiện tượng:_ Server bị treo cứng hoặc tăng vọt mức tiêu thụ CPU lên 100% khi chỉ có vài chục người dùng đăng nhập đồng thời.
- _Nguyên nhân:_ Lập trình viên cấu hình Cost Factor của BCrypt lên mức quá cao (ví dụ: 16 hoặc 18). Do cơ chế hàm số mũ $2^{strength}$, mức 16 mất khoảng 3–5 giây tính toán cho mỗi request mã hóa trên một CPU core.
- _Cách khắc phục:_ Benchmark trực tiếp trên máy chủ Production để đảm bảo thời gian xử lý của mỗi phép băm nằm trong khoảng tối ưu: **100ms – 250ms** (thông thường mức `strength = 10` đến `12` là tối ưu cho hạ tầng hiện nay).

---

### 5. BỘ CÂU HỎI TƯ DUY & PHẢN BIỆN (DEEP-DIVE QUESTIONS)

1. _Khi triển khai tính năng cập nhật thông tin cá nhân của người dùng trên InstaClone, nếu người dùng chỉ cập nhật Bio và Email mà không đổi mật khẩu, điều gì sẽ xảy ra nếu Service vô tình lấy mật khẩu hiện tại trong database và gọi lại `passwordEncoder.encode(user.getPassword())` trước khi `save()`?_
2. _Tại sao việc kiểm tra mật khẩu bằng phương thức `password.equals(storedPassword)` lại dẫn đến lỗ hổng Side-Channel Timing Attack, và lớp `MessageDigest.isEqual()` trong Spring Security giải quyết bài toán này như thế nào?_

---

---

## CHƯƠNG 4: CƠ CHẾ XÁC THỰC (AUTHENTICATION FLOW) & SECURITY CONTEXT

### 1. ĐẶT VẤN ĐỀ & CÂU HỎI TRỌNG TÂM

#### Bài toán thực tế trên mạng xã hội InstaClone

Sau khi người dùng đã đăng ký tài khoản trên InstaClone, hệ thống cần xử lý yêu cầu đăng nhập gửi kèm JSON Payload chứa `username` và `password`. Nếu xác thực thành công, hệ thống cần ghi nhớ trạng thái của người dùng để ở các request kế tiếp (ví dụ: lấy thông tin cá nhân `GET /api/v1/users/me`), Backend biết chính xác ai đang thực hiện request mà không bắt buộc họ phải gửi lại mật khẩu.

Nhiều lập trình viên tự xử lý luồng này bằng cách:

1. Tự tiêm `UserRepository` vào `LoginController`, so sánh chuỗi mật khẩu thủ công.

2. Tự lưu thông tin người dùng vào một trường `static` hoặc tự tạo một biến trong `HttpSession`.

**Thảm họa kiến trúc và an ninh xảy ra:**

- **Ô nhiễm ngữ cảnh luồng (Thread-Safety & Race Conditions):** Nếu lưu thông tin vào biến tĩnh hoặc chia sẻ tài nguyên không đúng chuẩn, người dùng A có thể nhìn thấy dữ liệu riêng tư của người dùng B khi Web Server tái sử dụng Thread (Thread Pooling).
- **Mất tính thống nhất với hệ sinh thái Spring:** Tự xử lý xác thực khiến hệ thống tách rời hoàn toàn khỏi các bộ lọc chuẩn của Spring Security. Hậu quả là các tính năng như `SecurityContextHolder`, bảo vệ phân quyền phương thức `@PreAuthorize`, hoặc tự động ghi nhật ký kiểm toán (Audit Logging) không thể hoạt động.

- **Lỗ hổng dò quét tài khoản (Username Enumeration):** Khi xác thực thất bại, lập trình viên thường bắt ngoại lệ và trả về các thông báo chi tiết như: _"Tên đăng nhập không tồn tại"_ hoặc _"Mật khẩu không chính xác"_. Điều này tạo điều kiện cho kẻ tấn công dò quét được danh sách toàn bộ các tài khoản có thật trên InstaClone.

#### Câu hỏi tư duy trọng tâm

> _Từ khoảnh khắc người dùng bấm nút gửi payload đăng nhập cho tới khi `SecurityContextHolder` lưu trữ thành công thông tin Principal, request đã đi qua những trạm kiểm soát nào trong Spring Security 7.x? Tại sao Spring Security 7.x lại áp dụng cơ chế Explicit Save kết hợp Jackson 3 cho SecurityContext thay vì tự động lưu ngầm định như các phiên bản cũ?_

---

### 2. PHÂN TÍCH KIẾN TRÚC & CƠ CHẾ NỘI TẠI (UNDER THE HOOD)

#### 2.1. Các thực thể cốt lõi trong mô hình xác thực (Core Interfaces)

Hệ thống xác thực của Spring Security được xây dựng dựa trên các thành phần độc lập nhưng phối hợp chặt chẽ theo mẫu thiết kế **Chain of Responsibility** và **Strategy Pattern**:

```
[ Client Request: {username, password} ]
                  │
                  ▼
   +───────────────────────────────+
   | AuthenticationFilter          |  Tạo UsernamePasswordAuthenticationToken
   | (hoặc Custom Auth Controller) |  (Trạng thái: Chưa xác thực - isAuthenticated = false)
   +──────────────┬────────────────+
                  │
                  ▼ Gọi authenticate(token)
   +───────────────────────────────+
   | AuthenticationManager         |  Giao diện điều phối tổng thể
   | (Thực thể: ProviderManager)   |  Duyệt qua danh sách AuthenticationProvider
   +──────────────┬────────────────+
                  │
                  ▼ Ủy quyền cho Provider thích hợp
   +───────────────────────────────+
   | DaoAuthenticationProvider     |  Xử lý xác thực Username / Password
   +──────────────┬────────────────+
                  │
         ┌────────┴────────────────────────┐
         │ 1. loadUserByUsername()         │ 2. matches(rawPassword, encodedPassword)
         ▼                                 ▼
+──────────────────────────+     +──────────────────────────+
| UserDetailsService       |     | PasswordEncoder          |
| (InstaCloneUserDetail...) |     | (DelegatingPassword...)  |
+──────────────────────────+     +──────────────────────────+

```

1. **`Authentication` (Interface):** Đại diện cho một yêu cầu xác thực hoặc một danh tính đã được xác thực. Gồm các phương thức cốt lõi:

- `getPrincipal()`: Trả về danh tính người dùng (thường là một thực thể `UserDetails` khi đã xác thực, hoặc chuỗi username khi chưa xác thực).

- `getCredentials()`: Trả về bằng chứng xác thực (ví dụ: chuỗi mật khẩu gốc; trường này thường bị xóa sạch ngay sau khi xác thực thành công để chống rò rỉ bộ nhớ).

- `getAuthorities()`: Danh sách các quyền hạn được cấp (`Collection<? extends GrantedAuthority>`).

- `isAuthenticated()`: Trạng thái cờ `boolean` xác nhận danh tính đã được xác minh thành công hay chưa.

2. **`AuthenticationManager` & `ProviderManager`:**

- `AuthenticationManager` là interface đầu não nhận một đối tượng `Authentication` chưa xác thực và trả về một `Authentication` đã xác thực hoàn toàn (kèm quyền hạn) hoặc ném ra `AuthenticationException`.

- `ProviderManager` là lớp triển khai mặc định của `AuthenticationManager`, chứa một danh sách `List<AuthenticationProvider>`. Nó lần lượt duyệt qua từng Provider xem lớp nào hỗ trợ (`supports()`) kiểu token hiện tại để xử lý.

3. **`DaoAuthenticationProvider`:**

- Là `AuthenticationProvider` mặc định chuyên xử lý cặp giá trị username/password.

- Trách nhiệm: Gọi `UserDetailsService.loadUserByUsername()` để nạp đối tượng `UserDetails` từ nguồn dữ liệu, sau đó gọi `PasswordEncoder.matches()` để so khớp mật khẩu.

4. **`UserDetailsService`:**

- Interface chỉ có duy nhất một phương thức: `UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;`.

- Chỉ đóng vai trò là cầu nối truy xuất dữ liệu (Data Access Adapter), tuyệt đối **không** chứa logic so khớp mật khẩu hay phân quyền.

#### 2.2. Sơ đồ tuần tự quá trình xác thực (Sequence Diagram)

Dưới đây là luồng tương tác chi tiết khi một request đăng nhập được xử lý qua `DaoAuthenticationProvider`:

```
Client             ProviderManager      DaoAuthProvider     UserDetailsService    PasswordEncoder    SecurityContextHolder
  │                       │                    │                    │                   │                     │
  │── POST /login ───────>│                    │                    │                   │                     │
  │   (user, pass)        │                    │                    │                   │                     │
  │                       │── authenticate() ─>│                    │                   │                     │
  │                       │                    │── loadUserBy... ──>│                   │                     │
  │                       │                    │   (username)       │                   │                     │
  │                       │                    │<── UserDetails ────│                   │                     │
  │                       │                    │                    │                   │                     │
  │                       │                    │── matches(raw, encoded) ──────────────>│                     │
  │                       │                    │<── true ───────────────────────────────│                     │
  │                       │                    │                                                              │
  │                       │                    │── Tạo Authentication Token mới (authenticated = true)        │
  │                       │                    │   (Xóa bỏ credentials nhạy cảm)                              │
  │                       │<── Auth Token ─────│                                                              │
  │                       │                    │                                                              │
  │                       │────────────────────┼─────────────────────────────────────────────────────────────>│ Lưu trữ Context
  │<── 200 OK (Login OK) ─│                    │                                                              │ (ThreadLocal)

```

#### 2.3. Cơ chế Quản lý `SecurityContext` trong Spring Security 7.x

##### `SecurityContextHolder` và `ThreadLocal`

`SecurityContextHolder` là lớp tiện ích tĩnh dùng để truy cập `SecurityContext` hiện tại. Mặc định, nó sử dụng chiến lược `MODE_THREADLOCAL`, nghĩa là mỗi luồng (Thread) xử lý HTTP request trong Web Container sẽ sở hữu một bản sao `SecurityContext` độc lập gắn liền với vòng đời của luồng đó.

##### Sự thay đổi đột phá trong 7.x: `SecurityContextHolderFilter` & Explicit Save

- _Lịch sử:_ Ở các phiên bản Spring Security 5.x trở về trước, lớp `SecurityContextPersistenceFilter` hoạt động theo cơ chế ngầm định (Implicit Save). Bất kỳ thay đổi nào trên `SecurityContext` trong suốt vòng đời của request đều tự động được đồng bộ vào `HttpSession` khi request kết thúc. Điều này dẫn đến sự cố tranh chấp ghi (Race condition) và không tối ưu hóa tài nguyên cho kiến trúc bất đồng bộ hoặc RESTful.
- _Chuẩn Spring Security 7.x:_ Sử dụng **`SecurityContextHolderFilter`** kết hợp cơ chế **Explicit Save** (Lưu tường minh):

- `SecurityContext` **chỉ được lưu** vào `SecurityContextRepository` (ví dụ: Session hoặc Cache Store) khi và chỉ khi lập trình viên hoặc framework gọi tường minh phương thức `securityContextRepository.saveContext(context, request, response)`.

- Sau khi kết thúc chuỗi Filter, `SecurityContextHolderFilter` tự động dọn sạch ngữ cảnh bằng lệnh `SecurityContextHolder.clearContext()` để đảm bảo luồng (Thread) khi trả về Thread Pool không bị ô nhiễm dữ liệu của người dùng trước.

##### Tuần tự hóa phiên làm việc chuẩn Jackson 3

Trong Spring Framework 7 và Spring Security 7.x, toàn bộ các lớp đối tượng bảo mật liên quan đến phiên (`SecurityContext`, `User`, `OAuth2AuthenticationToken`) khi lưu trữ phân tán vào Redis/Session Store đều được tuần tự hóa thông qua bộ thư viện **Jackson 3** (Jakarta JSON Dataformats) thay cho Jackson 2 cũ, giúp cải thiện tốc độ serialize và ngăn ngừa triệt để các lỗ hổng thực thi mã từ xa (RCE Deserialization Attacks).

---

### 3. MINH HỌA CODE CHUẨN MỰC TRÊN INSTACLONE

Dưới đây là toàn bộ mã nguồn xử lý xác thực danh tính cho InstaClone trên nền tảng Java 21 và Spring Security 7.x.

#### 3.1. Cài đặt `UserDetails`: `InstaCloneUserDetails`

Thay vì để Entity `User` phụ thuộc trực tiếp vào các giao diện của Spring Security, ta tạo một lớp Adapter đóng gói danh tính người dùng:

```java
package com.instaclone.infrastructure.security.model;

import com.instaclone.domain.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class InstaCloneUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public InstaCloneUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}

```

#### 3.2. Cài đặt `UserDetailsService`: `InstaCloneUserDetailsService`

```java
package com.instaclone.infrastructure.security.service;

import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.UserRepository;
import com.instaclone.infrastructure.security.model.InstaCloneUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstaCloneUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public InstaCloneUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new InstaCloneUserDetails(user);
    }
}

```

#### 3.3. Cấu hình Xuất bản Bean `AuthenticationManager`

Trong Spring Security 7.x, để có thể tiêm `AuthenticationManager` vào Controller hoặc Service, ta xuất bản thông qua cấu hình chuẩn của `AuthenticationConfiguration`:

```java
package com.instaclone.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
public class AuthenticationManagerConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

```

#### 3.4. DTO Yêu cầu Đăng nhập

```java
package com.instaclone.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Username cannot be blank")
    String username,

    @NotBlank(message = "Password cannot be blank")
    String password
) {}

```

#### 3.5. Triển khai API Đăng nhập với Explicit Save chuẩn 7.x

Đoạn code dưới đây xử lý đăng nhập, bắt các lỗi xác thực để ngăn chặn việc rò rỉ danh tính (Username Enumeration) và thực thi cơ chế Explicit Save cho `SecurityContext`:

```java
package com.instaclone.presentation.controller;

import com.instaclone.application.dto.request.LoginRequest;
import com.instaclone.application.dto.response.UserProfileResponse;
import com.instaclone.infrastructure.security.model.InstaCloneUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationManager authenticationManager;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthenticationController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        try {
            // 1. Tạo Authentication Token chưa xác thực từ dữ liệu đăng nhập
            UsernamePasswordAuthenticationToken unauthenticatedToken =
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());

            // 2. Kích hoạt AuthenticationManager để thực thi toàn bộ chuỗi DaoAuthenticationProvider
            Authentication authentication = authenticationManager.authenticate(unauthenticatedToken);

            // 3. Khởi tạo SecurityContext mới và thiết lập Authentication thành công
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);

            // 4. CHUẨN 7.x EXPLICIT SAVE: Bắt buộc lưu tường minh Context vào Repository (Session)
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            log.info("User logged in successfully: {}", request.username());
            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (BadCredentialsException | DisabledException | LockedException ex) {
            // AN NINH: Luôn trả về thông báo chung để ngăn chặn lỗ hổng Username Enumeration
            log.warn("Authentication failed for username '{}': {}", request.username(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", "Invalid username or password"
            ));
        } catch (AuthenticationException ex) {
            log.error("System authentication error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Unauthorized",
                "message", "Authentication process failed"
            ));
        }
    }

    /**
     * API kiểm tra thông tin người dùng hiện tại từ SecurityContextHolder
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof InstaCloneUserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        UserProfileResponse response = new UserProfileResponse(
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            userDetails.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet())
        );

        return ResponseEntity.ok(response);
    }
}

```

---

### 4. BẪY LỖI KINH ĐIỂN & RỦI RO BẢO MẬT (GOTCHAS & PITFALLS)

- **Bẫy 1: Quên cơ chế Explicit Save trong Spring Security 7.x khiến Request kế tiếp bị 401**

- _Hiện tượng:_ Gọi API `/api/v1/auth/login` báo đăng nhập thành công (`200 OK`), nhưng ngay lập tức gọi API `/api/v1/auth/me` thì bị lỗi `401 Unauthorized`.

- _Nguyên nhân:_ Lập trình viên thiết lập `SecurityContextHolder.getContext().setAuthentication(auth)` nhưng **quên không gọi** `securityContextRepository.saveContext(context, request, response)`. Trong Spring Security 7.x với cơ chế Explicit Save, nếu không gọi lệnh lưu tường minh này, SecurityContext sẽ bị hủy khi luồng kết thúc và không bao giờ được ghi vào `HttpSession`.

- _Cách khắc phục:_ Luôn tiêm `SecurityContextRepository` và gọi hàm `saveContext()` sau khi cập nhật danh tính người dùng trong các Custom Auth Controller.

- **Bẫy 2: Lỗ hổng dò quét tài khoản (Username Enumeration) qua ngoại lệ**

- _Hiện tượng:_ Khi đăng nhập với tên người dùng không tồn tại thì hệ thống trả về: _"Tài khoản không tồn tại"_, còn khi nhập đúng tài khoản nhưng sai mật khẩu thì hệ thống trả về: _"Mật khẩu không chính xác"_.

- _Nguyên nhân:_ Bắt trực tiếp ngoại lệ `UsernameNotFoundException` và trả về chi tiết thông điệp lỗi cho Client. Mặc định trong `DaoAuthenticationProvider`, Spring Security đã chủ động thiết lập cờ `hideUserNotFoundExceptions = true` (chuyển đổi `UsernameNotFoundException` thành `BadCredentialsException`) để bảo vệ hệ thống. Tuy nhiên, nếu lập trình viên tự viết logic trong Controller mà bóc tách chi tiết hai thông điệp này, hệ thống sẽ bị lộ danh sách người dùng cho tin tặc khai thác.

- _Cách khắc phục:_ Luôn gom tất cả lỗi xác thực liên quan đến định danh/mật khẩu thành một thông điệp đồng nhất: _"Invalid username or password"_.

- **Bẫy 3: Mất thông tin xác thực khi xử lý tác vụ bất đồng bộ (`@Async`)**

- _Hiện tượng:_ Trong luồng xử lý chính, người dùng đã đăng nhập, nhưng khi gọi sang một phương thức gắn `@Async` (ví dụ: tạo thumbnail ảnh bài đăng trong background) thì `SecurityContextHolder.getContext().getAuthentication()` trả về `null`.

- _Nguyên nhân:_ `SecurityContextHolder` mặc định sử dụng `ThreadLocal`. Khi một phương thức chạy bất đồng bộ qua `@Async`, nó được thực thi trên một Thread hoàn toàn mới từ Thread Pool khác, luồng này không có bản sao dữ liệu của luồng chính.

- _Cách khắc phục:_ Cấu hình `DelegatingSecurityContextAsyncTaskExecutor` cho bộ điều phối luồng `TaskExecutor`, hoặc cấu hình `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)` cho các môi trường phù hợp.

---

### 5. BỘ CÂU HỎI TƯ DUY & PHẢN BIỆN (DEEP-DIVE QUESTIONS)

1. _Tại sao phương thức `loadUserByUsername()` trong `UserDetailsService` chỉ nhận một tham số duy nhất là `String username` mà không nhận mật khẩu? Về mặt phân tách trách nhiệm kiến trúc, tại sao việc so sánh mật khẩu lại thuộc về `AuthenticationProvider` chứ không phải `UserDetailsService`?_

2. _Khi người dùng hoàn tất đăng nhập thành công trên InstaClone, thuộc tính `credentials` (mật khẩu gốc) trong đối tượng `Authentication` được xử lý như thế nào trước khi lưu vào `SecurityContextHolder`? Việc giữ lại `credentials` trong bộ nhớ ứng dụng tiềm ẩn những rủi ro bảo mật nào?_
