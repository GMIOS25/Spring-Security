package com.instaclone.infrastructure.security.config;

import com.instaclone.infrastructure.security.filter.InstaRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityChainsConfig {

    private final Clock clock;
    private final int maxRateLimit;
    private final List<Customizer<HttpSecurity>> customizers;

    public SecurityChainsConfig(
            Clock clock,
            @Value("${instaclone.ratelimit.max-per-minute:60}") int maxRateLimit,
            List<Customizer<HttpSecurity>> customizers) {
        this.clock = clock;
        this.maxRateLimit = maxRateLimit;
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
    public org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    @Bean
    public org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }

    @Bean
    public org.springframework.security.web.authentication.session.SessionAuthenticationStrategy sessionAuthenticationStrategy(
            org.springframework.security.core.session.SessionRegistry reg) {
        var concurrency = new org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy(reg);
        concurrency.setMaximumSessions(2);
        concurrency.setExceptionIfMaximumExceeded(false);
        return new org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy(java.util.List.of(
                concurrency,
                new org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy(),
                new org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy(reg)
        ));
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
        http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/feed/public/**"))
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain statelessApiFilterChain(HttpSecurity http, InstaRateLimitFilter rateLimitFilter) throws Exception {
        applyCustomizers(http);
        http.securityMatcher(new OrRequestMatcher(bearerHeaderMatcher(), credentialIssuingEndpoints()))
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/token", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain webSessionFilterChain(
            HttpSecurity http,
            InstaRateLimitFilter rateLimitFilter,
            org.springframework.security.core.session.SessionRegistry sessionRegistry) throws Exception {
        applyCustomizers(http);
        http.csrf(Customizer.withDefaults())
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sf -> sf.changeSessionId())
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry)
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
