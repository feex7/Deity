package com.blog.controller;

import com.blog.entity.Favorite;
import com.blog.repository.FavoriteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteRepository repo;

    public FavoriteController(FavoriteRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Favorite> list(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
                keyword, keyword);
        }
        if (category != null && !category.isBlank()) {
            return repo.findByCategoryOrderByCreatedAtDesc(category);
        }
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public Favorite create(@RequestBody Favorite fav) {
        if (fav.getTitle() == null || fav.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (fav.getUrl() == null || fav.getUrl().isBlank()) {
            throw new IllegalArgumentException("链接不能为空");
        }
        return repo.save(fav);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Favorite update) {
        Favorite fav = repo.findById(id).orElse(null);
        if (fav == null) return ResponseEntity.notFound().build();
        fav.setTitle(update.getTitle());
        fav.setUrl(update.getUrl());
        fav.setDescription(update.getDescription());
        fav.setCategory(update.getCategory());
        return ResponseEntity.ok(repo.save(fav));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}