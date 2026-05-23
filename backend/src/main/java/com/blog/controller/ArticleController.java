package com.blog.controller;

import com.blog.entity.Article;
import com.blog.repository.ArticleRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleRepository articleRepository;

    public ArticleController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @GetMapping("/articles")
    public List<Article> list() {
        return articleRepository.findAll();
    }

    @GetMapping("/articles/{id}")
    public Article get(@PathVariable Long id) {
        return articleRepository.findById(id).orElse(null);
    }

    @PostMapping("/articles")
    public Article create(@RequestBody Article article) {
        if (article.getTitle() == null || article.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (article.getTitle().length() > 200) {
            throw new IllegalArgumentException("标题不能超过200个字符");
        }
        if (article.getDate() == null) {
            article.setDate(java.time.LocalDate.now());
        }
        if (article.getAuthor() == null) {
            article.setAuthor("Admin");
        }
        return articleRepository.save(article);
    }

    @PutMapping("/articles/{id}")
    public Article update(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        Article article = articleRepository.findById(id).orElse(null);
        if (article == null) return null;

        if (updates.containsKey("title")) article.setTitle((String) updates.get("title"));
        if (updates.containsKey("summary")) article.setSummary((String) updates.get("summary"));
        if (updates.containsKey("category")) article.setCategory((String) updates.get("category"));
        if (updates.containsKey("content")) article.setContent((String) updates.get("content"));
        if (updates.containsKey("tags")) article.setTags((String) updates.get("tags"));

        return articleRepository.save(article);
    }

    @DeleteMapping("/articles/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        articleRepository.deleteById(id);
        return Map.of("success", true);
    }
}