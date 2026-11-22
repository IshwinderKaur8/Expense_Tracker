package com.ishwinder.expense_tracker.budget;

import com.ishwinder.expense_tracker.budget.dto.BudgetRequest;
import com.ishwinder.expense_tracker.budget.dto.BudgetResponse;
import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.BadRequestException;
import com.ishwinder.expense_tracker.common.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate OCT_2026 = LocalDate.of(2026, 10, 1);

    @Mock
    private BudgetRepository repository;
    @Mock
    private CategoryService categoryService;

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(repository, categoryService, () -> USER_ID);
    }

    @Test
    void set_createsOverallBudget_whenNoneExists() {
        when(repository.findByUserIdAndBudgetMonthAndCategoryIdIsNull(USER_ID, OCT_2026))
                .thenReturn(Optional.empty());
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        service.set(new BudgetRequest("2026-10", null, new BigDecimal("25000")));

        ArgumentCaptor<Budget> saved = ArgumentCaptor.forClass(Budget.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCategoryId()).isNull();
        assertThat(saved.getValue().getBudgetMonth()).isEqualTo(OCT_2026);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("25000.00");
        verifyNoInteractions(categoryService);       // overall budget needs no category check
    }

    @Test
    void set_replacesAmount_whenBudgetAlreadyExists() {
        Budget existing = new Budget(USER_ID, 3L, OCT_2026, new BigDecimal("5000.00"));
        when(categoryService.getOwned(3L)).thenReturn(new CategoryResponse(3L, "Food", null));
        when(repository.findByUserIdAndBudgetMonthAndCategoryId(USER_ID, OCT_2026, 3L))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

        BudgetResponse response = service.set(new BudgetRequest("2026-10", 3L, new BigDecimal("8000")));

        verify(repository).save(existing);           // the same row is updated, no duplicate
        assertThat(existing.getAmount()).isEqualByComparingTo("8000.00");
        assertThat(response.categoryName()).isEqualTo("Food");
    }

    @Test
    void set_throwsNotFound_whenCategoryIsNotOwned() {
        when(categoryService.getOwned(99L)).thenThrow(new NotFoundException("Category 99 not found"));

        assertThatThrownBy(() -> service.set(new BudgetRequest("2026-10", 99L, BigDecimal.TEN)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void set_throwsBadRequest_whenMonthIsInvalid() {
        assertThatThrownBy(() -> service.set(new BudgetRequest("2026-13", null, BigDecimal.TEN)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void list_putsOverallFirst_thenCategoriesAlphabetically() {
        Budget travel = new Budget(USER_ID, 5L, OCT_2026, new BigDecimal("3000.00"));
        Budget overall = new Budget(USER_ID, null, OCT_2026, new BigDecimal("25000.00"));
        Budget food = new Budget(USER_ID, 3L, OCT_2026, new BigDecimal("8000.00"));
        when(repository.findByUserIdAndBudgetMonth(USER_ID, OCT_2026))
                .thenReturn(List.of(travel, overall, food));
        when(categoryService.findByIds(any())).thenReturn(Map.of(
                3L, new CategoryResponse(3L, "Food", null),
                5L, new CategoryResponse(5L, "Travel", null)));

        List<BudgetResponse> result = service.list("2026-10");

        assertThat(result).extracting(BudgetResponse::categoryName)
                .containsExactly(null, "Food", "Travel");
    }
}