package com.xinhao.chat.controller;

import com.xinhao.chat.entity.User;
import com.xinhao.chat.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<?> createGroup(@AuthenticationPrincipal User currentUser, @RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            Object memberIdsObj = payload.get("memberIds");
            if (memberIdsObj == null) throw new RuntimeException("Member IDs required");
            
            List<?> memberIdsList = (List<?>) memberIdsObj;
            List<Long> members = memberIdsList.stream()
                    .map(id -> ((Number) id).longValue())
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(groupService.createGroup(currentUser.getId(), name, members));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyGroups(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(groupService.getMyGroups(currentUser.getId()));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> inviteMembers(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @RequestBody Map<String, List<Long>> payload) {
        try {
            List<Long> members = payload.get("memberIds").stream().map(Number::longValue).collect(java.util.stream.Collectors.toList());
            groupService.inviteMembers(currentUser.getId(), id, members);
            return ResponseEntity.ok("邀请已发送，等待对方同意");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/requests")
    public ResponseEntity<?> getGroupRequests(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(groupService.getPendingRequests(currentUser.getId()));
    }
    
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<?> acceptGroupRequest(@PathVariable Long requestId) {
        try {
            groupService.acceptGroupRequest(requestId);
            return ResponseEntity.ok("已加入群聊");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<?> rejectGroupRequest(@PathVariable Long requestId) {
        try {
            groupService.rejectGroupRequest(requestId);
            return ResponseEntity.ok("已拒绝");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroup(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            return ResponseEntity.ok(groupService.updateGroup(currentUser.getId(), id, payload.get("name"), payload.get("avatar"), payload.get("description")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> kickMember(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @PathVariable Long userId) {
        try {
            groupService.kickMember(currentUser.getId(), id, userId);
            return ResponseEntity.ok("踢出成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/members/{userId}/mute")
    public ResponseEntity<?> muteMember(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @PathVariable Long userId, @RequestBody Map<String, Integer> payload) {
        try {
            Integer minutes = payload.get("minutes");
            if (minutes == null) throw new RuntimeException("Minutes required");
            groupService.muteMember(currentUser.getId(), id, userId, minutes);
            return ResponseEntity.ok("禁言成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/members/{userId}/mute")
    public ResponseEntity<?> unmuteMember(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @PathVariable Long userId) {
        try {
            groupService.unmuteMember(currentUser.getId(), id, userId);
            return ResponseEntity.ok("解除禁言成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGroup(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
        try {
            groupService.deleteGroup(currentUser.getId(), id);
            return ResponseEntity.ok("解散群聊成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<?> pinGroup(@AuthenticationPrincipal User currentUser, @PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        try {
            Boolean pinned = payload.get("pinned");
            if (pinned == null) pinned = true;
            groupService.pinGroup(currentUser.getId(), id, pinned);
            return ResponseEntity.ok("操作成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
