package com.instaclone.infrastructure.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemovedApisClaimsTest {

    @Test
    @DisplayName("Chương 2 §2.2 & L418–424: Không còn dấu vết các API lỗi thời trong src/main/java")
    void testNoDeprecatedApisInMainSourceCode() throws IOException {
        Path srcMain = Path.of("src/main/java");
        Pattern forbidden = Pattern.compile("WebSecurityConfigurerAdapter|AntPathRequestMatcher|MvcRequestMatcher|\\.and\\(\\)");

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(srcMain)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> {
                     try {
                         String content = Files.readString(path);
                         Matcher matcher = forbidden.matcher(content);
                         if (matcher.find()) {
                             violations.add(path + " contains forbidden pattern: " + matcher.group());
                         }
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 });
        }

        assertThat(violations).as("Không được dùng các API cổ đã bị loại bỏ trong Spring Security 7").isEmpty();
    }

    @Test
    @DisplayName("Chương 2 §2.2: Các lớp cổ thực sự bị xoá khỏi classpath Spring Security 7")
    void testDeprecatedClassesRemovedFromClasspath() {
        assertThatThrownBy(() -> Class.forName("org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter"))
                .isInstanceOf(ClassNotFoundException.class);

        assertThatThrownBy(() -> Class.forName("org.springframework.security.web.util.matcher.AntPathRequestMatcher"))
                .isInstanceOf(ClassNotFoundException.class);

        assertThatThrownBy(() -> Class.forName("org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
