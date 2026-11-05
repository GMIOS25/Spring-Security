package com.instaclone.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String caption;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum PostVisibility {
        PUBLIC, FOLLOWERS_ONLY, PRIVATE
    }

    public Post() {}

    public Post(String caption, String imageUrl, User author, PostVisibility visibility) {
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.author = author;
        this.visibility = visibility != null ? visibility : PostVisibility.PUBLIC;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public PostVisibility getVisibility() { return visibility; }
    public void setVisibility(PostVisibility visibility) { this.visibility = visibility; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
