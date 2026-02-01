// src/main/java/com/moneymanager/repository/AccountRepository.java
package com.moneymanager.repository;

import com.moneymanager.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    List<Account> findByUserIdAndActiveTrue(Long userId);
    Optional<Account> findByIdAndUserId(Long id, Long userId);
    List<Account> findByUserIdAndAccountType(Long userId, Account.AccountType accountType);
}