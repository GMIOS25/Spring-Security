package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("benchmark")
class CostFactorBenchmarkTest {

    @Test
    @DisplayName("Bẫy 3: Benchmark thời gian mã hóa BCrypt ở các mức Cost Factor 10, 12, 14")
    void benchmarkCostFactors() {
        String password = "Password123!";

        // Warmup
        new BCryptPasswordEncoder(6).encode(password);

        // Strength 10
        long start10 = System.nanoTime();
        new BCryptPasswordEncoder(10).encode(password);
        long elapsed10Ms = (System.nanoTime() - start10) / 1_000_000;

        // Strength 12
        long start12 = System.nanoTime();
        new BCryptPasswordEncoder(12).encode(password);
        long elapsed12Ms = (System.nanoTime() - start12) / 1_000_000;

        // Strength 14
        long start14 = System.nanoTime();
        new BCryptPasswordEncoder(14).encode(password);
        long elapsed14Ms = (System.nanoTime() - start14) / 1_000_000;

        System.out.printf("--- BCrypt Cost Factor Benchmark ---%n");
        System.out.printf("Strength 10: %d ms (tối ưu cho web app)%n", elapsed10Ms);
        System.out.printf("Strength 12: %d ms%n", elapsed12Ms);
        System.out.printf("Strength 14: %d ms (nguy cơ DoS khi tải cao)%n", elapsed14Ms);
        System.out.printf("-----------------------------------%n");

        // Assert strength 10 dưới ngưỡng rộng 1500ms để không bị flaky trên các máy CPU chậm
        assertThat(elapsed10Ms).isLessThanOrEqualTo(1500);
        // Cost factor tăng 2 mũ: strength 12 phải lâu hơn strength 10
        assertThat(elapsed14Ms).isGreaterThan(elapsed10Ms);
    }
}
