
package com.ishwinder.expense_tracker.subscription;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@RequestBody CreateSubscriptionRequest request) {
        Subscription subscription = subscriptionService.create(
                request.categoryId(),
                request.amount(),
                request.description(),
                request.frequency(),
                request.startDate()
        );

        return toResponse(subscription);
    }

    @GetMapping
    public List<SubscriptionResponse> getAll() {
        return subscriptionService.getAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public SubscriptionResponse getById(@PathVariable Long id) {
        return toResponse(subscriptionService.getById(id));
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(
            @PathVariable Long id,
            @RequestBody UpdateSubscriptionRequest request) {

        Subscription subscription = subscriptionService.update(
                id,
                request.categoryId(),
                request.amount(),
                request.description(),
                request.frequency()
        );

        return toResponse(subscription);
    }

    @PatchMapping("/{id}/pause")
    public SubscriptionResponse pause(@PathVariable Long id) {
        return toResponse(subscriptionService.pause(id));
    }

    @PatchMapping("/{id}/resume")
    public SubscriptionResponse resume(@PathVariable Long id) {
        return toResponse(subscriptionService.resume(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        subscriptionService.delete(id);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCategoryId(),
                subscription.getAmount(),
                subscription.getDescription(),
                subscription.getFrequency(),
                subscription.getStartDate(),
                subscription.getNextDueDate(),
                subscription.isActive()
        );
    }

    public record CreateSubscriptionRequest(
            Long categoryId,
            BigDecimal amount,
            String description,
            RecurrenceFrequency frequency,
            LocalDate startDate
    ) {}

    public record UpdateSubscriptionRequest(
            Long categoryId,
            BigDecimal amount,
            String description,
            RecurrenceFrequency frequency
    ) {}

    public record SubscriptionResponse(
            Long id,
            Long categoryId,
            BigDecimal amount,
            String description,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate nextDueDate,
            boolean active
    ) {}
}
