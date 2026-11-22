package com.ishwinder.expense_tracker.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    List<Budget> findByUserIdAndBudgetMonth(Long userId, LocalDate budgetMonth);

    Optional<Budget> findByUserIdAndBudgetMonthAndCategoryId(Long userId, LocalDate budgetMonth,
                                                             Long categoryId);

    // "= NULL" never matches in SQL, so the overall budget needs its own IS NULL query
    Optional<Budget> findByUserIdAndBudgetMonthAndCategoryIdIsNull(Long userId,
                                                                   LocalDate budgetMonth);
}