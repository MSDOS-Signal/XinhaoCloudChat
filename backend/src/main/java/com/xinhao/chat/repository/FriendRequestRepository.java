package com.xinhao.chat.repository;

import com.xinhao.chat.entity.FriendRequest;
import com.xinhao.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    List<FriendRequest> findByReceiverAndStatus(User receiver, String status);
    // Return List to handle potential duplicates safely
    List<FriendRequest> findBySenderAndReceiver(User sender, User receiver);
}
