package com.xinhao.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The commenter

    @ManyToOne
    @JoinColumn(name = "reply_to_user_id")
    private User replyToUser; // The user being replied to (optional)

    @Column(nullable = false)
    private String content;
    
    @CreationTimestamp
    private LocalDateTime createTime;
}
