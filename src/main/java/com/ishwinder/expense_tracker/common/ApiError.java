package com.ishwinder.expense_tracker.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        List<FieldIssue> details,
        String path
) {
    public record FieldIssue(String field, String message) { }
}