package com.xinhao.chat.service;

import com.xinhao.chat.dto.LoginRequest;
import com.xinhao.chat.dto.RegisterRequest;
import com.xinhao.chat.dto.UpdateUserRequest;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.xinhao.chat.repository.MessageRepository messageRepository;

    @Autowired
    private com.xinhao.chat.repository.MomentRepository momentRepository;

    @Autowired
    private com.xinhao.chat.repository.FriendRequestRepository friendRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public java.util.Map<String, Object> getUnreadCounts(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        long messages = messageRepository.countNewMessages(userId, user.getLastReadChatTime());
        long comments = momentRepository.countNewComments(userId, user.getLastReadMomentsTime());
        // Friend Requests
        long requests = friendRequestRepository.findByReceiverAndStatus(user, "PENDING").size();

        java.util.Map<Long, Long> senderCounts = new java.util.HashMap<>();
        java.util.List<Object[]> bySender = messageRepository.countNewMessagesBySender(userId, user.getLastReadChatTime());
        for (Object[] row : bySender) {
            senderCounts.put((Long) row[0], (Long) row[1]);
        }
        
        java.util.Map<Long, Long> groupCounts = new java.util.HashMap<>();
        java.util.List<Object[]> byGroup = messageRepository.countNewMessagesByGroup(userId, user.getLastReadChatTime());
        for (Object[] row : byGroup) {
            groupCounts.put((Long) row[0], (Long) row[1]);
        }

        java.util.Map<String, Object> counts = new java.util.HashMap<>();
        counts.put("chat", messages);
        counts.put("moments", comments);
        counts.put("contacts", requests);
        counts.put("chatBySender", senderCounts);
        counts.put("chatByGroup", groupCounts);
        return counts;
    }

    public void updateLastReadTime(Long userId, String type) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("chat".equals(type)) {
            user.setLastReadChatTime(java.time.LocalDateTime.now());
        } else if ("moments".equals(type)) {
            user.setLastReadMomentsTime(java.time.LocalDateTime.now());
        }
        userRepository.save(user);
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setGender(request.getGender());
        // Set default avatar
        user.setAvatar("https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png");
        
        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        return user;
    }

    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            user.setNickname(request.getNickname());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            user.setAvatar(request.getAvatar());
        }

        return userRepository.save(user);
    }

    public java.util.List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameContaining(keyword)
                .stream()
                .filter(u -> u.getSearchable() == null || u.getSearchable())
                .collect(java.util.stream.Collectors.toList());
    }

    public User updateSettings(Long userId, Boolean searchable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (searchable != null) {
            user.setSearchable(searchable);
        }
        return userRepository.save(user);
    }
}
