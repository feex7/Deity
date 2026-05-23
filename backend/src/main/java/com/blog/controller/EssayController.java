package com.blog.controller;

import com.blog.entity.Essay;
import com.blog.repository.EssayRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/essays")
public class EssayController {

    private final EssayRepository repo;

    public EssayController(EssayRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Essay> list() {
        return repo.findAll();
    }

    @PostMapping
    public Essay create(@RequestBody Essay essay) {
        if (essay.getTitle() == null || essay.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (essay.getDate() == null) {
            essay.setDate(java.time.LocalDate.now());
        }
        return repo.save(essay);
    }

    @PutMapping("/{id}")
    public Essay update(@PathVariable Long id, @RequestBody Essay updates) {
        Essay e = repo.findById(id).orElse(null);
        if (e == null) return null;
        if (updates.getTitle() != null) e.setTitle(updates.getTitle());
        if (updates.getContent() != null) e.setContent(updates.getContent());
        if (updates.getTags() != null) e.setTags(updates.getTags());
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("success", true);
    }
}