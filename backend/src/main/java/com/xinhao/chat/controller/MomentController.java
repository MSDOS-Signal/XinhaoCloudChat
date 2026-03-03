package com.xinhao.chat.controller;

import com.xinhao.chat.dto.MomentRequest;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.MomentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moments")
public class MomentController {

    @Autowired
    private MomentService momentService;

    @PostMapping
    public ResponseEntity<?> publishMoment(@AuthenticationPrincipal User currentUser, @RequestBody MomentRequest request) {
        try {
            return ResponseEntity.ok(momentService.publishMoment(currentUser.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getFeed() {
        return ResponseEntity.ok(momentService.getFeed());
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeMoment(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
        try {
            momentService.likeMoment(currentUser.getId(), id);
            return ResponseEntity.ok("操作成功");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<?> commentMoment(
            @AuthenticationPrincipal User currentUser, 
            @PathVariable Long id, 
            @RequestBody java.util.Map<String, Object> body) {
        try {
            String content = (String) body.get("content");
            Object replyToVal = body.get("replyToUserId");
            Long replyToUserId = replyToVal != null ? ((Number) replyToVal).longValue() : null;
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("评论内容不能为空");
            }
            momentService.commentMoment(currentUser.getId(), id, content, replyToUserId);
            return ResponseEntity.ok("评论成功");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMoment(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
        try {
            momentService.deleteMoment(currentUser.getId(), id);
            return ResponseEntity.ok("删除成功");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
