package com.blog.service;

import com.blog.entity.*;
import com.blog.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class TimelineService {

    private final ArticleRepository articleRepo;
    private final PlanRepository planRepo;
    private final EssayRepository essayRepo;
    private final ProjectRepository projectRepo;

    public TimelineService(ArticleRepository articleRepo, PlanRepository planRepo,
                           EssayRepository essayRepo, ProjectRepository projectRepo) {
        this.articleRepo = articleRepo;
        this.planRepo = planRepo;
        this.essayRepo = essayRepo;
        this.projectRepo = projectRepo;
    }

    public List<Map<String, Object>> getTimeline() {
        List<Map<String, Object>> timeline = new ArrayList<>();

        for (Article a : articleRepo.findAll()) {
            if (a.getTitle() == null || a.getTitle().isBlank()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", a.getId());
            entry.put("type", "ARTICLE");
            entry.put("typeLabel", "文章");
            entry.put("title", a.getTitle());
            entry.put("summary", a.getSummary() != null ? a.getSummary() : "");
            entry.put("date", a.getDate() != null ? a.getDate().toString() : "");
            entry.put("link", "/posts/" + a.getId());
            timeline.add(entry);
        }

        for (Plan p : planRepo.findAll()) {
            if (p.getTitle() == null || p.getTitle().isBlank()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", p.getId());
            entry.put("type", "PLAN");
            entry.put("typeLabel", "计划");
            entry.put("title", p.getTitle());
            entry.put("summary", p.getDescription() != null ? p.getDescription() : "");
            entry.put("date", p.getDate() != null ? p.getDate().toString() : "");
            entry.put("link", "/plans");
            timeline.add(entry);
        }

        for (Essay e : essayRepo.findAll()) {
            if (e.getTitle() == null || e.getTitle().isBlank()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", e.getId());
            entry.put("type", "ESSAY");
            entry.put("typeLabel", "随笔");
            entry.put("title", e.getTitle());
            entry.put("summary", "");
            entry.put("date", e.getDate() != null ? e.getDate().toString() : "");
            entry.put("link", "/essays");
            timeline.add(entry);
        }

        for (Project p : projectRepo.findAll()) {
            if (p.getTitle() == null || p.getTitle().isBlank()) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", p.getId());
            entry.put("type", "PROJECT");
            entry.put("typeLabel", "项目");
            entry.put("title", p.getTitle());
            entry.put("summary", p.getDescription() != null ? p.getDescription() : "");
            entry.put("date", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            entry.put("link", "/projects");
            timeline.add(entry);
        }

        timeline.sort((a, b) -> {
            String da = (String) a.getOrDefault("date", "");
            String db = (String) b.getOrDefault("date", "");
            return db.compareTo(da);
        });

        return timeline;
    }
}