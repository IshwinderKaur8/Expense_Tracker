package com.ishwinder.expense_tracker.budget;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "category_id", updatable = false)      // null = overall budget
    private Long categoryId;

    @Column(name = "budget_month", nullable = false, updatable = false)
    private LocalDate budgetMonth;                         // always the 1st of the month

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Budget() { }

    public Budget(Long userId, Long categoryId, LocalDate budgetMonth, BigDecimal amount) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.budgetMonth = budgetMonth;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCategoryId() { return categoryId; }
    public LocalDate getBudgetMonth() { return budgetMonth; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}