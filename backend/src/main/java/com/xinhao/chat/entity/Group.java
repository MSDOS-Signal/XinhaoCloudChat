package com.xinhao.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "chat_groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members;

    private String avatar;

    private String description;

    @ElementCollection
    @CollectionTable(name = "group_mutes", joinColumns = @JoinColumn(name = "group_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "unmute_time")
    private java.util.Map<Long, LocalDateTime> mutedMembers;

    @CreationTimestamp
    private LocalDateTime createTime;

    private LocalDateTime lastActiveTime;

    @Transient
    private boolean pinned;
}
