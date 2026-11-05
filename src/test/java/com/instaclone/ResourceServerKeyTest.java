package com.instaclone;

import com.instaclone.infrastructure.security.jwt.auth.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ResourceServerKeyTest {

    @Autowired
    private RSAPublicKey rsaPublicKey;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Chương 7 Q1: Resource Server xác minh token chỉ với RSAPublicKey")
    void nimbusJwtDecoderVerifiesWithOnlyPublicKey() {
        var auth = new UsernamePasswordAuthenticationToken(
                "alice",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtTokenService.generateAccessToken(auth, 1L);

        // Khởi tạo decoder độc lập CHỈ dùng Public Key
        NimbusJwtDecoder standaloneDecoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
        Jwt decoded = standaloneDecoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("alice");
        assertThat(decoded.getIssuer().toString()).isEqualTo("https://api.instaclone.com");
    }

    @Test
    @DisplayName("Chương 7 Q1: Quét package jwt.resource không chứa bất kỳ tham chiếu nào tới RSAPrivateKey hay JwtEncoder")
    void resourceServerPackageHasNoPrivateKeyOrEncoderReferences() throws Exception {
        String packagePath = "com/instaclone/infrastructure/security/jwt/resource";
        URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(packagePath);
        assertThat(packageUrl).isNotNull();

        File dir = new File(packageUrl.toURI());
        File[] classFiles = dir.listFiles((d, name) -> name.endsWith(".class"));
        assertThat(classFiles).isNotEmpty();

        List<String> violations = new ArrayList<>();

        for (File file : classFiles) {
            byte[] bytes;
            try (FileInputStream fis = new FileInputStream(file)) {
                bytes = fis.readAllBytes();
            }
            String bytecodeContent = new String(bytes, StandardCharsets.ISO_8859_1);

            if (bytecodeContent.contains("RSAPrivateKey") || bytecodeContent.contains("rsaPrivateKey")) {
                violations.add(file.getName() + " references RSAPrivateKey");
            }
            if (bytecodeContent.contains("JwtEncoder") || bytecodeContent.contains("jwtEncoder")) {
                violations.add(file.getName() + " references JwtEncoder");
            }
        }

        assertThat(violations).isEmpty();
    }
}
