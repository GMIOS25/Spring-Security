package com.instaclone.infrastructure.bootstrap;

import com.instaclone.domain.entity.Post;
import com.instaclone.domain.entity.User;
import com.instaclone.domain.repository.PostRepository;
import com.instaclone.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile({"dev", "test"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PostRepository postRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataSeeder: Database already seeded, skipping.");
            return;
        }

        log.info("DataSeeder: Seeding demo data for dev/test profiles...");
        String encodedPassword = passwordEncoder.encode("Password123!");

        User alice = userRepository.save(new User("alice", encodedPassword, "alice@instaclone.com", Set.of("ROLE_USER")));
        User bob = userRepository.save(new User("bob", encodedPassword, "bob@instaclone.com", Set.of("ROLE_USER")));
        User charlie = userRepository.save(new User("charlie", encodedPassword, "charlie@instaclone.com", Set.of("ROLE_USER")));
        User modAlice = userRepository.save(new User("mod_alice", encodedPassword, "mod_alice@instaclone.com", Set.of("ROLE_USER", "ROLE_MODERATOR")));
        User cuongAdmin = userRepository.save(new User("cuong_admin", encodedPassword, "cuong_admin@instaclone.com", Set.of("ROLE_USER", "ROLE_ADMIN")));

        // Seed posts: alice has 1 PUBLIC + 1 PRIVATE + 1 FOLLOWERS_ONLY; bob has 1 PUBLIC
        postRepository.save(new Post("Alice public post", "https://images.instaclone.local/p1.jpg", alice, Post.PostVisibility.PUBLIC));
        postRepository.save(new Post("Alice private post", "https://images.instaclone.local/p2.jpg", alice, Post.PostVisibility.PRIVATE));
        postRepository.save(new Post("Alice followers only post", "https://images.instaclone.local/p3.jpg", alice, Post.PostVisibility.FOLLOWERS_ONLY));

        postRepository.save(new Post("Bob public post", "https://images.instaclone.local/p4.jpg", bob, Post.PostVisibility.PUBLIC));

        log.info("DataSeeder: Successfully seeded 5 users and 4 posts.");
    }
}
