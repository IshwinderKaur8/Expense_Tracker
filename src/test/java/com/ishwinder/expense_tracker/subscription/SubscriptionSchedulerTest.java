package com.ishwinder.expense_tracker.subscription;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SubscriptionSchedulerTest {

    @Test
    void schedulerInvokesProcessingService() {
        SubscriptionProcessingService processingService =
                mock(SubscriptionProcessingService.class);

        SubscriptionScheduler scheduler =
                new SubscriptionScheduler(processingService);

        scheduler.processSubscriptionsDaily();

        verify(processingService).processDueSubscriptions(
                java.time.LocalDate.now());
    }
}