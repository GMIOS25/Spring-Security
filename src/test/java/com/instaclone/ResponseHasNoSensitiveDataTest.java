package com.instaclone;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResponseHasNoSensitiveDataTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithUserDetails("bob")
    @DisplayName("D7: JSON bài đăng không chứa password hay hash BCrypt ($2a$), chỉ trả authorUsername")
    void postResponseContainsNoSensitiveUserData() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Post post = postRepository.save(new Post("Public Post for D7 test", "https://img/d7.jpg", alice, Post.PostVisibility.PUBLIC));

        MvcResult result = mockMvc.perform(get("/api/v1/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.caption").value("Public Post for D7 test"))
                .andExpect(jsonPath("$.authorUsername").value("alice"))
                .andReturn();

        String body = result.getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain("password")
                .doesNotContain("$2a$")
                .doesNotContain("{bcrypt}")
                .doesNotContain("email");
    }
}
