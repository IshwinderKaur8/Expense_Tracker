package com.ishwinder.expense_tracker.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    List<Subscription> findAllByUserIdOrderByNextDueDateAsc(Long userId);

    List<Subscription> findAllByUserIdAndActiveOrderByNextDueDateAsc(
            Long userId, boolean active);

    List<Subscription> findAllByActiveTrueAndNextDueDateLessThanEqual(
            LocalDate date);
}