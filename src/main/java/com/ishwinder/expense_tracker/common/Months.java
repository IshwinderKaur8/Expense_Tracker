package com.ishwinder.expense_tracker.common;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class Months {

    private Months() { }

    /** Parses "2026-10" into a YearMonth, or throws a 400-style exception. */
    public static YearMonth parse(String value) {
        if (value == null) {
            throw new BadRequestException("Month is required, in the form 2026-10");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Month must look like 2026-10");
        }
    }
}