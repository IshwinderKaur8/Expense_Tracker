-- ---------------------------------------------------------------
-- users: exists from day one so multi-user needs no schema change
-- ---------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- never store plain passwords
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- categories: each user owns their own list (US-2)
-- ---------------------------------------------------------------
CREATE TABLE categories (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    name       VARCHAR(50) NOT NULL,
    color      CHAR(7)     NULL,                  -- e.g. '#FF8800', used by charts
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_user_name (user_id, name),
    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- expenses: the core table (US-1)
-- ---------------------------------------------------------------
CREATE TABLE expenses (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    category_id  BIGINT        NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,          -- money is never FLOAT/DOUBLE
    expense_date DATE          NOT NULL,          -- the day it was spent
    description  VARCHAR(255)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_expenses_amount CHECK (amount > 0),
    CONSTRAINT fk_expenses_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT,                       -- cannot delete a category in use
    -- reports always filter by user + date range, so index exactly that
    KEY idx_expenses_user_date (user_id, expense_date),
    KEY idx_expenses_user_cat_date (user_id, category_id, expense_date)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------
-- budgets: monthly limits (US-3, later US-7)
-- category_id NULL means the overall budget for that month
-- budget_month always stores the 1st day of the month
-- NOTE: MySQL treats NULLs as distinct in UNIQUE keys, so "one overall
-- budget per month" is enforced in the service layer, not here.
-- ---------------------------------------------------------------
CREATE TABLE budgets (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    category_id  BIGINT        NULL,
    budget_month DATE          NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_budgets_amount CHECK (amount > 0),
    CONSTRAINT chk_budgets_first_day CHECK (DAY(budget_month) = 1),
    CONSTRAINT fk_budgets_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE CASCADE,
    UNIQUE KEY uq_budgets_user_cat_month (user_id, category_id, budget_month)
) ENGINE=InnoDB;
