package com.instaclone.infrastructure.security.config;

import com.instaclone.infrastructure.security.filter.InstaRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityChainsConfig {

    private final Clock clock;
    private final int maxRateLimit;
    private final boolean requireHttps;
    private final List<String> allowedOrigins;
    private final List<Customizer<HttpSecurity>> customizers;

    public SecurityChainsConfig(
            Clock clock,
            @Value("${instaclone.ratelimit.max-per-minute:60}") int maxRateLimit,
            @Value("${instaclone.security.require-https:false}") boolean requireHttps,
            @Value("${instaclone.security.cors.allowed-origins:http://localhost:3000,https://instaclone.com,https://app.instaclone.com}") List<String> allowedOrigins,
            List<Customizer<HttpSecurity>> customizers) {
        this.clock = clock;
        this.maxRateLimit = maxRateLimit;
        this.requireHttps = requireHttps;
        this.allowedOrigins = allowedOrigins;
        this.customizers = customizers;
    }

    @Bean
    public InstaRateLimitFilter instaRateLimitFilter() {
        return new InstaRateLimitFilter(maxRateLimit, clock);
    }

    @Bean
    public FilterRegistrationBean<InstaRateLimitFilter> disableAutoRegistrationOfRateLimitFilter(InstaRateLimitFilter filter) {
        FilterRegistrationBean<InstaRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry reg) {
        var concurrency = new ConcurrentSessionControlAuthenticationStrategy(reg);
        concurrency.setMaximumSessions(2);
        concurrency.setExceptionIfMaximumExceeded(false);
        return new CompositeSessionAuthenticationStrategy(List.of(
                concurrency,
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(reg)
        ));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-CSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("X-XSRF-TOKEN", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void applyCustomizers(HttpSecurity http) {
        if (customizers != null) {
            for (Customizer<HttpSecurity> c : customizers) {
                c.customize(http);
            }
        }
    }

    private RequestMatcher bearerHeaderMatcher() {
        return request -> {
            String auth = request.getHeader("Authorization");
            return auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7);
        };
    }

    private RequestMatcher credentialIssuingEndpoints() {
        var p = PathPatternRequestMatcher.withDefaults();
        return new OrRequestMatcher(
            p.matcher(HttpMethod.POST, "/api/v1/auth/register"),
            p.matcher(HttpMethod.POST, "/api/v1/auth/token"),
            p.matcher(HttpMethod.POST, "/api/v1/auth/refresh")
        );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicFeedFilterChain(HttpSecurity http) throws Exception {
        applyCustomizers(http);
        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }
        http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/feed/public/**"))
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain statelessApiFilterChain(
            HttpSecurity http,
            InstaRateLimitFilter rateLimitFilter,
            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        applyCustomizers(http);
        http.securityMatcher(new OrRequestMatcher(bearerHeaderMatcher(), credentialIssuingEndpoints()))
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/token", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain webSessionFilterChain(
            HttpSecurity http,
            InstaRateLimitFilter rateLimitFilter,
            SessionRegistry sessionRegistry,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        applyCustomizers(http);
        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }

        var requestHandler = new XorCsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http.cors(c -> c.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
            )
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sf -> sf.changeSessionId())
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry)
                .expiredSessionStrategy(event -> {
                    HttpServletResponse response = event.getResponse();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Session expired due to concurrent login\"}");
                })
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .xssProtection(Customizer.withDefaults())
                .contentTypeOptions(Customizer.withDefaults())
            )
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/csrf/token").permitAll()
                .requestMatchers("/api/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                .requestMatchers("/api/v1/posts/**").authenticated()
                .requestMatchers("/uploads/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Access denied\"}");
                    }
                })
            )
            .addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class);
        return http.build();
    }
}
