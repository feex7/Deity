package com.blog.controller;

import com.blog.entity.HotTopic;
import com.blog.repository.HotTopicRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hot-topics")
public class HotTopicController {

    private final HotTopicRepository repo;

    public HotTopicController(HotTopicRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<HotTopic> list() {
        return repo.findAll();
    }

    @PostMapping
    public HotTopic create(@RequestBody HotTopic topic) {
        if (topic.getDate() == null) {
            topic.setDate(java.time.LocalDate.now());
        }
        return repo.save(topic);
    }

    @PutMapping("/{id}")
    public HotTopic update(@PathVariable Long id, @RequestBody HotTopic updates) {
        HotTopic t = repo.findById(id).orElse(null);
        if (t == null) return null;
        if (updates.getTitle() != null) t.setTitle(updates.getTitle());
        if (updates.getSummary() != null) t.setSummary(updates.getSummary());
        if (updates.getHeatScore() != null) t.setHeatScore(updates.getHeatScore());
        if (updates.getSource() != null) t.setSource(updates.getSource());
        if (updates.getSourceUrl() != null) t.setSourceUrl(updates.getSourceUrl());
        return repo.save(t);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("success", true);
    }
}