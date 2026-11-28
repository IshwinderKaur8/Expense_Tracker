package com.ishwinder.expense_tracker.subscription;

import com.ishwinder.expense_tracker.expense.Expense;
import com.ishwinder.expense_tracker.expense.ExpenseService;

import com.ishwinder.expense_tracker.subscription.RecurrenceFrequency;
import com.ishwinder.expense_tracker.subscription.SubscriptionOccurrenceRepository;
import com.ishwinder.expense_tracker.subscription.SubscriptionProcessingService;
import com.ishwinder.expense_tracker.subscription.SubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class SubscriptionProcessingServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionOccurrenceRepository occurrenceRepository;

    @Mock
    private ExpenseService expenseService;

    private SubscriptionProcessingService processingService;

    @BeforeEach
    void setUp() {
        processingService = new SubscriptionProcessingService(
                subscriptionRepository,
                occurrenceRepository,
                expenseService
        );
    }

    private void setSubscriptionId(Subscription subscription, Long id) {
        try {
            Field field = Subscription.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(subscription, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void processDueSubscriptionCreatesExpenseAndOccurrence() {
        LocalDate dueDate = LocalDate.of(2026, 11, 1);

        Subscription subscription = new Subscription(
                1L,
                2L,
                new BigDecimal("499.00"),
                "Streaming",
                RecurrenceFrequency.MONTHLY,
                dueDate,
                dueDate
        );

        setSubscriptionId(subscription, 10L);

        Expense expense = mock(Expense.class);

        when(subscriptionRepository
                .findAllByActiveTrueAndNextDueDateLessThanEqual(dueDate))
                .thenReturn(List.of(subscription));

        when(occurrenceRepository
                .existsBySubscription_IdAndScheduledDate(10L, dueDate))
                .thenReturn(false);

        when(expenseService.createFromSubscription(
                1L, 2L, new BigDecimal("499.00"), dueDate, "Streaming"))
                .thenReturn(expense);

        int count = processingService.processDueSubscriptions(dueDate);

        assertEquals(1, count);

        verify(expenseService).createFromSubscription(
                1L, 2L, new BigDecimal("499.00"), dueDate, "Streaming");

        verify(occurrenceRepository).save(
                argThat(occurrence ->
                        occurrence.getSubscription() == subscription
                                && occurrence.getScheduledDate().equals(dueDate)
                                && occurrence.getExpense() == expense
                )
        );

        assertEquals(LocalDate.of(2026, 12, 1),
                subscription.getNextDueDate());
    }

    @Test
    void processDueSubscriptionSkipsExistingOccurrence() {
        LocalDate dueDate = LocalDate.of(2026, 11, 1);

        Subscription subscription = new Subscription(
                1L,
                2L,
                new BigDecimal("499.00"),
                "Streaming",
                RecurrenceFrequency.MONTHLY,
                dueDate,
                dueDate
        );

        setSubscriptionId(subscription, 10L);

        when(subscriptionRepository
                .findAllByActiveTrueAndNextDueDateLessThanEqual(dueDate))
                .thenReturn(List.of(subscription));

        when(occurrenceRepository
                .existsBySubscription_IdAndScheduledDate(10L, dueDate))
                .thenReturn(true);

        int count = processingService.processDueSubscriptions(dueDate);

        assertEquals(0, count);

        verifyNoInteractions(expenseService);
        verify(occurrenceRepository, never()).save(any());

        assertEquals(LocalDate.of(2026, 12, 1),
                subscription.getNextDueDate());
    }
    @Test
    void processOverdueSubscriptionCreatesAllMissedOccurrences() {
        LocalDate firstDueDate = LocalDate.of(2026, 9, 1);
        LocalDate today = LocalDate.of(2026, 11, 28);

        Subscription subscription = new Subscription(
                1L,
                2L,
                new BigDecimal("499.00"),
                "Streaming",
                RecurrenceFrequency.MONTHLY,
                firstDueDate,
                firstDueDate
        );

        setSubscriptionId(subscription, 10L);

        when(subscriptionRepository
                .findAllByActiveTrueAndNextDueDateLessThanEqual(today))
                .thenReturn(List.of(subscription));

        when(occurrenceRepository
                .existsBySubscription_IdAndScheduledDate(anyLong(), any()))
                .thenReturn(false);

        when(expenseService.createFromSubscription(
                anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(mock(Expense.class));

        int count = processingService.processDueSubscriptions(today);

        assertEquals(3, count);

        verify(expenseService, times(3)).createFromSubscription(
                anyLong(), anyLong(), any(), any(), any());

        assertEquals(LocalDate.of(2026, 12, 1),
                subscription.getNextDueDate());
    }

    @Test
    void monthlyRecurrenceFromMonthEndAdjustsToValidDate() {
        LocalDate januaryEnd = LocalDate.of(2026, 1, 31);

        Subscription subscription = new Subscription(
                1L,
                2L,
                new BigDecimal("499.00"),
                "Streaming",
                RecurrenceFrequency.MONTHLY,
                januaryEnd,
                januaryEnd
        );

        assertEquals(LocalDate.of(2026, 2, 28),
                subscription.getNextDueDate().plusMonths(1));
    }
}