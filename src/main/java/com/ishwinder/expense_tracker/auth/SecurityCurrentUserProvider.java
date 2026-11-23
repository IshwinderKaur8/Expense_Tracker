package com.ishwinder.expense_tracker.auth;

import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails user) {
            return user.getId();
        }
        // Never fall back to a default user: a missing login must fail loudly
        throw new IllegalStateException("No authenticated user in the security context");
    }
}