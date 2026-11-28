
package com.ishwinder.expense_tracker.subscription;

import com.ishwinder.expense_tracker.expense.Expense;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "subscription_occurrences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_scheduled_date",
                        columnNames = {"subscription_id", "scheduled_date"}
                )
        }
)
public class SubscriptionOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", unique = true)
    private Expense expense;

    protected SubscriptionOccurrence() {
    }

    public SubscriptionOccurrence(
            Subscription subscription,
            LocalDate scheduledDate,
            Expense expense) {
        this.subscription = subscription;
        this.scheduledDate = scheduledDate;
        this.expense = expense;
    }

    public Long getId() {
        return id;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }
}
