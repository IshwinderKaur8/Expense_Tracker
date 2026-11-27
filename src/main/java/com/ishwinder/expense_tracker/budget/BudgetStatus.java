package com.ishwinder.expense_tracker.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** How much of a budget has been used. Drives the yellow and red warnings. */
public enum BudgetStatus {
    NONE,        // no budget is set
    OK,          // below the warning threshold
    WARNING,     // at or above 80% of the budget, but not over it
    EXCEEDED;    // spending is above the budget

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal WARNING_PERCENT = BigDecimal.valueOf(80);

    public static BudgetStatus evaluate(BigDecimal spent, BigDecimal budget) {
        if (budget == null || budget.signum() <= 0) {
            return NONE;
        }
        if (spent.compareTo(budget) > 0) {
            return EXCEEDED;
        }
        // exact comparison (spent x 100 >= budget x 80) avoids rounding surprises at the boundary
        boolean nearLimit = spent.multiply(HUNDRED).compareTo(budget.multiply(WARNING_PERCENT)) >= 0;
        return nearLimit ? WARNING : OK;
    }

    /** Percentage of the budget used, with two decimals; null when there is no budget. */
    public static BigDecimal percentUsed(BigDecimal spent, BigDecimal budget) {
        if (budget == null || budget.signum() <= 0) {
            return null;
        }
        return spent.multiply(HUNDRED).divide(budget, 2, RoundingMode.HALF_UP);
    }
}