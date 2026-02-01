// src/main/java/com/moneymanager/repository/BudgetRepository.java
package com.moneymanager.repository;

import com.moneymanager.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserIdAndActiveTrue(Long userId);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    List<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.active = true " +
            "AND b.startDate <= :date AND b.endDate >= :date")
    List<Budget> findActiveBudgetsByUserAndDate(@Param("userId") Long userId,
                                                @Param("date") LocalDate date);

    @Query("SELECT SUM(b.spentAmount) FROM Budget b WHERE b.user.id = :userId " +
            "AND b.category.id = :categoryId AND b.startDate <= :date AND b.endDate >= :date")
    Optional<BigDecimal> getTotalSpentByCategoryAndDate(@Param("userId") Long userId,
                                                        @Param("categoryId") Long categoryId,
                                                        @Param("date") LocalDate date);
}