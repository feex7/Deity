package com.blog.controller;

import com.blog.entity.Plan;
import com.blog.repository.PlanRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanRepository repo;

    public PlanController(PlanRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Plan> list() {
        return repo.findAll();
    }

    @PostMapping
    public Plan create(@RequestBody Plan plan) {
        if (plan.getTitle() == null || plan.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (plan.getDate() == null) {
            plan.setDate(java.time.LocalDate.now());
        }
        return repo.save(plan);
    }

    @PutMapping("/{id}")
    public Plan update(@PathVariable Long id, @RequestBody Plan updates) {
        Plan p = repo.findById(id).orElse(null);
        if (p == null) return null;
        if (updates.getTitle() != null) p.setTitle(updates.getTitle());
        if (updates.getDescription() != null) p.setDescription(updates.getDescription());
        if (updates.getStatus() != null) p.setStatus(updates.getStatus());
        if (updates.getPriority() != null) p.setPriority(updates.getPriority());
        if (updates.getTags() != null) p.setTags(updates.getTags());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("success", true);
    }
}