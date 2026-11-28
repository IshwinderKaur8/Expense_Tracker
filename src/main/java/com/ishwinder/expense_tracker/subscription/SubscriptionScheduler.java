package com.ishwinder.expense_tracker.subscription;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SubscriptionScheduler {

    private final SubscriptionProcessingService processingService;

    public SubscriptionScheduler(SubscriptionProcessingService processingService) {
        this.processingService = processingService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processSubscriptionsDaily() {
        processingService.processDueSubscriptions(LocalDate.now());
    }
}