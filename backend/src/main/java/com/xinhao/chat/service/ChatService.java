package com.xinhao.chat.service;

import com.xinhao.chat.entity.Group;
import com.xinhao.chat.entity.Message;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.GroupRepository;
import com.xinhao.chat.repository.MessageRepository;
import com.xinhao.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.xinhao.chat.repository.FriendshipRepository friendshipRepository;
    
    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AiService aiService;

    @org.springframework.transaction.annotation.Transactional
    public Message sendMessage(Long senderId, Long receiverId, Long groupId, String content, String type, Long replyToMessageId) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("发送者不存在"));
        
        Message message = new Message();
        message.setSender(sender);
        message.setContent(content);
        message.setType(type != null ? type : "text");
        message.setCreateTime(java.time.LocalDateTime.now());
        
        if (replyToMessageId != null) {
            Message replyTo = messageRepository.findById(replyToMessageId).orElse(null);
            message.setReplyToMessage(replyTo);
        }

        Message savedMessage;
        if (groupId != null) {
            Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
            if (!group.getMembers().contains(sender)) {
                throw new RuntimeException("你不是该群成员");
            }
            message.setGroup(group);
            savedMessage = messageRepository.save(message);
            
            // Update group last active time
            group.setLastActiveTime(java.time.LocalDateTime.now());
            groupRepository.save(group);
            
            // Broadcast to group topic
            messagingTemplate.convertAndSend("/topic/group/" + groupId, savedMessage);
        } else if (receiverId != null) {
            User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("接收者不存在"));
            
            // AI Logic
            if ("ai_assistant".equals(receiver.getUsername()) || receiver.getId().equals(1L) || receiver.getId().equals(6L)) {
                 message.setReceiver(receiver);
                 savedMessage = messageRepository.save(message);
                 
                 // Trigger AI asynchronously
                 final String userContent = content;
                 new Thread(() -> {
                     aiService.streamChat(senderId, userContent);
                 }).start();
            } else {
                // Check friendship BEFORE saving
                if (!friendshipRepository.existsByUserAndFriend(sender, receiver)) {
                    throw new RuntimeException("发送失败，请添加对方为好友");
                }
                
                message.setReceiver(receiver);
                savedMessage = messageRepository.save(message);
                
                // Update friendship last active time for both users
                friendshipRepository.findByUserAndFriend(sender, receiver).ifPresent(f -> {
                    f.setLastActiveTime(java.time.LocalDateTime.now());
                    friendshipRepository.save(f);
                });
                friendshipRepository.findByUserAndFriend(receiver, sender).ifPresent(f -> {
                    f.setLastActiveTime(java.time.LocalDateTime.now());
                    friendshipRepository.save(f);
                });
                
                // Send to receiver's user queue
                messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/messages", savedMessage);
                messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/messages", savedMessage);
            }
        } else {
            throw new RuntimeException("必须指定接收者或群组");
        }
        
        return savedMessage;
    }

    public void recallMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("消息不存在"));
        
        boolean isOwner = false;
        if (message.getGroup() != null) {
            isOwner = message.getGroup().getOwner().getId().equals(userId);
        }

        if (!message.getSender().getId().equals(userId) && !isOwner) {
            throw new RuntimeException("只能撤回自己的消息，或者群主可以撤回群成员消息");
        }
        
        // Check time limit (2 minutes) ONLY if not owner
        if (!isOwner && java.time.LocalDateTime.now().minusMinutes(2).isAfter(message.getCreateTime())) {
            throw new RuntimeException("发送超过2分钟的消息不能撤回");
        }
        
        message.setType("recall");
        message.setContent("撤回了一条消息");
        messageRepository.save(message);
        
        // Notify clients
        if (message.getGroup() != null) {
             messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(), message);
        } else {
             messagingTemplate.convertAndSendToUser(message.getReceiver().getUsername(), "/queue/messages", message);
             messagingTemplate.convertAndSendToUser(message.getSender().getUsername(), "/queue/messages", message);
        }
    }
    
    // Legacy support without replyTo
    public Message sendMessage(Long senderId, Long receiverId, Long groupId, String content, String type) {
        return sendMessage(senderId, receiverId, groupId, content, type, null);
    }

    public List<Message> getMessages(Long userId, Long otherUserId, Long groupId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));

        if (groupId != null) {
            Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("群组不存在"));
            if (!group.getMembers().contains(user)) {
                throw new RuntimeException("你不是该群成员");
            }
            return messageRepository.findByGroupOrderByCreateTimeAsc(group);
        } else if (otherUserId != null) {
            User otherUser = userRepository.findById(otherUserId).orElseThrow(() -> new RuntimeException("对方不存在"));
            return messageRepository.findChatHistory(user, otherUser);
        } else {
            throw new RuntimeException("参数错误");
        }
    }
}
