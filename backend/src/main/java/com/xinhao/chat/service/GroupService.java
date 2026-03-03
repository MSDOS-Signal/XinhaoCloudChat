package com.xinhao.chat.service;

import com.xinhao.chat.entity.Group;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.GroupRepository;
import com.xinhao.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    public Group createGroup(Long ownerId, String name, List<Long> memberIds) {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Group group = new Group();
        group.setName(name);
        group.setOwner(owner);
        group.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"); // Default group avatar
        group.setMutedMembers(new HashMap<>());

        Set<User> members = new HashSet<>();
        members.add(owner);
        
        for (Long id : memberIds) {
            userRepository.findById(id).ifPresent(members::add);
        }
        
        if (members.size() < 3) {
             throw new RuntimeException("群聊人数必须大于等于3人");
        }

        group.setMembers(members);
        return groupRepository.save(group);
    }

    public List<Group> getMyGroups(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        List<Group> groups = groupRepository.findByMembersContaining(user);
        
        // Populate pinned status and sort
        // Sort order: Pinned desc, LastActiveTime desc, CreateTime desc
        groups.forEach(g -> g.setPinned(user.getPinnedGroupIds().contains(g.getId())));
        
        groups.sort((g1, g2) -> {
            if (g1.isPinned() != g2.isPinned()) {
                return g1.isPinned() ? -1 : 1;
            }
            LocalDateTime t1 = g1.getLastActiveTime() != null ? g1.getLastActiveTime() : g1.getCreateTime();
            LocalDateTime t2 = g2.getLastActiveTime() != null ? g2.getLastActiveTime() : g2.getCreateTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        
        return groups;
    }

    public void pinGroup(Long userId, Long groupId, boolean pin) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        
        if (pin) {
            user.getPinnedGroupIds().add(groupId);
        } else {
            user.getPinnedGroupIds().remove(groupId);
        }
        userRepository.save(user);
    }

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.xinhao.chat.repository.GroupRequestRepository groupRequestRepository;

    public void inviteMembers(Long userId, Long groupId, List<Long> memberIds) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        User inviter = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!group.getMembers().contains(inviter)) {
            throw new RuntimeException("你不是该群成员");
        }

        // Instead of adding directly, create GroupRequest
        for (Long id : memberIds) {
            userRepository.findById(id).ifPresent(invitee -> {
                if (group.getMembers().contains(invitee)) return; // Already member
                
                // Check if already invited
                if (groupRequestRepository.existsByReceiverAndGroupAndStatus(invitee, group, "PENDING")) {
                    return;
                }

                com.xinhao.chat.entity.GroupRequest request = new com.xinhao.chat.entity.GroupRequest();
                request.setSender(inviter);
                request.setReceiver(invitee);
                request.setGroup(group);
                request.setStatus("PENDING");
                request.setReason(inviter.getNickname() + " 邀请你加入群聊 " + group.getName());
                groupRequestRepository.save(request);
                
                // Notify invitee
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", "group_invite");
                payload.put("request", request);
                messagingTemplate.convertAndSendToUser(invitee.getUsername(), "/queue/friends", payload); // Reuse friends queue for requests
            });
        }
    }
    
    public List<com.xinhao.chat.entity.GroupRequest> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        return groupRequestRepository.findByReceiverAndStatus(user, "PENDING");
    }
    
    public void acceptGroupRequest(Long requestId) {
        com.xinhao.chat.entity.GroupRequest request = groupRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("请求状态异常");
        }
        
        request.setStatus("ACCEPTED");
        groupRequestRepository.save(request);
        
        Group group = request.getGroup();
        group.getMembers().add(request.getReceiver());
        groupRepository.save(group);
        
        // Notify group update
        java.util.Map<String, Object> event = new java.util.HashMap<>();
        event.put("type", "update"); // Or 'member_added'
        messagingTemplate.convertAndSend("/topic/group/" + group.getId() + "/events", event);
        
        // Notify receiver to update group list (via simple message or refresh trigger)
        // Ideally we should push the new group to the user
    }
    
    public void rejectGroupRequest(Long requestId) {
        com.xinhao.chat.entity.GroupRequest request = groupRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));
        request.setStatus("REJECTED");
        groupRequestRepository.save(request);
    }

    public void muteMember(Long userId, Long groupId, Long targetUserId, int durationMinutes) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("只有群主可以禁言成员");
        }
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("群主不能禁言自己");
        }
        
        if (group.getMutedMembers() == null) {
            group.setMutedMembers(new HashMap<>());
        }
        
        LocalDateTime unmuteTime = LocalDateTime.now().plusMinutes(durationMinutes);
        group.getMutedMembers().put(targetUserId, unmuteTime);
        groupRepository.save(group);
        
        // Broadcast mute event
        HashMap<String, Object> event = new HashMap<>();
        event.put("type", "mute");
        event.put("groupId", groupId);
        event.put("userId", targetUserId);
        event.put("duration", durationMinutes);
        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/events", event);
    }
    
    public void unmuteMember(Long userId, Long groupId, Long targetUserId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("只有群主可以解除禁言");
        }
        if (group.getMutedMembers() != null) {
            group.getMutedMembers().remove(targetUserId);
        }
        groupRepository.save(group);
        
        // Broadcast unmute event
        HashMap<String, Object> event = new HashMap<>();
        event.put("type", "mute");
        event.put("groupId", groupId);
        event.put("userId", targetUserId);
        event.put("duration", 0); // 0 means unmute
        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/events", event);
    }

    public void kickMember(Long userId, Long groupId, Long targetUserId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("只有群主可以踢人");
        }
        if (userId.equals(targetUserId)) {
            throw new RuntimeException("群主不能踢自己");
        }
        group.getMembers().removeIf(m -> m.getId().equals(targetUserId));
        groupRepository.save(group);

        // Broadcast kick event
        HashMap<String, Object> event = new HashMap<>();
        event.put("type", "kick");
        event.put("groupId", groupId);
        event.put("userId", targetUserId);
        messagingTemplate.convertAndSend("/topic/group/" + groupId + "/events", event);
    }
    
    public Group updateGroup(Long userId, Long groupId, String name, String avatar, String description) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("只有群主可以修改群资料");
        }
        boolean changed = false;
        if (name != null && !name.trim().isEmpty()) { group.setName(name); changed = true; }
        if (avatar != null && !avatar.trim().isEmpty()) { group.setAvatar(avatar); changed = true; }
        if (description != null) { group.setDescription(description); changed = true; }
        
        Group saved = groupRepository.save(group);
        
        if (changed) {
            HashMap<String, Object> event = new HashMap<>();
            event.put("type", "update");
            event.put("groupId", groupId);
            event.put("name", saved.getName());
            event.put("avatar", saved.getAvatar());
            event.put("description", saved.getDescription());
            messagingTemplate.convertAndSend("/topic/group/" + groupId + "/events", event);
        }
        return saved;
    }

    public void deleteGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
        if (!group.getOwner().getId().equals(userId)) {
            throw new RuntimeException("只有群主可以解散群聊");
        }
        groupRepository.delete(group);
    }
}
