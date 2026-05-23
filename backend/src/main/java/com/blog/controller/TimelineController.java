package com.blog.controller;

import com.blog.service.TimelineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineService service;

    public TimelineController(TimelineService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> getTimeline() {
        return service.getTimeline();
    }
}