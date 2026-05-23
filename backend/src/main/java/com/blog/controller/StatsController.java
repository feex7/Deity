package com.blog.controller;

import com.blog.repository.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ProjectRepository projectRepo;
    private final ArticleRepository articleRepo;
    private final PlanRepository planRepo;

    public StatsController(ProjectRepository projectRepo,
                           ArticleRepository articleRepo,
                           PlanRepository planRepo) {
        this.projectRepo = projectRepo;
        this.articleRepo = articleRepo;
        this.planRepo = planRepo;
    }

    @GetMapping
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("projects", projectRepo.count());
        stats.put("articles", articleRepo.count());
        stats.put("plans", planRepo.count());
        return stats;
    }
}