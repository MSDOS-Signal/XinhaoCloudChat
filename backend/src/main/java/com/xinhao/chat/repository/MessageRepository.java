package com.xinhao.chat.repository;

import com.xinhao.chat.entity.Group;
import com.xinhao.chat.entity.Message;
import com.xinhao.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE (m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1) ORDER BY m.createTime ASC")
    List<Message> findChatHistory(User u1, User u2);

    List<Message> findByGroupOrderByCreateTimeAsc(Group group);

    @Query("SELECT COUNT(m) FROM Message m WHERE (m.receiver.id = :userId OR m.group.id IN (SELECT g.id FROM Group g JOIN g.members u WHERE u.id = :userId)) AND m.createTime > :lastReadTime AND m.sender.id != :userId")
    long countNewMessages(@Param("userId") Long userId, @Param("lastReadTime") LocalDateTime lastReadTime);

    @Query("SELECT m.sender.id, COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.createTime > :lastReadTime AND m.sender.id != :userId GROUP BY m.sender.id")
    List<Object[]> countNewMessagesBySender(@Param("userId") Long userId, @Param("lastReadTime") LocalDateTime lastReadTime);
    
    @Query("SELECT m.group.id, COUNT(m) FROM Message m WHERE m.group.id IN (SELECT g.id FROM Group g JOIN g.members u WHERE u.id = :userId) AND m.createTime > :lastReadTime AND m.sender.id != :userId GROUP BY m.group.id")
    List<Object[]> countNewMessagesByGroup(@Param("userId") Long userId, @Param("lastReadTime") LocalDateTime lastReadTime);
}
