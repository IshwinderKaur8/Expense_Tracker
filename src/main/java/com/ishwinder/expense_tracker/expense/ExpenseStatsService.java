package com.ishwinder.expense_tracker.expense;

import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import com.ishwinder.expense_tracker.expense.dto.CategoryTotal;
import com.ishwinder.expense_tracker.expense.dto.DailyTotal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Read-only statistics over the current user's expenses (used by reports). */
@Service
public class ExpenseStatsService {

    private final ExpenseRepository repository;
    private final CurrentUserProvider currentUser;

    public ExpenseStatsService(ExpenseRepository repository, CurrentUserProvider currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /** Both dates are inclusive. */
    @Transactional(readOnly = true)
    public List<CategoryTotal> totalsByCategory(LocalDate from, LocalDate to) {
        return repository.totalsByCategory(currentUser.currentUserId(), from, to);
    }

    /** Both dates are inclusive. Only days that have spending are returned. */
    @Transactional(readOnly = true)
    public List<DailyTotal> dailyTotals(LocalDate from, LocalDate to) {
        return repository.dailyTotals(currentUser.currentUserId(), from, to);
    }
}