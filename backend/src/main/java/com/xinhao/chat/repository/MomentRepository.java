package com.xinhao.chat.repository;

import com.xinhao.chat.entity.Moment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
    List<Moment> findAllByOrderByCreateTimeDesc();

    @Query("SELECT COUNT(c) FROM Moment m JOIN m.comments c WHERE m.user.id = :userId AND c.createTime > :lastReadTime AND c.user.id != :userId")
    long countNewComments(@Param("userId") Long userId, @Param("lastReadTime") LocalDateTime lastReadTime);
}
