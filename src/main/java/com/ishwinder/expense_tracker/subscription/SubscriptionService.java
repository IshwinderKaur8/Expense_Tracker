package com.ishwinder.expense_tracker.subscription;

import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            CategoryService categoryService,
            CurrentUserProvider currentUserProvider) {
        this.subscriptionRepository = subscriptionRepository;
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    public Subscription create(
            Long categoryId,
            BigDecimal amount,
            String description,
            RecurrenceFrequency frequency,
            LocalDate startDate) {

        validate(amount, description, frequency, startDate);
        categoryService.getOwned(categoryId);

        LocalDate nextDueDate = startDate;

        Subscription subscription = new Subscription(
                currentUserProvider.currentUserId(),
                categoryId,
                amount,
                description.trim(),
                frequency,
                startDate,
                nextDueDate
        );

        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getAll() {
        return subscriptionRepository.findAllByUserIdOrderByNextDueDateAsc(
                currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActive() {
        return subscriptionRepository.findAllByUserIdAndActiveOrderByNextDueDateAsc(
                currentUserProvider.currentUserId(), true);
    }

    @Transactional(readOnly = true)
    public Subscription getById(Long id) {
        return subscriptionRepository.findByIdAndUserId(
                id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found."));
    }

    public Subscription update(
            Long id,
            Long categoryId,
            BigDecimal amount,
            String description,
            RecurrenceFrequency frequency) {

        validate(amount, description, frequency, null);
        categoryService.getOwned(categoryId);

        Subscription subscription = getById(id);

        subscription.setCategoryId(categoryId);
        subscription.setAmount(amount);
        subscription.setDescription(description.trim());
        subscription.setFrequency(frequency);

        return subscriptionRepository.save(subscription);
    }

    public Subscription pause(Long id) {
        Subscription subscription = getById(id);
        subscription.setActive(false);
        return subscriptionRepository.save(subscription);
    }

    public Subscription resume(Long id) {
        Subscription subscription = getById(id);
        subscription.setActive(true);
        return subscriptionRepository.save(subscription);
    }

    public void delete(Long id) {
        Subscription subscription = getById(id);
        subscriptionRepository.delete(subscription);
    }

    private void validate(
            BigDecimal amount,
            String description,
            RecurrenceFrequency frequency,
            LocalDate startDate) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required.");
        }

        if (description.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 255 characters.");
        }

        if (frequency == null) {
            throw new IllegalArgumentException("Frequency is required.");
        }

        if (startDate != null && startDate.isAfter(LocalDate.now())) {
            // Future start dates are allowed.
        }
    }
}
