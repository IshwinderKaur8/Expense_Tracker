package com.ishwinder.expense_tracker.subscription;

import com.ishwinder.expense_tracker.expense.Expense;
import com.ishwinder.expense_tracker.expense.ExpenseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionProcessingService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOccurrenceRepository occurrenceRepository;
    private final ExpenseService expenseService;

    public SubscriptionProcessingService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionOccurrenceRepository occurrenceRepository,
            ExpenseService expenseService) {
        this.subscriptionRepository = subscriptionRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.expenseService = expenseService;
    }

    @Transactional
    public int processDueSubscriptions(LocalDate today) {
        List<Subscription> dueSubscriptions =
                subscriptionRepository.findAllByActiveTrueAndNextDueDateLessThanEqual(today);

        int processedCount = 0;

        for (Subscription subscription : dueSubscriptions) {
            while (subscription.isActive()
                    && !subscription.getNextDueDate().isAfter(today)) {

                LocalDate scheduledDate = subscription.getNextDueDate();

                if (!occurrenceRepository.existsBySubscription_IdAndScheduledDate(
                        subscription.getId(), scheduledDate)) {

                    Expense expense = expenseService.createFromSubscription(
                            subscription.getUserId(),
                            subscription.getCategoryId(),
                            subscription.getAmount(),
                            scheduledDate,
                            subscription.getDescription()
                    );

                    occurrenceRepository.save(
                            new SubscriptionOccurrence(subscription, scheduledDate, expense)
                    );

                    processedCount++;
                }

                subscription.setNextDueDate(
                        calculateNextDate(scheduledDate, subscription.getFrequency())
                );
            }
        }

        return processedCount;
    }

    private LocalDate calculateNextDate(
            LocalDate currentDate,
            RecurrenceFrequency frequency) {

        return switch (frequency) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case YEARLY -> currentDate.plusYears(1);
        };
    }
}