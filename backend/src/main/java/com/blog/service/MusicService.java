package com.blog.service;

import com.blog.entity.Music;
import com.blog.repository.MusicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class MusicService {

    private final MusicRepository repo;
    private final Path uploadDir = Paths.get("uploads/music");

    public MusicService(MusicRepository repo) {
        this.repo = repo;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建音乐上传目录", e);
        }
    }

    public List<Music> listAll() {
        return repo.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public Music upload(String title, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        String ext = ".mp3";
        if (originalName != null && originalName.contains(".")) {
            String rawExt = originalName.substring(originalName.lastIndexOf("."));
            ext = rawExt.replaceAll("[^a-zA-Z0-9.]", "");
            if (ext.length() > 8) ext = ".mp3";
        }
        String storedName = UUID.randomUUID().toString() + ext;
        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Music music = new Music();
        music.setTitle(title != null && !title.isBlank() ? title : originalName);
        music.setFileName(storedName);
        music.setFileSize(file.getSize());
        return repo.save(music);
    }

    @Transactional
    public void delete(Long id) {
        Music music = repo.findById(id).orElse(null);
        if (music == null) return;
        try {
            Files.deleteIfExists(uploadDir.resolve(music.getFileName()));
        } catch (IOException ignored) {}
        repo.deleteById(id);
    }

    public Path getFile(Long id) {
        Music music = repo.findById(id).orElse(null);
        if (music == null) return null;
        Path file = uploadDir.resolve(music.getFileName());
        return Files.exists(file) ? file : null;
    }
}