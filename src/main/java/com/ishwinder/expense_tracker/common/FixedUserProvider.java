package com.ishwinder.expense_tracker.common;

import org.springframework.stereotype.Component;

// Temporary: always user 1. Replaced by a login-based provider later.
@Component
public class FixedUserProvider implements CurrentUserProvider {
    @Override
    public Long currentUserId() {
        return 1L;
    }
}