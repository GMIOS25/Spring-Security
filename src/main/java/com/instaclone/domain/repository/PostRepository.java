package com.instaclone.domain.repository;

import com.instaclone.domain.entity.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<Post> findById(Long id);

    List<Post> findByAuthorId(Long authorId);

    void deleteAllByAuthorId(Long authorId);
}
