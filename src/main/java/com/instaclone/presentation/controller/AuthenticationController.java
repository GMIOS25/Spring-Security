package com.instaclone.presentation.controller;

import com.instaclone.application.dto.request.LoginRequest;
import com.instaclone.application.dto.response.UserProfileResponse;
import com.instaclone.infrastructure.security.model.InstaCloneUserDetails;
import com.instaclone.infrastructure.security.ott.MagicLinkMailer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final MagicLinkMailer magicLinkMailer;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Autowired
    public AuthenticationController(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            @Autowired(required = false) MagicLinkMailer magicLinkMailer) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.magicLinkMailer = magicLinkMailer;
    }

    public AuthenticationController(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this(authenticationManager, sessionAuthenticationStrategy, null);
    }

    @GetMapping("/ott/latest")
    public ResponseEntity<Map<String, String>> getLatestOttToken() {
        String token = (magicLinkMailer != null) ? magicLinkMailer.getLastToken() : null;
        return ResponseEntity.ok(Map.of("token", token != null ? token : ""));
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            // 1. Tạo Authentication Token chưa xác thực
            UsernamePasswordAuthenticationToken unauthenticatedToken =
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());

            // 2. Kích hoạt AuthenticationManager (ProviderManager -> DaoAuthenticationProvider)
            Authentication authentication = authenticationManager.authenticate(unauthenticatedToken);

            // MFA: If user was already authenticated in this session (e.g., via OTT), merge authorities
            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null && existingAuth.isAuthenticated() && existingAuth.getName().equals(authentication.getName())) {
                authentication = authentication.toBuilder()
                        .authorities(auths -> auths.addAll(existingAuth.getAuthorities()))
                        .build();
            }

            // 3. Khởi tạo SecurityContext mới và thiết lập Authentication thành công
            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);

            // 4. Kích hoạt SessionAuthenticationStrategy (D9 / B6: Session fixation & concurrency)
            sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

            // 5. CHUẨN 7.x EXPLICIT SAVE: Bắt buộc lưu tường minh Context vào Repository (Session)
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            log.info("User logged in successfully: {}", request.username());
            return ResponseEntity.ok(Map.of("message", "Login successful"));

        } catch (BadCredentialsException | DisabledException | LockedException ex) {
            // AN NINH (Bẫy 2): Luôn trả về thông báo chung để ngăn chặn Username Enumeration
            log.warn("Authentication failed for username '{}': {}", request.username(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Invalid username or password"
            ));
        } catch (AuthenticationException ex) {
            log.error("System authentication error: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Authentication process failed"
            ));
        }
    }

    @GetMapping({"/api/v1/auth/me", "/api/v1/users/me"})
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "UNAUTHORIZED",
                    "message", "Not authenticated"
            ));
        }

        if (authentication.getPrincipal() instanceof InstaCloneUserDetails userDetails) {
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            UserProfileResponse response = new UserProfileResponse(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles
            );
            return ResponseEntity.ok(response);
        }

        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            Object userId = jwt.getClaim("userId");
            String username = jwt.getSubject();
            List<String> roles = jwt.getClaimAsStringList("roles");
            return ResponseEntity.ok(Map.of(
                    "id", userId != null ? userId : 0,
                    "username", username,
                    "roles", roles != null ? roles : java.util.List.of()
            ));
        }

        // Trường hợp khác (ví dụ: MockUser)
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "authorities", authorities
        ));
    }
}
