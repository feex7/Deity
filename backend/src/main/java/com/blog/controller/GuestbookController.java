package com.blog.controller;

import com.blog.entity.Guestbook;
import com.blog.repository.GuestbookRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/guestbook")
public class GuestbookController {

    private final GuestbookRepository repo;

    public GuestbookController(GuestbookRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Guestbook> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Guestbook entry) {
        if (entry.getNickname() != null && entry.getNickname().length() > 20) {
            return ResponseEntity.badRequest().body(Map.of("error", "昵称不能超过20个字符"));
        }
        if (entry.getContent() == null || entry.getContent().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "留言内容不能为空"));
        }
        if (entry.getContent().length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "留言内容不能超过500个字符"));
        }
        Guestbook saved = repo.save(entry);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}