package com.xinhao.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(columnDefinition = "TEXT")
    private String content;

    // text, image, file, recall
    private String type;

    @ManyToOne
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @CreationTimestamp
    private LocalDateTime createTime;
}
