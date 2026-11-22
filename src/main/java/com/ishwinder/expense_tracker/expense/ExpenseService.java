package com.ishwinder.expense_tracker.expense;

import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.BadRequestException;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import com.ishwinder.expense_tracker.common.NotFoundException;
import com.ishwinder.expense_tracker.common.PageResponse;
import com.ishwinder.expense_tracker.expense.dto.ExpenseRequest;
import com.ishwinder.expense_tracker.expense.dto.ExpenseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseRepository repository;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUser;

    public ExpenseService(ExpenseRepository repository, CategoryService categoryService,
                          CurrentUserProvider currentUser) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.currentUser = currentUser;
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        CategoryResponse category = categoryService.getOwned(request.categoryId());
        Expense saved = repository.save(new Expense(
                currentUser.currentUserId(),
                category.id(),
                request.amount().setScale(2),      // @Digits guarantees no rounding is needed
                request.expenseDate(),
                cleanDescription(request.description())));
        return ExpenseResponse.from(saved, category.name());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id) {
        Expense expense = findOwned(id);
        CategoryResponse category = categoryService.getOwned(expense.getCategoryId());
        return ExpenseResponse.from(expense, category.name());
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(LocalDate from, LocalDate to, Long categoryId,
                                              int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Expense> result = repository.search(
                currentUser.currentUserId(), from, to, categoryId, pageable);

        // one batch lookup for all category names on this page (avoids N+1 queries)
        Set<Long> categoryIds = result.getContent().stream()
                .map(Expense::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, CategoryResponse> categories = categoryService.findByIds(categoryIds);

        List<ExpenseResponse> items = result.getContent().stream()
                .map(e -> ExpenseResponse.from(e, nameOf(categories, e.getCategoryId())))
                .toList();

        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findOwned(id);
        CategoryResponse category = categoryService.getOwned(request.categoryId());

        expense.setCategoryId(category.id());
        expense.setAmount(request.amount().setScale(2));
        expense.setExpenseDate(request.expenseDate());
        expense.setDescription(cleanDescription(request.description()));
        return ExpenseResponse.from(expense, category.name());
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findOwned(id));
    }

    private Expense findOwned(Long id) {
        return repository.findByIdAndUserId(id, currentUser.currentUserId())
                .orElseThrow(() -> new NotFoundException("Expense " + id + " not found"));
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private String nameOf(Map<Long, CategoryResponse> categories, Long categoryId) {
        CategoryResponse category = categories.get(categoryId);
        return category != null ? category.name() : "Unknown";
    }
}