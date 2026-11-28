-- ---------------------------------------------------------------
-- subscriptions: recurring expense definitions
-- ---------------------------------------------------------------
CREATE TABLE subscriptions (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    category_id    BIGINT         NOT NULL,
    amount         DECIMAL(12,2)  NOT NULL,
    description     VARCHAR(255)   NULL,
    frequency       VARCHAR(20)    NOT NULL,
    start_date     DATE           NOT NULL,
    next_due_date  DATE           NOT NULL,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_subscriptions_amount CHECK (amount > 0),
    CONSTRAINT chk_subscriptions_frequency
        CHECK (frequency IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_subscriptions_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT,
    KEY idx_subscriptions_user (user_id),
    KEY idx_subscriptions_due_active (active, next_due_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- subscription_occurrences: tracks generated expense instances
-- unique subscription + scheduled date prevents duplicate generation
-- ---------------------------------------------------------------
CREATE TABLE subscription_occurrences (
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    subscription_id   BIGINT   NOT NULL,
    scheduled_date    DATE     NOT NULL,
    expense_id        BIGINT   NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_subscription_occurrence (subscription_id, scheduled_date),
    UNIQUE KEY uq_occurrence_expense (expense_id),
    CONSTRAINT fk_occurrences_subscription
        FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_occurrences_expense
        FOREIGN KEY (expense_id) REFERENCES expenses (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;