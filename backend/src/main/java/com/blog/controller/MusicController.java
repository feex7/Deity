package com.blog.controller;

import com.blog.entity.Music;
import com.blog.service.MusicService;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final MusicService service;

    public MusicController(MusicService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Music m : service.listAll()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("fileSize", m.getFileSize());
            map.put("createdAt", m.getCreatedAt());
            result.add(map);
        }
        return result;
    }

    @PostMapping
    public ResponseEntity<?> upload(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "文件为空"));
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "仅支持音频文件"));
            }
            Music music = service.upload(title, file);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", music.getId());
            r.put("title", music.getTitle());
            return ResponseEntity.ok(r);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id, HttpServletRequest request) {
        Path filePath = service.getFile(id);
        if (filePath == null) return ResponseEntity.notFound().build();
        try {
            long fileSize = Files.size(filePath);
            String rangeHeader = request.getHeader("Range");

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String rangeValue = rangeHeader.substring(6).split("-")[0];
                long start = rangeValue.isEmpty() ? 0 : Long.parseLong(rangeValue);
                long end = Math.min(start + 1024 * 1024, fileSize - 1);

                byte[] data = new byte[(int)(end - start + 1)];
                try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                    raf.seek(start);
                    raf.readFully(data);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
                headers.set("Accept-Ranges", "bytes");
                headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
                headers.setContentLength(data.length);

                return new ResponseEntity<>(new ByteArrayResource(data), headers, HttpStatus.PARTIAL_CONTENT);
            }

            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header("Accept-Ranges", "bytes")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}