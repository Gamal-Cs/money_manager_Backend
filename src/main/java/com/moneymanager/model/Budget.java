// src/main/java/com/moneymanager/model/Budget.java
package com.moneymanager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean active = true;

    public enum Period {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY,
        CUSTOM
    }

    // Helper method to calculate remaining amount
    public BigDecimal getRemainingAmount() {
        return amount.subtract(spentAmount);
    }

    // Helper method to calculate percentage used
    public double getPercentageUsed() {
        if (amount.compareTo(BigDecimal.ZERO) == 0) return 0;
        return spentAmount.divide(amount, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
    }

    // Helper method to check if budget is over
    public boolean isOverBudget() {
        return spentAmount.compareTo(amount) > 0;
    }
}
