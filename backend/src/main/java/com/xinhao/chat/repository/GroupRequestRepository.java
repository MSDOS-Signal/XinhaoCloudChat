package com.xinhao.chat.repository;

import com.xinhao.chat.entity.Group;
import com.xinhao.chat.entity.GroupRequest;
import com.xinhao.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRequestRepository extends JpaRepository<GroupRequest, Long> {
    List<GroupRequest> findByReceiverAndStatus(User receiver, String status);
    Optional<GroupRequest> findBySenderAndReceiverAndGroupAndStatus(User sender, User receiver, Group group, String status);
    boolean existsByReceiverAndGroupAndStatus(User receiver, Group group, String status);
}
