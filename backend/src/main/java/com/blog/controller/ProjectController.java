package com.blog.controller;

import com.blog.entity.Project;
import com.blog.repository.ProjectRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository repo;

    public ProjectController(ProjectRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Project> list() {
        return repo.findAll();
    }

    @PostMapping
    public Project create(@RequestBody Project project) {
        if (project.getTitle() == null || project.getTitle().isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(java.time.LocalDate.now());
        }
        return repo.save(project);
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable Long id, @RequestBody Project updates) {
        Project p = repo.findById(id).orElse(null);
        if (p == null) return null;
        if (updates.getTitle() != null) p.setTitle(updates.getTitle());
        if (updates.getDescription() != null) p.setDescription(updates.getDescription());
        if (updates.getUrl() != null) p.setUrl(updates.getUrl());
        if (updates.getStatus() != null) p.setStatus(updates.getStatus());
        if (updates.getTags() != null) p.setTags(updates.getTags());
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("success", true);
    }
}