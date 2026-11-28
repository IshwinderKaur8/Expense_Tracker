
package com.ishwinder.expense_tracker.subscription;

import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;

import com.ishwinder.expense_tracker.subscription.RecurrenceFrequency;
import com.ishwinder.expense_tracker.subscription.SubscriptionRepository;
import com.ishwinder.expense_tracker.subscription.SubscriptionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Long userId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        categoryId = 10L;
    }

    @Test
    void createShouldSaveSubscription() {
        LocalDate startDate = LocalDate.now();
        BigDecimal amount = new BigDecimal("499.00");
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        Subscription subscription = new Subscription(
                userId, categoryId, amount, "Netflix",
                RecurrenceFrequency.MONTHLY, startDate, startDate
        );

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(subscription);

        Subscription result = subscriptionService.create(
                categoryId, amount, "Netflix",
                RecurrenceFrequency.MONTHLY, startDate
        );

        assertNotNull(result);
        assertEquals("Netflix", result.getDescription());
        assertEquals(amount, result.getAmount());
        assertEquals(RecurrenceFrequency.MONTHLY, result.getFrequency());

        verify(categoryService).getOwned(categoryId);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void createShouldRejectInvalidAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                subscriptionService.create(
                        categoryId, BigDecimal.ZERO, "Netflix",
                        RecurrenceFrequency.MONTHLY, LocalDate.now()
                ));

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void createShouldRejectBlankDescription() {
        assertThrows(IllegalArgumentException.class, () ->
                subscriptionService.create(
                        categoryId, BigDecimal.TEN, "  ",
                        RecurrenceFrequency.MONTHLY, LocalDate.now()
                ));

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void getByIdShouldReturnOwnedSubscription() {
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        Subscription subscription = new Subscription(
                userId, categoryId, BigDecimal.TEN, "Test",
                RecurrenceFrequency.MONTHLY,
                LocalDate.now(), LocalDate.now()
        );

        when(subscriptionRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(subscription));

        Subscription result = subscriptionService.getById(5L);

        assertNotNull(result);
        assertEquals("Test", result.getDescription());
    }

    @Test
    void getByIdShouldRejectSubscriptionNotOwned() {
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        when(subscriptionRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                subscriptionService.getById(5L));
    }

    @Test
    void pauseShouldDeactivateSubscription() {
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        Subscription subscription = new Subscription(
                userId, categoryId, BigDecimal.TEN, "Test",
                RecurrenceFrequency.MONTHLY,
                LocalDate.now(), LocalDate.now()
        );

        when(subscriptionRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(subscription))
                .thenReturn(subscription);

        Subscription result = subscriptionService.pause(5L);

        assertFalse(result.isActive());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void resumeShouldActivateSubscription() {
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        Subscription subscription = new Subscription(
                userId, categoryId, BigDecimal.TEN, "Test",
                RecurrenceFrequency.MONTHLY,
                LocalDate.now(), LocalDate.now()
        );

        subscription.setActive(false);

        when(subscriptionRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(subscription))
                .thenReturn(subscription);

        Subscription result = subscriptionService.resume(5L);

        assertTrue(result.isActive());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void deleteShouldDeleteOwnedSubscription() {
        when(currentUserProvider.currentUserId()).thenReturn(userId);
        Subscription subscription = new Subscription(
                userId, categoryId, BigDecimal.TEN, "Test",
                RecurrenceFrequency.MONTHLY,
                LocalDate.now(), LocalDate.now()
        );

        when(subscriptionRepository.findByIdAndUserId(5L, userId))
                .thenReturn(Optional.of(subscription));

        subscriptionService.delete(5L);

        verify(subscriptionRepository).delete(subscription);
    }
}
