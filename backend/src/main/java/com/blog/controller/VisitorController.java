package com.blog.controller;

import com.blog.entity.Visitor;
import com.blog.repository.VisitorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController {

    private final VisitorRepository repo;

    public VisitorController(VisitorRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/track")
    public ResponseEntity<?> track(@RequestBody Map<String, String> body) {
        String visitorId = body.get("visitorId");
        if (visitorId == null || visitorId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "visitorId required"));
        }
        LocalDate today = LocalDate.now();
        if (!repo.existsByVisitorIdAndVisitDate(visitorId, today)) {
            Visitor v = new Visitor();
            v.setVisitorId(visitorId);
            v.setVisitDate(today);
            repo.save(v);
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayVisitors", repo.countDistinctByVisitDate(today));
        result.put("monthVisitors", repo.countDistinctByVisitDateBetween(monthStart, monthEnd));
        return result;
    }
}