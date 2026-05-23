package com.blog.controller;

import com.blog.config.AdminConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final AdminConfig adminConfig;

    public AdminController(AdminConfig adminConfig) {
        this.adminConfig = adminConfig;
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            log.warn("验证请求缺少 username 或 password");
            return Map.of("success", false, "message", "用户名或密码不能为空");
        }

        if (password.length() > 128) {
            return Map.of("success", false, "message", "密码错误");
        }

        boolean usernameMatch = adminConfig.getUsername().equals(username);
        boolean passwordMatch = adminConfig.verify(password);

        if (!usernameMatch || !passwordMatch) {
            log.warn("管理员验证失败: username={}", username);
            return Map.of("success", false, "message", "密码错误");
        }

        log.info("管理员验证成功");
        return Map.of("success", true, "message", "验证成功");
    }
}