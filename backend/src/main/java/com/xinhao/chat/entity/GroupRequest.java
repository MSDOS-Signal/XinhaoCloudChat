package com.xinhao.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "group_requests")
public class GroupRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // The inviter (usually group owner)

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver; // The invitee

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // PENDING, ACCEPTED, REJECTED
    private String status;
    
    private String reason; // Optional: "Invited you to join group X"

    @CreationTimestamp
    private LocalDateTime createTime;
}
