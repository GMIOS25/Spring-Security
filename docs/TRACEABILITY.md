# MA TRẬN TRUY VẾT (LT.md → code → test)

Ma trận này ánh xạ các khái niệm, bẫy bảo mật và câu hỏi trong `docs/LT.md` tới lớp code trong `src/main` và bài test tương ứng trong `src/test`.

| Chương / mục | Khái niệm trong LT.md | Hiện thực (src/main) | Test chứng minh (src/test) | Trạng thái |
| --- | --- | --- | --- | --- |
| 1 §2.1 (L150–163) | Identification, Credentials, Authentication, Principal, Authority, Trust Boundary | `domain.entity.User` | `LoginFlowTest` + `docs/QA.md` | ⬜ |
| 1 §2.2 (L165–206) | Servlet Filter đứng trước DispatcherServlet; Interceptor không bảo vệ tài nguyên ngoài MVC | `infrastructure.security.antipattern.FragileSecurityInterceptor`, `infrastructure.security.config.SecurityConfig` | `AntiPatternInterceptorTest`, `StaticResourceProtectedTest`, `FirewallVsInterceptorTest` | ✅ |
| 1 §2.3 (L208–214) | Least privilege, deny-by-default, `POST_DELETE_ANY` | `infrastructure.security.config.SecurityConfig` | `DenyByDefaultTest`, `BolaDeleteTest` | ✅ |
| 1 Bẫy 1 (L371) | 401 Unauthorized ≠ 403 Forbidden | `infrastructure.security.config.SecurityConfig`, `presentation.controller.ModerationController` | `StatusSemanticsTest` | ✅ |
| 1 Bẫy 2 (L376) | Không tin Frontend → BOLA / IDOR | `application.service.PostService`, `infrastructure.security.evaluator.PostSecurityEvaluator` | `BolaDeleteTest` | ✅ |
| 1 Q1, Q2 (L385–386) | 401 vs 403; filter đặt sau DispatcherServlet | `infrastructure.security.config.SecurityConfig` | `StatusSemanticsTest`, `FilterChainIntrospectionTest` + `docs/QA.md` | ✅ |
| 2 §2.1 (L434–493) | DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain, khớp chain đầu tiên | `infrastructure.security.config.SecurityChainsConfig` | `FilterChainIntrospectionTest` | ✅ |
| 2 §2.2 (L497–509) | PathPatternRequestMatcher; Lambda DSL, không `.and()`; `Customizer<HttpSecurity>` | `infrastructure.security.config.SecurityChainsConfig`, `ModularSecurityConfig` | `RemovedApisClaimsTest`, `ModularCustomizerTest` | ✅ |
| 2 §3.3 (L612–686) | Custom Filter kế thừa `OncePerRequestFilter` (rate limit) | `infrastructure.security.filter.InstaRateLimitFilter` | `RateLimitFilterTest` | ✅ |
| 2 Bẫy 1 (L694) | Filter chạy đúp khi đăng ký thành Bean | `infrastructure.security.config.SecurityChainsConfig` | `FilterRunsOnceTest` | ✅ |
| 2 Bẫy 2 (L714) | Thứ tự rule: cụ thể trước, tổng quát sau | `infrastructure.security.config.SecurityChainsConfig` | `AuthorizationRuleOrderTest` | ✅ |
| 2 Q1, Q2 (L724–725) | `permitAll` vẫn đi qua filter; nhiều chain + `@Order` | `infrastructure.security.config.SecurityChainsConfig` | `PermitAllStillTraversesFilterChainTest`, `FilterChainIntrospectionTest` | ✅ |
| 3 §2.1 (L767–776) | Salt ngẫu nhiên, slow hashing, work factor | `infrastructure.security.config.PasswordEncoderConfig` | `PasswordEncodingTest`, `CostFactorBenchmarkTest` | ✅ |
| 3 §2.2 (L778–826) | `DelegatingPasswordEncoder`, tiền tố `{id}`, `upgradeEncoding` | `infrastructure.security.config.PasswordEncoderConfig` | `EncodingUpgradeTest` | ✅ |
| 3 §2.3 (L828–866) | Cấu trúc 60 ký tự của BCrypt | `infrastructure.security.config.PasswordEncoderConfig` | `PasswordEncodingTest` | ✅ |
| 3 §3 (L870–1037) | Đăng ký + mã hoá một lần ở Service | `application.service.UserService`, `presentation.controller.AuthRegistrationController` | `RegisterApiTest` | ✅ |
| 3 Bẫy 1, 2, 3 (L1043–1059) | Thiếu prefix; double encoding; cost quá cao | `MissingPrefixTest`, `DoubleEncodingTest`, `CostFactorBenchmarkTest` | `MissingPrefixTest`, `DoubleEncodingTest`, `CostFactorBenchmarkTest` | ✅ |
| 3 Q1, Q2 (L1065–1066) | Re-encode khi cập nhật hồ sơ; timing attack | `application.service.UserService` | `DoubleEncodingTest`, `docs/QA.md` | ✅ |
| 4 §2.1–2.2 (L1113–1194) | Authentication, ProviderManager, DaoAuthenticationProvider, UDS, PasswordEncoder | `infrastructure.security.service.InstaCloneUserDetailsService`, `infrastructure.security.model.InstaCloneUserDetails`, `infrastructure.security.config.AuthenticationManagerConfig` | `LoginFlowTest`, `CredentialsErasedTest` (+ D20 trong `docs/QA.md`) | ✅ |
| 4 §2.3 (L1196–1209) | `ThreadLocal`, `SecurityContextHolderFilter`, Explicit Save, dọn context | `presentation.controller.AuthenticationController` | `SecurityContextIsolationTest`, `ForgetfulLoginTest` | ✅ |
| 4 §2.3 (L1211–1213) | Tuần tự hoá phiên bằng Jackson 3 | Đang chờ | ST-1 `Jackson3SessionSerializationTest` (stretch) | ⬜ |
| 4 Bẫy 1, 2, 3 (L1477–1499) | Quên Explicit Save; Username Enumeration; mất context khi `@Async` | `presentation.controller.AuthenticationController`, `infrastructure.security.config.AsyncConfig`, `application.service.AsyncDemoService` | `ForgetfulLoginTest`, `UsernameEnumerationTest`, `AsyncContextPropagationTest` | ✅ |
| 4 Q1, Q2 (L1505–1507) | UDS chỉ nhận username; credentials bị xoá | `infrastructure.security.service.InstaCloneUserDetailsService` | `docs/QA.md`, `CredentialsErasedTest` | ✅ |
| 5 §2.1 (L1559–1613) | `AuthorizationManager` thay `AccessDecisionManager` | `infrastructure.security.config.SecurityChainsConfig` | `AuthorizationManagerClaimsTest` | ✅ |
| 5 §2.2 (L1615–1621) | `hasRole` (tự thêm `ROLE_`) vs `hasAuthority` | `antipattern.RoleVsAuthorityDemoService` | `HasRoleVsHasAuthorityTest` | ✅ |
| 5 §2.3, §3 (L1623–1815) | Method security qua AOP; chống BOLA bằng `@PreAuthorize` | `application.service.PostService`, `infrastructure.security.evaluator.PostSecurityEvaluator`, `presentation.controller.PostController` | `BolaDeleteTest`, `PostVisibilityTest`, `ResponseHasNoSensitiveDataTest` | ✅ |
| 5 Bẫy 1, 2, 3 (L1823–1839) | Self-invocation; 403 vs 404; `@PostAuthorize` trên thao tác ghi | `antipattern.SelfInvocationDemo`, `antipattern.PostAuthorizeOnWriteDemo`, `application.service.PostService` | `SelfInvocationTest`, `PostVisibilityTest`, `PostAuthorizeOnWriteTest` | ✅ |
| 5 Q1, Q2 (L1845–1847) | Phương thức private + AOP; chi phí SpEL | `docs/QA.md` | `docs/QA.md` | ✅ |
| 6 §2.1 (L1898–1931) | Synchronizer Token; XOR masking chống BREACH | `presentation.controller.CsrfController`, `infrastructure.security.config.SecurityChainsConfig` | `CsrfProtectionTest`, `CsrfAttackSimulationTest` | ✅ |
| 6 §2.2 (L1933–1939) | Session fixation; giới hạn phiên đồng thời | `infrastructure.security.config.SecurityChainsConfig`, `presentation.controller.AuthenticationController`, `infrastructure.security.model.InstaCloneUserDetails` | `SessionFixationTest`, `ConcurrentSessionTest` | ✅ |
| 6 §2.3 (L1941–1947) | CORS, preflight, thứ tự `CorsFilter` | `infrastructure.security.config.SecurityChainsConfig` | `CorsPreflightTest` | ✅ |
| 6 §2.4 (L1949–1951) | `redirectToHttps` thay `requiresChannel` | `infrastructure.security.config.SecurityChainsConfig` | `HttpsRedirectTest` | ✅ |
| 6 §3.1 (L2013–2017) | Security headers | `infrastructure.security.config.SecurityChainsConfig` | `SecurityHeadersTest` | ✅ |
| 6 Bẫy 1, 2, 3 (L2084–2102) | Tắt CSRF bừa bãi; `*` + credentials; preflight bị 403 | `infrastructure.security.config.SecurityChainsConfig` | `ChainCsrfPolicyTest`, `CorsPreflightTest` | ✅ |
| 6 Q1, Q2 (L2108–2110) | CORS ≠ CSRF; JWT header vs JWT cookie | `docs/QA.md` | `CsrfAttackSimulationTest`, `StatelessChainTest` + `docs/QA.md` | ✅ |
| 7 §2.1 (L2152–2197) | Cấu trúc JWT, chữ ký bất đối xứng, payload không bí mật | Đang chờ | `JwtStructureTest`, `JwtVerificationTest`, `ResourceServerKeyTest` | ⬜ |
| 7 §2.2 (L2199–2240) | `BearerTokenAuthenticationFilter`, `JwtDecoder`, `JwtAuthenticationConverter`, kiểm `typ` | Đang chờ | `JwtVerificationTest`, `ClaimMappingTest` | ⬜ |
| 7 §2.3 (L2242–2252) | Access/Refresh token; thu hồi bằng blacklist `jti` + TTL | Đang chờ | `RefreshFlowTest`, `LogoutAllTest` | ⬜ |
| 7 Câu hỏi trọng tâm (L2146) | Xác thực stateless không cần DB | Đang chờ | `StatelessVerificationNoDbTest` | ⬜ |
| 7 Bẫy 1, 2, 3 (L2456–2473) | Quên `STATELESS`; claim `roles` vs `scope`; lộ dữ liệu trong payload | Đang chờ | `StatelessChainTest`, `ClaimMappingTest`, `JwtStructureTest` | ⬜ |
| 7 Q1, Q2 (L2479–2480) | Chỉ giữ public key; offline vs introspection/blacklist | Đang chờ | `ResourceServerKeyTest`, `LogoutAllTest` + `docs/QA.md` | ⬜ |
| 8 §2.1 (L2527–2555) | OAuth 2.1: PKCE bắt buộc, không Password Grant | Đang chờ | `OAuth2LoginPkceTest` | ⬜ |
| 8 §2.2 (L2557–2566) | MFA: `FactorGrantedAuthority`, yêu cầu đủ yếu tố | Đang chờ | `MfaDeleteAccountTest` | ⬜ |
| 8 §2.3 (L2568–2575) | One-Time Token, chống replay | Đang chờ | `OttLoginFlowTest` | ⬜ |
| 8 §2.4, §3.3 (L2577–2790) | `@WithMockUser`, `@WithUserDetails`, `jwt()`, MockMvc | Đang chờ | `PostControllerSecurityTest`, `TestPrincipalTypesTest` | ⬜ |
| 8 Bẫy 1, 2, 3 (L2798–2824) | Thiếu CSRF trong test; sai kiểu principal; redirect URI lỏng | Đang chờ | `CsrfInTestsTest`, `TestPrincipalTypesTest`, `OAuth2LoginPkceTest` | ⬜ |
| 8 Q1, Q2 (L2830–2831) | `@WebMvcTest` vs `@SpringBootTest`; Magic Link GET vs POST | Đang chờ | `WebMvcSliceVsFullStackTest`, `OttLoginFlowTest` + `docs/QA.md` | ⬜ |
