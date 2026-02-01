// src/main/java/com/moneymanager/service/AccountService.java
package com.moneymanager.service;

import com.moneymanager.dto.account.AccountDTO;
import com.moneymanager.dto.account.AccountRequest;
import com.moneymanager.model.Account;
import com.moneymanager.model.User;
import com.moneymanager.repository.AccountRepository;
import com.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountDTO createAccount(String userEmail, AccountRequest request) {
        User user = getUserByEmail(userEmail);

        // Validate account type
        Account.AccountType accountType;
        try {
            accountType = Account.AccountType.valueOf(request.accountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid account type");
        }

        Account account = Account.builder()
                .name(request.name())
                .accountType(accountType)
                .accountNumber(request.accountNumber())
                .balance(request.balance())
                .currency(request.currency())
                .description(request.description())
                .user(user)
                .active(true)
                .build();

        Account saved = accountRepository.save(account);
        return convertToDTO(saved);
    }

    public List<AccountDTO> getUserAccounts(String userEmail) {
        User user = getUserByEmail(userEmail);
        return accountRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AccountDTO updateAccount(Long accountId, String userEmail, AccountRequest request) {
        User user = getUserByEmail(userEmail);
        Account account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setName(request.name());
        account.setAccountType(Account.AccountType.valueOf(request.accountType().toUpperCase()));
        account.setAccountNumber(request.accountNumber());
        account.setBalance(request.balance());
        account.setCurrency(request.currency());
        account.setDescription(request.description());

        Account updated = accountRepository.save(account);
        return convertToDTO(updated);
    }

    public void deleteAccount(Long accountId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Account account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Soft delete
        account.setActive(false);
        accountRepository.save(account);
    }

    public AccountDTO getAccountById(Long accountId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Account account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return convertToDTO(account);
    }

    public BigDecimal getTotalBalance(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(user.getId());

        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> getBalanceByAccountType(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(user.getId());

        return accounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getAccountType().name(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Account::getBalance,
                                BigDecimal::add
                        )
                ));
    }

    public AccountDTO updateAccountBalance(Long accountId, String userEmail, BigDecimal newBalance) {
        User user = getUserByEmail(userEmail);
        Account account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setBalance(newBalance);
        Account updated = accountRepository.save(account);
        return convertToDTO(updated);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private AccountDTO convertToDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getName(),
                account.getAccountType().name(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getDescription(),
                account.getUser().getId(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.isActive()
        );
    }
}