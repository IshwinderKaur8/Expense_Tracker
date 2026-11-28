
package com.ishwinder.expense_tracker.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionOccurrenceRepository
        extends JpaRepository<SubscriptionOccurrence, Long> {

    boolean existsBySubscription_IdAndScheduledDate(
            Long subscriptionId, LocalDate scheduledDate);

    Optional<SubscriptionOccurrence> findBySubscription_IdAndScheduledDate(
            Long subscriptionId, LocalDate scheduledDate);

    List<SubscriptionOccurrence> findAllBySubscription_IdOrderByScheduledDateAsc(
            Long subscriptionId);
}
