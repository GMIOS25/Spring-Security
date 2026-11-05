package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebMvcSliceVsFullStackTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Full Stack: HttpClient gọi qua toàn bộ chuỗi Servlet thật, DispatcherServlet và Security")
    void testFullStackViaRealHttpClient() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // 1. Feed công khai qua toàn bộ chuỗi Servlet thật -> 200 OK
        HttpRequest publicReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/feed/public/explore"))
                .GET()
                .build();
        HttpResponse<String> publicResp = client.send(publicReq, HttpResponse.BodyHandlers.ofString());
        assertThat(publicResp.statusCode()).isEqualTo(200);

        // 2. Thao tác xóa khi chưa đăng nhập qua socket thật -> FilterChain trả 401 JSON
        HttpRequest unauthReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/posts/999"))
                .DELETE()
                .build();
        HttpResponse<String> unauthResp = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());
        assertThat(unauthResp.statusCode()).isEqualTo(401);
        assertThat(unauthResp.body()).contains("UNAUTHORIZED");
    }
}
