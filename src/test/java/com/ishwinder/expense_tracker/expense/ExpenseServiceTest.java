package com.ishwinder.expense_tracker.expense;

import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.BadRequestException;
import com.ishwinder.expense_tracker.common.NotFoundException;
import com.ishwinder.expense_tracker.expense.dto.ExpenseRequest;
import com.ishwinder.expense_tracker.expense.dto.ExpenseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ExpenseRepository repository;
    @Mock
    private CategoryService categoryService;

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(repository, categoryService, () -> USER_ID);
    }

    @Test
    void create_savesExpenseWithTwoDecimalsAndTrimmedDescription() {
        when(categoryService.getOwned(3L)).thenReturn(new CategoryResponse(3L, "Food", null));
        when(repository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = service.create(new ExpenseRequest(
                new BigDecimal("249.5"), 3L, LocalDate.of(2026, 10, 31), "  Lunch  "));

        ArgumentCaptor<Expense> saved = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("249.50");
        assertThat(saved.getValue().getAmount().scale()).isEqualTo(2);
        assertThat(saved.getValue().getDescription()).isEqualTo("Lunch");
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.categoryName()).isEqualTo("Food");
    }

    @Test
    void create_throwsNotFound_andSavesNothing_whenCategoryIsNotOwned() {
        when(categoryService.getOwned(99L)).thenThrow(new NotFoundException("Category 99 not found"));

        assertThatThrownBy(() -> service.create(new ExpenseRequest(
                BigDecimal.TEN, 99L, LocalDate.of(2026, 10, 31), null)))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void list_throwsBadRequest_whenFromIsAfterTo() {
        assertThatThrownBy(() -> service.list(
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1), null, 0, 20))
                .isInstanceOf(BadRequestException.class);

        verify(repository, never()).search(any(), any(), any(), any(), any());
    }

    @Test
    void list_clampsPageSizeToMaximum() {
        when(repository.search(eq(USER_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(null, null, null, 0, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq(USER_ID), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }
}