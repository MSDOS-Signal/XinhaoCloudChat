package com.xinhao.chat.controller;

import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @PostMapping("/request/{receiverId}")
    public ResponseEntity<?> sendRequest(
            @AuthenticationPrincipal User currentUser, 
            @PathVariable Long receiverId,
            @RequestParam(required = false, defaultValue = "") String reason) {
        try {
            friendService.sendFriendRequest(currentUser.getId(), receiverId, reason);
            return ResponseEntity.ok("请求已发送");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getPendingRequests(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendService.getPendingRequests(currentUser.getId()));
    }

    @PostMapping("/accept/{requestId}")
    public ResponseEntity<?> acceptRequest(@PathVariable Long requestId) {
        try {
            friendService.acceptFriendRequest(requestId);
            return ResponseEntity.ok("已添加好友");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId) {
        try {
            friendService.rejectFriendRequest(requestId);
            return ResponseEntity.ok("已拒绝");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getFriends(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(friendService.getFriends(currentUser.getId()));
    }
    
    @PostMapping("/{id}/pin")
    public ResponseEntity<?> pinFriend(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean pinned = payload.get("pinned");
            if (pinned == null) pinned = true;
            friendService.pinFriend(currentUser.getId(), id, pinned);
            return ResponseEntity.ok("操作成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFriend(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
        try {
            friendService.deleteFriend(currentUser.getId(), id);
            return ResponseEntity.ok("好友已删除");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
