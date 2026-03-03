package com.xinhao.chat.controller;

import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<?> sendMessage(@AuthenticationPrincipal User currentUser, @RequestBody Map<String, Object> payload) {
        try {
            Object receiverIdVal = payload.get("receiverId");
            Long receiverId = receiverIdVal != null ? ((Number) receiverIdVal).longValue() : null;

            Object groupIdVal = payload.get("groupId");
            Long groupId = groupIdVal != null ? ((Number) groupIdVal).longValue() : null;

            Object replyToMessageIdVal = payload.get("replyToMessageId");
            Long replyToMessageId = replyToMessageIdVal != null ? ((Number) replyToMessageIdVal).longValue() : null;

            String content = (String) payload.get("content");
            String type = (String) payload.get("type");

            return ResponseEntity.ok(chatService.sendMessage(currentUser.getId(), receiverId, groupId, content, type, replyToMessageId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/recall")
    public ResponseEntity<?> recallMessage(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
        try {
            chatService.recallMessage(currentUser.getId(), id);
            return ResponseEntity.ok("撤回成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMessages(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long friendId,
            @RequestParam(required = false) Long groupId) {
        try {
            return ResponseEntity.ok(chatService.getMessages(currentUser.getId(), friendId, groupId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
