package com.xinhao.chat.service;

import com.xinhao.chat.dto.MomentRequest;
import com.xinhao.chat.entity.Moment;
import com.xinhao.chat.entity.User;
import com.xinhao.chat.repository.MomentRepository;
import com.xinhao.chat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MomentService {

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public Moment publishMoment(Long userId, MomentRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Moment moment = new Moment();
        moment.setUser(user);
        moment.setContent(request.getContent());
        moment.setImages(request.getImages());
        
        Moment savedMoment = momentRepository.save(moment);
        messagingTemplate.convertAndSend("/topic/moments", savedMoment);
        return savedMoment;
    }

    public List<Moment> getFeed() {
        return momentRepository.findAllByOrderByCreateTimeDesc();
    }

    public void likeMoment(Long userId, Long momentId) {
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        if (moment.getLikedUserIds().contains(userId)) {
            moment.getLikedUserIds().remove(userId);
        } else {
            moment.getLikedUserIds().add(userId);
        }
        momentRepository.save(moment);
        messagingTemplate.convertAndSend("/topic/moments", moment);
    }

    public void commentMoment(Long userId, Long momentId, String content, Long replyToUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));

        com.xinhao.chat.entity.Comment comment = new com.xinhao.chat.entity.Comment();
        comment.setUser(user);
        comment.setContent(content);

        if (replyToUserId != null) {
            User replyToUser = userRepository.findById(replyToUserId)
                    .orElseThrow(() -> new RuntimeException("回复的用户不存在"));
            comment.setReplyToUser(replyToUser);
        }
        
        moment.getComments().add(comment);
        momentRepository.save(moment);
        messagingTemplate.convertAndSend("/topic/moments", moment);
    }

    public void deleteMoment(Long userId, Long momentId) {
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new RuntimeException("动态不存在"));
        
        if (!moment.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权删除他人动态");
        }
        
        momentRepository.delete(moment);
    }
}
