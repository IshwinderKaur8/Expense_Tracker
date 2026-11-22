package com.ishwinder.expense_tracker.budget;

import com.ishwinder.expense_tracker.budget.dto.BudgetRequest;
import com.ishwinder.expense_tracker.budget.dto.BudgetResponse;
import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import com.ishwinder.expense_tracker.common.Months;
import com.ishwinder.expense_tracker.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository repository;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUser;

    public BudgetService(BudgetRepository repository, CategoryService categoryService,
                         CurrentUserProvider currentUser) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.currentUser = currentUser;
    }

    /** Creates the budget, or replaces the amount if one already exists (idempotent). */
    @Transactional
    public BudgetResponse set(BudgetRequest request) {
        LocalDate monthStart = Months.parse(request.month()).atDay(1);
        Long userId = currentUser.currentUserId();

        String categoryName = null;
        if (request.categoryId() != null) {
            categoryName = categoryService.getOwned(request.categoryId()).name();   // 404 if not ours
        }

        Optional<Budget> existing = request.categoryId() == null
                ? repository.findByUserIdAndBudgetMonthAndCategoryIdIsNull(userId, monthStart)
                : repository.findByUserIdAndBudgetMonthAndCategoryId(
                        userId, monthStart, request.categoryId());

        BigDecimal amount = request.amount().setScale(2);
        Budget budget = existing.orElseGet(
                () -> new Budget(userId, request.categoryId(), monthStart, amount));
        budget.setAmount(amount);

        return BudgetResponse.from(repository.save(budget), categoryName);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(String month) {
        LocalDate monthStart = Months.parse(month).atDay(1);
        List<Budget> budgets = repository.findByUserIdAndBudgetMonth(
                currentUser.currentUserId(), monthStart);

        Set<Long> categoryIds = budgets.stream()
                .map(Budget::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, CategoryResponse> categories = categoryService.findByIds(categoryIds);

        // overall budget first, then per-category budgets alphabetically
        return budgets.stream()
                .map(b -> BudgetResponse.from(b, nameOf(categories, b.getCategoryId())))
                .sorted(Comparator
                        .comparing((BudgetResponse r) -> r.categoryId() != null)
                        .thenComparing(r -> r.categoryName() == null ? "" : r.categoryName()))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Budget budget = repository.findByIdAndUserId(id, currentUser.currentUserId())
                .orElseThrow(() -> new NotFoundException("Budget " + id + " not found"));
        repository.delete(budget);
    }

    private String nameOf(Map<Long, CategoryResponse> categories, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        CategoryResponse category = categories.get(categoryId);
        return category != null ? category.name() : "Unknown";
    }
}