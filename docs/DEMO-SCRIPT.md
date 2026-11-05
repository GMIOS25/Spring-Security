# Kịch Bản Trình Diễn Bảo Mật (DEMO-SCRIPT)

Tài liệu này cung cấp các kịch bản chạy thử bằng `curl` theo từng chương lý thuyết trong `docs/LT.md`, chỉ rõ kết quả mong đợi và trỏ trực tiếp tới dòng tài liệu tương ứng.

> **Ghi chú chuẩn OAuth 2.1:** Ứng dụng tuân thủ chuẩn OAuth 2.1 hiện đại, đã khai tử hoàn toàn Resource Owner Password Credentials Grant (Password Grant) và Implicit Grant. Mọi luồng đăng nhập của bên thứ ba đều sử dụng Authorization Code Grant kết hợp PKCE (`S256`).

---

## 1. Chương 1: Ranh giới tin cậy & Ngữ nghĩa 401 Unauthorized vs 403 Forbidden

Tham chiếu `docs/LT.md` dòng 369–386 (Bẫy 1 & Câu hỏi 1).

### Kịch bản 1.1: Yêu cầu chưa xác thực (Unauthenticated) -> 401
Yêu cầu tạo bài viết nhưng không gửi bất kỳ thông tin nhận thực nào:
```bash
curl -i -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{"caption":"Anonymous post attempt","imageUrl":"https://img/test.jpg"}'
```
- **Kết quả mong đợi:** HTTP `401 Unauthorized`.
- **Giải thích:** Người dùng chưa cung cấp danh tính hợp lệ, hệ thống từ chối ngay tại tầng Filter trước khi request vào DispatcherServlet.

### Kịch bản 1.2: Người dùng hợp lệ nhưng thiếu quyền hạn (Forbidden) -> 403
Người dùng `alice` (vai trò `ROLE_USER`) cố tình truy cập khu vực kiểm duyệt của Moderator/Admin:
```bash
curl -i -X GET http://localhost:8080/api/v1/moderation/reports \
  -u alice:Password123!
```
- **Kết quả mong đợi:** HTTP `403 Forbidden`.
- **Giải thích:** Danh tính `alice` đã được xác thực thành công (401 không xảy ra), nhưng không đủ quyền `hasAnyRole('MODERATOR', 'ADMIN')` nên bị từ chối với mã 403.

---

## 2. Chương 2: Filter Chain & Giới hạn tần suất (Rate Limiting)

Tham chiếu `docs/LT.md` dòng 434–493 và dòng 612–718 (Chain order & Bẫy 1).

### Kịch bản 2.1: Truy cập Public Feed trên Chain 1 (Order 1) -> 200
```bash
curl -i -X GET http://localhost:8080/api/v1/feed/public/explore
```
- **Kết quả mong đợi:** HTTP `200 OK` (JSON danh sách bài viết công khai, không yêu cầu session hay token).

### Kịch bản 2.2: Rate Limit chặn spam request -> 429 Too Many Requests
Gửi nhiều request liên tục vượt quá ngưỡng cho phép (mặc định 60 req/phút):
```bash
for i in {1..65}; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/posts \
    -u alice:Password123! \
    -H "Content-Type: application/json" \
    -d '{"caption":"spam","imageUrl":"https://img/s.jpg"}'
done
```
- **Kết quả mong đợi:** Các request đầu trả về 201/success; khi vượt ngưỡng 60 req/phút, hệ thống trả về HTTP `429 Too Many Requests` kèm JSON:
  `{"error":"TOO_MANY_REQUESTS","message":"Rate limit exceeded. Please wait a moment."}`

---

## 3. Chương 3: Quản lý danh tính, BCrypt Salt & Đăng ký tài khoản

Tham chiếu `docs/LT.md` dòng 767–866 và 1041–1066.

### Kịch bản 3.1: So sánh Hash của hai người dùng cùng chung mật khẩu
Cả `alice` và `bob` đều dùng mật khẩu `Password123!`. Kiểm tra hash đã lưu trong database:
```sql
SELECT USERNAME, PASSWORD FROM USERS WHERE USERNAME IN ('alice', 'bob');
```
- **Kết quả:**
  - `alice`: `{bcrypt}$2a$10$vF3k7r8...` (60 ký tự sau prefix)
  - `bob`:   `{bcrypt}$2a$10$mK9x1p2...` (hoàn toàn khác hash của alice)
- **Giải thích:** Cơ chế BCrypt tự động sinh chuỗi Salt ngẫu nhiên 128-bit (22 ký tự Base64) cho mỗi lần mã hóa, triệt tiêu hoàn toàn tấn công Rainbow Table.

### Kịch bản 3.2: Đăng ký tài khoản mới an toàn
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"new_user","email":"new_user@instaclone.com","password":"Password123!"}'
```
- **Kết quả mong đợi:** HTTP `201 Created`. Body trả về chỉ chứa thông tin profile (`username`, `email`), tuyệt đối không để lộ hash hay mật khẩu.

---

## 4. Chương 4: Cơ chế xác thực Session & SecurityContext

Tham chiếu `docs/LT.md` dòng 1113–1209 và 1412–1449.

### Kịch bản 4.1: Đăng nhập Session tạo Cookie và thay đổi Session ID
Lấy CSRF token trước, sau đó đăng nhập:
```bash
# 1. Lấy CSRF token và Cookie
curl -c cookies.txt -i http://localhost:8080/api/v1/csrf/token

# Giả sử token lấy được là: $TOKEN
curl -b cookies.txt -c cookies.txt -i -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"username":"alice","password":"Password123!"}'
```
- **Kết quả mong đợi:** HTTP `200 OK`, server sinh cookie `JSESSIONID` mới qua `ChangeSessionIdAuthenticationStrategy` chống Session Fixation.

### Kịch bản 4.2: Truy cập thông tin cá nhân với Session đã lưu
```bash
curl -b cookies.txt -i http://localhost:8080/api/v1/users/me
```
- **Kết quả mong đợi:** HTTP `200 OK`, trả về JSON của `alice`.

---

## 5. Chương 5: Phân quyền đa tầng & Phòng thủ BOLA / IDOR

Tham chiếu `docs/LT.md` dòng 1623–1839 (Bẫy 2: 403 vs 404).

### Kịch bản 5.1: Kẻ tấn công Bob xóa bài viết của Alice (BOLA Attack) -> 403
```bash
# Giả sử Post 1 thuộc về Alice (alicePostId = 1)
curl -i -X DELETE http://localhost:8080/api/v1/posts/1 \
  -u bob:Password123! \
  -H "X-Requested-With: XMLHttpRequest"
```
- **Kết quả mong đợi:** HTTP `403 Forbidden`. `@PreAuthorize("@postSecurity.isOwner(#postId, authentication) or hasAuthority('POST_DELETE_ANY')")` ngăn chặn Bob can thiệp bài viết của Alice; bài viết vẫn nguyên vẹn trong DB.

### Kịch bản 5.2: Bob cố xem bài PRIVATE của Alice -> 404 Not Found (Bảo vệ bí mật tồn tại)
```bash
# Giả sử Post 2 là bài PRIVATE của Alice
curl -i -X GET http://localhost:8080/api/v1/posts/2 \
  -u bob:Password123!
```
- **Kết quả mong đợi:** HTTP `404 Not Found` (thay vì 403).
- **Giải thích:** Nếu trả về 403, kẻ tấn công sẽ biết được bài viết id=2 tồn tại. Trả về 404 theo chính sách ẩn giấu sự tồn tại của tài nguyên riêng tư.

### Kịch bản 5.3: Kiểm duyệt viên mod_alice xóa bài vi phạm -> 204 No Content
```bash
curl -i -X DELETE http://localhost:8080/api/v1/posts/4 \
  -u mod_alice:Password123!
```
- **Kết quả mong đợi:** HTTP `204 No Content`. `mod_alice` có quyền `POST_DELETE_ANY` nên xóa thành công bài viết của người khác.

---

## 6. Chương 6: CSRF Token (XOR Masking) & Tấn công giả mạo

Tham chiếu `docs/LT.md` dòng 1898–1939 và 2084–2102.

### Kịch bản 6.1: Gửi request thay đổi trạng thái nhưng thiếu CSRF Token -> 403
```bash
curl -b cookies.txt -i -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -d '{"caption":"Unprotected post","imageUrl":"https://img/csrf.jpg"}'
```
- **Kết quả mong đợi:** HTTP `403 Forbidden` (`Invalid CSRF Token`). `CsrfFilter` chặn đứng request trước khi tới nghiệp vụ.

### Kịch bản 6.2: Gửi kèm XOR Masked Token -> 201 Created
```bash
# Lấy token đã che từ API
TOKEN=$(curl -s -b cookies.txt http://localhost:8080/api/v1/csrf/token | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -b cookies.txt -i -X POST http://localhost:8080/api/v1/posts \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"caption":"Safe post with CSRF token","imageUrl":"https://img/csrf.jpg"}'
```
- **Kết quả mong đợi:** HTTP `201 Created`.

---

## 7. Chương 7: Stateless JWT, Refresh Token & Thu hồi Token tức thì (JTI Blacklist)

Tham chiếu `docs/LT.md` dòng 2152–2252 và 2428–2447.

### Kịch bản 7.1: Lấy cặp Access Token và Refresh Token
```bash
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"Password123!"}')

ACCESS_TOKEN=$(echo $RESPONSE | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
REFRESH_TOKEN=$(echo $RESPONSE | sed -E 's/.*"refreshToken":"([^"]+)".*/\1/')
```

### Kịch bản 7.2: Gọi Stateless API bằng Access Token qua Header Bearer
```bash
curl -i -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
- **Kết quả mong đợi:** HTTP `200 OK`. `StatelessApiFilterChain` (Chain 2) xác thực chữ ký số RSA public key mà không cần tra cứu database.

### Kịch bản 7.3: Đăng xuất mọi thiết bị (`logout-all`) và thu hồi ngay lập tức
```bash
curl -i -X POST http://localhost:8080/api/v1/auth/logout-all \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
- **Kết quả mong đợi:** HTTP `200 OK`. Refresh token bị xóa và `jti` của access token hiện tại được đưa vào `InMemoryJtiBlacklist`.

### Kịch bản 7.4: Gọi lại API bằng token vừa bị thu hồi -> 401
```bash
curl -i -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
- **Kết quả mong đợi:** HTTP `401 Unauthorized` ngay lập tức, chứng minh cơ chế Blacklist JTI khắc phục nhược điểm không thể thu hồi của JWT.

---

## 8. Chương 8: One-Time Token (OTT), OAuth 2.1 PKCE & Multi-Factor Auth (MFA)

Tham chiếu `docs/LT.md` dòng 2527–2585 và 2693.

### Kịch bản 8.1: Sinh mã One-Time Token (Magic Link)
```bash
curl -b cookies.txt -i -X POST http://localhost:8080/ott/generate \
  -H "X-XSRF-TOKEN: $TOKEN" \
  -d "username=alice"
```
- **Kết quả mong đợi:** HTTP `200 OK` với thông báo token đã gửi tới email. Mã token được ghi nhận an toàn trong bộ nhớ `MagicLinkMailer` (che log `ali***`).

### Kịch bản 8.2: Xác thực bước 1 (GET form - không tiêu thụ token) & bước 2 (POST login)
- `GET http://localhost:8080/login/ott?token=...` -> Trả về form xác nhận `200 OK` (token chưa bị vô hiệu).
- `POST http://localhost:8080/login/ott` với param `token=...` -> Chuyển hướng `302`, thiết lập phiên đăng nhập sở hữu nhân tố `FACTOR_OTT`.

### Kịch bản 8.3: Xóa tài khoản vĩnh viễn bắt buộc xác thực đa nhân tố (MFA)
Xóa tài khoản `DELETE /api/v1/users/me` yêu cầu đồng thời cả `FACTOR_PASSWORD` và `FACTOR_OTT`:
1. Nếu chỉ có mật khẩu: trả về `403 Forbidden`.
2. Nếu chỉ có OTT: trả về `403 Forbidden`.
3. Khi hoàn tất cả mật khẩu và OTT trong cùng một phiên: trả về `200 OK` và toàn bộ tài khoản, bài đăng, refresh token bị xóa vĩnh viễn.
