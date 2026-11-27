package com.ishwinder.expense_tracker.budget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetStatusTest {

    @ParameterizedTest
    @CsvSource({
            "0.00,    1000.00, OK",
            "799.99,  1000.00, OK",
            "800.00,  1000.00, WARNING",
            "999.99,  1000.00, WARNING",
            "1000.00, 1000.00, WARNING",
            "1000.01, 1000.00, EXCEEDED"
    })
    void evaluate_classifiesSpendingAgainstBudget(BigDecimal spent, BigDecimal budget,
                                                  BudgetStatus expected) {
        assertThat(BudgetStatus.evaluate(spent, budget)).isEqualTo(expected);
    }

    @Test
    void evaluate_returnsNone_whenThereIsNoBudget() {
        assertThat(BudgetStatus.evaluate(new BigDecimal("50.00"), null))
                .isEqualTo(BudgetStatus.NONE);
    }

    @Test
    void percentUsed_roundsToTwoDecimals_andIsNullWithoutBudget() {
        assertThat(BudgetStatus.percentUsed(new BigDecimal("250.50"), new BigDecimal("400.00")))
                .isEqualByComparingTo("62.63");
        assertThat(BudgetStatus.percentUsed(new BigDecimal("10.00"), null)).isNull();
    }
}