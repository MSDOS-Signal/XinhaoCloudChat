package com.xinhao.chat.controller;

import com.xinhao.chat.dto.UpdateUserRequest;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            User updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable Long id, @RequestBody com.xinhao.chat.dto.UpdateSettingsRequest request) {
        try {
            User updatedUser = userService.updateSettings(id, request.getSearchable());
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/unread")
    public ResponseEntity<?> getUnreadCounts(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUnreadCounts(id));
    }

    @PostMapping("/{id}/read/{type}")
    public ResponseEntity<?> markRead(@PathVariable Long id, @PathVariable String type) {
        userService.updateLastReadTime(id, type);
        return ResponseEntity.ok("Updated");
    }
}
