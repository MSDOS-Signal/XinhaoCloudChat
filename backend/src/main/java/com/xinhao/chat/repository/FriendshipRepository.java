package com.xinhao.chat.repository;

import com.xinhao.chat.entity.Friendship;
import com.xinhao.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByUser(User user);
    boolean existsByUserAndFriend(User user, User friend);
    java.util.Optional<Friendship> findByUserAndFriend(User user, User friend);
    List<Friendship> findAllByUserAndFriend(User user, User friend);
}
