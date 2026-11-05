package com.instaclone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HttpsRedirectTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @TestPropertySource(properties = "instaclone.security.require-https=false")
    class HttpsDisabled {
        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("D8: Khi require-https=false (mặc định), request HTTP không bị 302 chuyển hướng sang HTTPS")
        void httpRequestsNotRedirected() throws Exception {
            mockMvc.perform(get("/api/v1/feed/public/explore").secure(false))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @TestPropertySource(properties = "instaclone.security.require-https=true")
    class HttpsEnabled {
        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Chương 6 §2.4 & D8: Khi require-https=true, request HTTP bị 302 chuyển hướng tới https://")
        void httpRequestsRedirectedToHttps() throws Exception {
            mockMvc.perform(get("/api/v1/feed/public/explore").secure(false))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", startsWith("https://")));
        }
    }
}
