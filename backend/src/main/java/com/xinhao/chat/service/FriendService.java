package com.xinhao.chat.service;

import com.xinhao.chat.entity.FriendRequest;
import com.xinhao.chat.entity.Friendship;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.FriendRequestRepository;
import com.xinhao.chat.repository.FriendshipRepository;
import com.xinhao.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public void sendFriendRequest(Long senderId, Long receiverId, String reason) {
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("发送者不存在"));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("接收者不存在"));

        if (friendshipRepository.existsByUserAndFriend(sender, receiver)) {
            throw new RuntimeException("已经是好友了");
        }

        List<FriendRequest> existingRequests = friendRequestRepository.findBySenderAndReceiver(sender, receiver);
        if (!existingRequests.isEmpty()) {
            // Check if any is PENDING
            for (FriendRequest req : existingRequests) {
                if ("PENDING".equals(req.getStatus())) {
                    throw new RuntimeException("已发送过请求，请等待对方处理");
                }
            }
            
            // If all are REJECTED or ACCEPTED (but not friends?), create new or update one
            // We should probably clean up old ones or reuse one
            // Simple logic: delete all old ones and create new
            friendRequestRepository.deleteAll(existingRequests);
        }

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus("PENDING");
        request.setReason(reason);
        friendRequestRepository.save(request);
    }

    public List<FriendRequest> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        return friendRequestRepository.findByReceiverAndStatus(user, "PENDING");
    }

    @Transactional
    public void acceptFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("请求状态异常");
        }

        request.setStatus("ACCEPTED");
        friendRequestRepository.save(request);

        // Check if friendship already exists to prevent duplicates
        if (!friendshipRepository.existsByUserAndFriend(request.getSender(), request.getReceiver())) {
            // Create bidirectional friendship
            Friendship f1 = new Friendship();
            f1.setUser(request.getSender());
            f1.setFriend(request.getReceiver());
            friendshipRepository.save(f1);

            Friendship f2 = new Friendship();
            f2.setUser(request.getReceiver());
            f2.setFriend(request.getSender());
            friendshipRepository.save(f2);
        }
        
        // Notify sender (Requester) that request is accepted
        java.util.Map<String, Object> payloadSender = new java.util.HashMap<>();
        payloadSender.put("type", "new_friend");
        payloadSender.put("friend", request.getReceiver()); // Acceptor is the new friend for Sender
        messagingTemplate.convertAndSendToUser(request.getSender().getUsername(), "/queue/friends", payloadSender);

        // Notify receiver (Acceptor) just in case (though they called the API)
        java.util.Map<String, Object> payloadReceiver = new java.util.HashMap<>();
        payloadReceiver.put("type", "new_friend");
        payloadReceiver.put("friend", request.getSender()); // Sender is the new friend for Receiver
        messagingTemplate.convertAndSendToUser(request.getReceiver().getUsername(), "/queue/friends", payloadReceiver);
    }
    
    public void rejectFriendRequest(Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("请求不存在"));
        request.setStatus("REJECTED");
        friendRequestRepository.save(request);
    }

    public List<User> getFriends(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        List<Friendship> friendships = friendshipRepository.findByUser(user);
        
        // Sort: Pinned desc, LastActiveTime desc, CreateTime desc
        friendships.sort((f1, f2) -> {
            boolean p1 = f1.getIsPinned() != null && f1.getIsPinned();
            boolean p2 = f2.getIsPinned() != null && f2.getIsPinned();
            if (p1 != p2) return p1 ? -1 : 1;
            
            LocalDateTime t1 = f1.getLastActiveTime() != null ? f1.getLastActiveTime() : f1.getCreateTime();
            LocalDateTime t2 = f2.getLastActiveTime() != null ? f2.getLastActiveTime() : f2.getCreateTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return friendships.stream()
                .map(f -> {
                    User friend = f.getFriend();
                    friend.setPinned(f.getIsPinned() != null && f.getIsPinned());
                    return friend;
                })
                .collect(Collectors.toList());
    }

    public void pinFriend(Long userId, Long friendId, boolean pin) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        User friend = userRepository.findById(friendId).orElseThrow(() -> new RuntimeException("好友不存在"));
        
        Friendship friendship = friendshipRepository.findByUserAndFriend(user, friend)
                .orElseThrow(() -> new RuntimeException("你们不是好友"));
        
        friendship.setIsPinned(pin);
        friendshipRepository.save(friendship);
    }
    
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        User friend = userRepository.findById(friendId).orElseThrow(() -> new RuntimeException("好友不存在"));
        
        // Delete bidirectional friendship
        // Use findAll to handle potential duplicates (which caused "Query did not return a unique result")
        List<Friendship> f1 = friendshipRepository.findAllByUserAndFriend(user, friend);
        friendshipRepository.deleteAll(f1);
        
        List<Friendship> f2 = friendshipRepository.findAllByUserAndFriend(friend, user);
        friendshipRepository.deleteAll(f2);
        
        // Reset friend request status to allow re-adding
        List<FriendRequest> requests1 = friendRequestRepository.findBySenderAndReceiver(user, friend);
        if (!requests1.isEmpty()) friendRequestRepository.deleteAll(requests1);
        
        List<FriendRequest> requests2 = friendRequestRepository.findBySenderAndReceiver(friend, user);
        if (!requests2.isEmpty()) friendRequestRepository.deleteAll(requests2);

        // Notify friend that they were deleted (so they can update UI)
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", "friend_deleted");
        payload.put("friendId", userId); // The user who deleted is the one disappearing from friend's list
        messagingTemplate.convertAndSendToUser(friend.getUsername(), "/queue/friends", payload);
    }
}
