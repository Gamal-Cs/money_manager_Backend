// src/main/java/com/moneymanager/repository/TransactionRepository.java
package com.moneymanager.repository;

import com.moneymanager.dto.transaction.TransactionDTO;
import com.moneymanager.model.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId, Pageable pageable);
    // Find all transactions for a user
    List<Transaction> findByUserId(Long userId);

    // Find transactions for a user within date range
    List<Transaction> findByUserIdAndTransactionDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Find a specific transaction by ID and user ID
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    // Count transactions for a user
    long countByUserId(Long userId);
}