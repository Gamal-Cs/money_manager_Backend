// src/main/java/com/moneymanager/repository/GoalRepository.java
package com.moneymanager.repository;

import com.moneymanager.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
    List<Goal> findByUserIdAndStatus(Long userId, Goal.Status status);
    Optional<Goal> findByIdAndUserId(Long id, Long userId);
    List<Goal> findByUserIdAndAccountId(Long userId, Long accountId);
}