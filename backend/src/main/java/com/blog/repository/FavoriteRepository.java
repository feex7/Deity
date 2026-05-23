package com.blog.repository;

import com.blog.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findAllByOrderByCreatedAtDesc();
    List<Favorite> findByCategoryOrderByCreatedAtDesc(String category);
    List<Favorite> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
        String titleKeyword, String descKeyword);
}