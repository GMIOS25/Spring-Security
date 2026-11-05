package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationManager;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationManagerClaimsTest {

    @Test
    @DisplayName("Kiểm chứng L1570: AccessDecisionManager, AccessDecisionVoter, FilterSecurityInterceptor đã bị xóa bỏ hoàn toàn khỏi classpath")
    void legacyAuthorizationClassesAreRemoved() {
        assertThatThrownBy(() -> Class.forName("org.springframework.security.access.AccessDecisionManager"))
                .isInstanceOf(ClassNotFoundException.class);

        assertThatThrownBy(() -> Class.forName("org.springframework.security.access.AccessDecisionVoter"))
                .isInstanceOf(ClassNotFoundException.class);

        assertThatThrownBy(() -> Class.forName("org.springframework.security.web.access.intercept.FilterSecurityInterceptor"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    @DisplayName("Kiểm chứng L1609-1613: AuthorizationManager là interface duy nhất với phương thức authorize")
    void authorizationManagerHasAuthorizeMethod() {
        Method[] methods = AuthorizationManager.class.getMethods();
        List<String> methodNames = Arrays.stream(methods).map(Method::getName).toList();

        assertThat(methodNames).contains("authorize");

        // Ghi nhận trạng thái phương thức check(...) theo L1613
        boolean hasCheck = methodNames.contains("check");
        System.out.println("AuthorizationManager methods: " + methodNames);
        System.out.println("AuthorizationManager has check method: " + hasCheck);
    }
}
