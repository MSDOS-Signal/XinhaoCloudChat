package com.xinhao.chat.controller;

import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@AuthenticationPrincipal User currentUser, @RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content required");
        }
        
        // Asynchronously start AI stream
        // In a real app, we might want to use @Async or a thread pool
        new Thread(() -> {
            aiService.streamChat(currentUser.getId(), content);
        }).start();

        return ResponseEntity.ok("AI processing started");
    }
}
