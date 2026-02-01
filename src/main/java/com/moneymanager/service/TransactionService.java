// src/main/java/com/moneymanager/service/TransactionService.java
package com.moneymanager.service;

import com.moneymanager.dto.transaction.TransactionDTO;
import com.moneymanager.dto.transaction.TransactionRequest;
import com.moneymanager.dto.transaction.TransactionSummary;
import com.moneymanager.model.Account;
import com.moneymanager.model.Category;
import com.moneymanager.model.Transaction;
import com.moneymanager.model.User;
import com.moneymanager.repository.AccountRepository;
import com.moneymanager.repository.CategoryRepository;
import com.moneymanager.repository.TransactionRepository;
import com.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    // ========== CREATE TRANSACTION WITH ACCOUNT & CATEGORY ==========

    public TransactionDTO createTransaction(String userEmail, TransactionRequest request) {
        // 1. Get user
        User user = getUserByEmail(userEmail);

        // 2. Validate basic request
        validateTransactionRequest(request);

        // 3. Get account (if provided)
        Account account = null;
        if (request.accountId() != null) {
            account = accountRepository.findByIdAndUserId(request.accountId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found or unauthorized"));
        }

        // 4. Get category (if provided)
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found or unauthorized"));
        }

        // 5. Validate category type matches transaction type
        if (category != null) {
            Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(request.type().toUpperCase());
            Category.Type categoryType = category.getType();

            if ((transactionType == Transaction.TransactionType.INCOME && categoryType != Category.Type.INCOME) ||
                    (transactionType == Transaction.TransactionType.EXPENSE && categoryType != Category.Type.EXPENSE)) {
                throw new RuntimeException("Category type does not match transaction type");
            }
        }

        // 6. Create transaction
        Transaction transaction = Transaction.builder()
                .title(request.title())
                .description(request.description())
                .amount(request.amount())
                .type(Transaction.TransactionType.valueOf(request.type().toUpperCase()))
                .transactionDate(request.transactionDate())
                .user(user)
                .account(account)
                .category(category)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 7. Update account balance if account is linked
        if (account != null) {
            updateAccountBalance(account, savedTransaction);
        }

        return convertToDTO(savedTransaction);
    }

    // ========== UPDATE TRANSACTION WITH ACCOUNT & CATEGORY ==========

    public TransactionDTO updateTransaction(Long transactionId, String userEmail, TransactionRequest request) {
        // 1. Get user and existing transaction
        User user = getUserByEmail(userEmail);
        Transaction transaction = getTransactionByIdAndUserId(transactionId, user.getId());

        // 2. Validate request
        validateTransactionRequest(request);

        // 3. Store old values for balance adjustment
        Account oldAccount = transaction.getAccount();
        BigDecimal oldAmount = transaction.getAmount();
        Transaction.TransactionType oldType = transaction.getType();

        // 4. Get new account (if provided)
        Account newAccount = null;
        if (request.accountId() != null) {
            newAccount = accountRepository.findByIdAndUserId(request.accountId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found or unauthorized"));
        }

        // 5. Get new category (if provided)
        Category newCategory = null;
        if (request.categoryId() != null) {
            newCategory = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found or unauthorized"));

            // Validate category type matches transaction type
            Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(request.type().toUpperCase());
            Category.Type categoryType = newCategory.getType();

            if ((transactionType == Transaction.TransactionType.INCOME && categoryType != Category.Type.INCOME) ||
                    (transactionType == Transaction.TransactionType.EXPENSE && categoryType != Category.Type.EXPENSE)) {
                throw new RuntimeException("Category type does not match transaction type");
            }
        }

        // 6. Update transaction fields
        transaction.setTitle(request.title());
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setType(Transaction.TransactionType.valueOf(request.type().toUpperCase()));
        transaction.setTransactionDate(request.transactionDate());
        transaction.setAccount(newAccount);
        transaction.setCategory(newCategory);

        Transaction updatedTransaction = transactionRepository.save(transaction);

        // 7. Handle account balance updates
        if (oldAccount != null) {
            // Reverse old transaction effect
            reverseAccountBalance(oldAccount, oldAmount, oldType);
        }

        if (newAccount != null) {
            // Apply new transaction effect
            updateAccountBalance(newAccount, updatedTransaction);
        }

        return convertToDTO(updatedTransaction);
    }

    // ========== DELETE TRANSACTION WITH BALANCE UPDATE ==========

    public void deleteTransaction(Long transactionId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = getTransactionByIdAndUserId(transactionId, user.getId());

        // Update account balance before deletion if account is linked
        if (transaction.getAccount() != null) {
            reverseAccountBalance(transaction.getAccount(), transaction.getAmount(), transaction.getType());
        }

        transactionRepository.delete(transaction);
    }

    // ========== GET TRANSACTIONS WITH FILTERS ==========

    public List<TransactionDTO> getTransactionsByUser(String userEmail) {
        User user = getUserByEmail(userEmail);
        return transactionRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByAccount(String userEmail, Long accountId) {
        User user = getUserByEmail(userEmail);

        // Verify account belongs to user
        accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return transactionRepository.findByUserId(user.getId())
                .stream()
                .filter(t -> t.getAccount() != null && t.getAccount().getId().equals(accountId))
                .map(this::convertToDTO)
                .toList();
    }

    public List<TransactionDTO> getTransactionsByCategory(String userEmail, Long categoryId) {
        User user = getUserByEmail(userEmail);

        // Verify category belongs to user
        categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return transactionRepository.findByUserId(user.getId())
                .stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsWithFilters(
            String userEmail,
            Transaction.TransactionType type,
            LocalDate startDate,
            LocalDate endDate,
            Long accountId,
            Long categoryId) {

        User user = getUserByEmail(userEmail);
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());

        // Apply filters
        return transactions.stream()
                .filter(t -> type != null && t.getType() == type)
                .filter(t -> startDate != null && !t.getTransactionDate().isBefore(startDate))
                .filter(t -> endDate != null && !t.getTransactionDate().isAfter(endDate))
                .filter(t -> accountId != null && (t.getAccount() != null && t.getAccount().getId().equals(accountId)))
                .filter(t -> categoryId != null && (t.getCategory() != null && t.getCategory().getId().equals(categoryId)))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========== BALANCE CALCULATIONS ==========

    public BigDecimal getBalance(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());

        if (transactions.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalIncome = calculateTotalByType(transactions, Transaction.TransactionType.INCOME);
        BigDecimal totalExpense = calculateTotalByType(transactions, Transaction.TransactionType.EXPENSE);

        return totalIncome.subtract(totalExpense).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> getAccountBalanceSummary(String userEmail, Long accountId) {
        User user = getUserByEmail(userEmail);

        // Verify account belongs to user
        Account account = accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        List<Transaction> accountTransactions = transactionRepository.findByUserId(user.getId())
                .stream()
                .filter(t -> t.getAccount() != null && t.getAccount().getId().equals(accountId))
                .collect(Collectors.toList());

        BigDecimal accountIncome = calculateTotalByType(accountTransactions, Transaction.TransactionType.INCOME);
        BigDecimal accountExpense = calculateTotalByType(accountTransactions, Transaction.TransactionType.EXPENSE);
        BigDecimal accountNet = accountIncome.subtract(accountExpense);

        return Map.of(
                "accountId", accountId,
                "accountName", account.getName(),
                "currentBalance", account.getBalance(),
                "calculatedIncome", accountIncome.setScale(2, RoundingMode.HALF_UP),
                "calculatedExpense", accountExpense.setScale(2, RoundingMode.HALF_UP),
                "calculatedNet", accountNet.setScale(2, RoundingMode.HALF_UP),
                "transactionCount", accountTransactions.size(),
                "balanceMatch", account.getBalance().compareTo(accountNet) == 0 ? "MATCH" : "MISMATCH"
        );
    }

    public Map<String, Object> getCategorySpending(String userEmail, Long categoryId) {
        User user = getUserByEmail(userEmail);

        // Verify category belongs to user
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<Transaction> categoryTransactions = transactionRepository.findByUserId(user.getId())
                .stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .toList();

        BigDecimal totalSpent = categoryTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "categoryId", categoryId,
                "categoryName", category.getName(),
                "totalSpent", totalSpent.setScale(2, RoundingMode.HALF_UP),
                "transactionCount", categoryTransactions.size(),
                "averageTransaction", categoryTransactions.isEmpty() ? BigDecimal.ZERO :
                        totalSpent.divide(BigDecimal.valueOf(categoryTransactions.size()), 2, RoundingMode.HALF_UP),
                "transactions", categoryTransactions.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    // ========== HELPER METHODS ==========

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private Transaction getTransactionByIdAndUserId(Long transactionId, Long userId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new RuntimeException("Transaction not found or unauthorized"));
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (!request.type().equalsIgnoreCase("INCOME") && !request.type().equalsIgnoreCase("EXPENSE")) {
            throw new RuntimeException("Type must be INCOME or EXPENSE");
        }

        if (request.transactionDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new RuntimeException("Transaction date cannot be in the future");
        }
    }

    private void updateAccountBalance(Account account, Transaction transaction) {
        if (transaction.getType() == Transaction.TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        }
        accountRepository.save(account);
    }

    private void reverseAccountBalance(Account account, BigDecimal amount, Transaction.TransactionType type) {
        if (type == Transaction.TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    private BigDecimal calculateTotalByType(List<Transaction> transactions, Transaction.TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getTitle(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getTransactionDate(),
                transaction.getUser().getId(),
                transaction.getAccount() != null ? transaction.getAccount().getId() : null,
                transaction.getCategory() != null ? transaction.getCategory().getId() : null,
                transaction.getCreatedAt() != null ? transaction.getCreatedAt().toLocalDate() : null
        );
    }

    public TransactionDTO getTransactionById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Transaction transaction = getTransactionByIdAndUserId(id, user.getId());
        return convertToDTO(transaction);
    }

    public List<TransactionDTO> getRecentTransactions(String userEmail, int limit) {
        User user = getUserByEmail(userEmail);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("transactionDate").descending());

        return transactionRepository.findByUserId(user.getId(), pageable)
                .stream()
                .map(this::convertToDTO)
                .toList(); // Use .collect(Collectors.toList()) if on Java < 16
    }

    public Map<String, BigDecimal> getBalanceDetails(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());

        BigDecimal totalIncome = calculateTotalByType(transactions, Transaction.TransactionType.INCOME);
        BigDecimal totalExpense = calculateTotalByType(transactions, Transaction.TransactionType.EXPENSE);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        return Map.of(
                "totalIncome", totalIncome.setScale(2, RoundingMode.HALF_UP),
                "totalExpense", totalExpense.setScale(2, RoundingMode.HALF_UP),
                "balance", balance.setScale(2, RoundingMode.HALF_UP),
                "transactionCount", BigDecimal.valueOf(transactionRepository.countByUserId(user.getId())),
                "incomeTransactionCount", BigDecimal.valueOf(
                        transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.INCOME).count()),
                "expenseTransactionCount", BigDecimal.valueOf(
                        transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.EXPENSE).count())
        );
    }

    public TransactionSummary getTransactionSummary(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = getUserByEmail(userEmail);

        if (startDate == null || endDate == null) {
            // Default to current month
            YearMonth currentMonth = YearMonth.now();
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }

        if (startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), startDate, endDate);

        BigDecimal totalIncome = calculateTotalByType(transactions, Transaction.TransactionType.INCOME);
        BigDecimal totalExpense = calculateTotalByType(transactions, Transaction.TransactionType.EXPENSE);
        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        // Calculate average transaction amounts
        long incomeCount = transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.INCOME).count();
        long expenseCount = transactions.stream().filter(t -> t.getType() == Transaction.TransactionType.EXPENSE).count();

        BigDecimal avgIncome = incomeCount > 0 ?
                totalIncome.divide(BigDecimal.valueOf(incomeCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal avgExpense = expenseCount > 0 ?
                totalExpense.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Get top categories
        Map<String, BigDecimal> topIncomeCategories = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, BigDecimal> topExpenseCategories = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new TransactionSummary(
                totalIncome.setScale(2, RoundingMode.HALF_UP),
                totalExpense.setScale(2, RoundingMode.HALF_UP),
                netAmount.setScale(2, RoundingMode.HALF_UP),
                startDate,
                endDate,
                (int) incomeCount,
                (int) expenseCount,
                avgIncome,
                avgExpense,
                topIncomeCategories,
                topExpenseCategories
        );
    }
}